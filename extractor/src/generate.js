const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const axios = require('axios');
const cheerio = require('cheerio');
const dayjs = require('dayjs');
const utc = require('dayjs/plugin/utc');
const timezone = require('dayjs/plugin/timezone');

dayjs.extend(utc);
dayjs.extend(timezone);

const CHANNELS = require('./channels.json');
const OUTPUT_DIR = path.resolve(__dirname, '../output');
const OUTPUT_XML = path.join(OUTPUT_DIR, 'epg_tnt.xml');
const OUTPUT_GZ = path.join(OUTPUT_DIR, 'epg_tnt.xml.gz');
const DAYS_TO_FETCH = 13;

const HTTP_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
  'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
  'Accept-Language': 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7'
};

const detailCache = new Map();

function cleanTitle(str) {
  return (str || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '');
}

function escapeXml(unsafe) {
  if (!unsafe) return '';
  return unsafe
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function formatXmltvDate(date) {
  return dayjs(date).tz('Europe/Paris').format('YYYYMMDDHHmmss ZZ');
}

function extractImageSrc($el) {
  let chosen = '';
  const pictureSource = $el.find('picture source').first().attr('srcset');
  if (pictureSource) {
    chosen = pictureSource.split(',')[0].trim().split(' ')[0];
  }

  if (!chosen) {
    const imgEl = $el.find('img').first();
    if (imgEl.length) {
      const dataSrc = imgEl.attr('data-src');
      const dataSrcset = imgEl.attr('data-srcset') || imgEl.attr('srcset');
      const src = imgEl.attr('src');
      if (dataSrc && !dataSrc.startsWith('data:')) {
        chosen = dataSrc;
      } else if (dataSrcset) {
        chosen = dataSrcset.split(',')[0].trim().split(' ')[0];
      } else if (src && !src.startsWith('data:')) {
        chosen = src;
      }
    }
  }

  if (chosen.startsWith('//')) {
    chosen = `https:${chosen}`;
  }
  if (chosen.startsWith('data:')) {
    chosen = '';
  }
  if (chosen && chosen.includes('/64x90/')) {
    chosen = chosen.replace(/\/64x90\/quality\/\d+\//, '/640x360/quality/90/');
  }
  return chosen;
}

async function loadXmltvReference() {
  const timeMap = new Map();
  const titleMap = new Map();

  const resolveChannelMap = ($root) => {
    const map = new Map();
    $root('channel').each((_, el) => {
      const rawId = $root(el).attr('id');
      const dispName = cleanTitle($root(el).find('display-name').text());
      const matched = CHANNELS.find(c => {
        const cName = cleanTitle(c.name);
        const cId = cleanTitle(c.id);
        return cName === dispName || cId === cleanTitle(rawId) || (cName.length > 3 && dispName.includes(cName)) || (dispName.length > 3 && cName.includes(dispName));
      });
      if (matched) {
        map.set(rawId, matched.id);
      }
    });
    return map;
  };

  const registerEntry = (channel, start, desc, subTitle, title, rating, category, credits, channelMap) => {
    if (!channel || !start || !desc || desc.length <= 5 || /^\d{1,2}[h:]\d{2}$/i.test(desc)) return;
    const entry = {
      desc,
      subTitle: subTitle || null,
      category: category || null,
      rating: rating || null,
      credits
    };

    const targetChannels = [channel];
    const mapped = channelMap.get(channel);
    if (mapped && mapped !== channel) targetChannels.push(mapped);

    for (const ch of targetChannels) {
      const timeKey = `${ch}_${start.substring(0, 12)}`;
      timeMap.set(timeKey, entry);

      const dayKey = `${ch}_${start.substring(0, 8)}_${cleanTitle(title)}`;
      titleMap.set(dayKey, entry);

      const generalKey = `${ch}_${cleanTitle(title)}`;
      if (!titleMap.has(generalKey)) {
        titleMap.set(generalKey, entry);
      }
    }
  };

  try {
    const res = await axios.get('https://xmltvfr.fr/xmltv/xmltv_tnt.xml.gz', {
      responseType: 'arraybuffer',
      timeout: 20000,
      headers: HTTP_HEADERS
    });
    const xml = zlib.gunzipSync(res.data).toString('utf-8');
    const $xml = cheerio.load(xml, { xmlMode: true });
    const channelMap = resolveChannelMap($xml);

    $xml('programme').each((_, el) => {
      const channel = $xml(el).attr('channel');
      const start = $xml(el).attr('start');
      const desc = $xml(el).find('desc').text().trim();
      const subTitle = $xml(el).find('sub-title').text().trim();
      const title = $xml(el).find('title').text().trim();
      const rating = $xml(el).find('rating value').text().trim();
      const category = $xml(el).find('category').text().trim();

      const presenters = $xml(el).find('credits presenter').map((_, p) => $xml(p).text().trim()).get();
      const actors = $xml(el).find('credits actor').map((_, a) => $xml(a).text().trim()).get();
      const directors = $xml(el).find('credits director').map((_, d) => $xml(d).text().trim()).get();

      registerEntry(channel, start, desc, subTitle, title, rating, category, { presenters, actors, directors }, channelMap);
    });
  } catch (err) {
    process.stderr.write(`Failed to load xmltv_tnt reference: ${err.message}\n`);
  }

  try {
    const res = await axios.get('https://xmltvfr.fr/xmltv/xmltv.xml.gz', {
      responseType: 'arraybuffer',
      timeout: 30000,
      headers: HTTP_HEADERS
    });
    const xml = zlib.gunzipSync(res.data).toString('utf-8');
    const $xml = cheerio.load(xml, { xmlMode: true });
    const channelMap = resolveChannelMap($xml);

    $xml('programme').each((_, el) => {
      const channel = $xml(el).attr('channel');
      if (channelMap.has(channel)) {
        const start = $xml(el).attr('start');
        const desc = $xml(el).find('desc').text().trim();
        const subTitle = $xml(el).find('sub-title').text().trim();
        const title = $xml(el).find('title').text().trim();
        const rating = $xml(el).find('rating value').text().trim();
        const category = $xml(el).find('category').text().trim();

        const presenters = $xml(el).find('credits presenter').map((_, p) => $xml(p).text().trim()).get();
        const actors = $xml(el).find('credits actor').map((_, a) => $xml(a).text().trim()).get();
        const directors = $xml(el).find('credits director').map((_, d) => $xml(d).text().trim()).get();

        registerEntry(channel, start, desc, subTitle, title, rating, category, { presenters, actors, directors }, channelMap);
      }
    });
  } catch (err) {
    process.stderr.write(`Failed to load extended xmltv reference: ${err.message}\n`);
  }

  return { timeMap, titleMap };
}

async function fetchDetailPageData(link) {
  if (!link || !link.startsWith('/programme/')) return null;
  const cleanLink = link.split('#')[0];
  if (detailCache.has(cleanLink)) return detailCache.get(cleanLink);

  const fullUrl = `https://www.programme-tv.net${cleanLink}`;
  try {
    const res = await axios.get(fullUrl, { headers: HTTP_HEADERS, timeout: 6000 });
    const $page = cheerio.load(res.data);
    let desc = null;
    let season = null;
    let format = null;
    let genre = null;
    let year = null;
    let country = null;
    let episodeNumber = null;

    $page('script[type="application/ld+json"]').each((_, s) => {
      try {
        const json = JSON.parse($page(s).html());
        if (json.description && json.description.length > 20) {
          desc = json.description;
        }
        if (json.seasonNumber) {
          season = parseInt(json.seasonNumber, 10);
        }
        if (json.datePublished) {
          const matchY = String(json.datePublished).match(/(\d{4})/);
          if (matchY) year = parseInt(matchY[1], 10);
        }
        if (json.episode && Array.isArray(json.episode)) {
          const anchor = link.includes('#') ? link.split('#')[1].split('-')[0] : '';
          const ep = json.episode.find(e => anchor && e.url && e.url.includes(anchor));
          if (ep) {
            if (ep.description && ep.description.length > 20) desc = ep.description;
            if (ep.episodeNumber) episodeNumber = parseInt(ep.episodeNumber, 10);
          }
        }
      } catch {}
    });

    if (!desc) {
      const og = $page('meta[property="og:description"]').attr('content');
      if (og && og.length > 20 && !og.includes('Consultez la grille')) {
        desc = og;
      }
    }

    if (!desc) {
      const syn = $page('.synopsis, .program-summary, [class*="synopsis"]').text().replace(/\s+/g, ' ').trim();
      if (syn && syn.length > 20) {
        desc = syn;
      }
    }

    const overviewTitle = $page('.overview-title').text().trim();
    const seasonMatch = overviewTitle.match(/Saison\s*(\d+)/i) || cleanLink.match(/saisons\/(\d+)/i);
    if (!season && seasonMatch) {
      season = parseInt(seasonMatch[1], 10);
    }

    $page('.overview-stickerListItem').each((_, li) => {
      const text = $page(li).text().trim();
      if (text.includes('Format :') && !format) {
        format = text.replace('Format :', '').trim();
      }
      if (text.includes('Genre :') && !genre) {
        genre = text.replace('Genre :', '').trim();
      }
      if (text.includes('Sortie :') && !year) {
        const matchY = text.match(/(\d{4})/);
        if (matchY) year = parseInt(matchY[1], 10);
      }
      if (text.includes('Pays :') && !country) {
        country = text.replace('Pays :', '').trim();
      }
    });

    let seriesYear = null;
    if (cleanLink.includes('secrets-dhistoire')) {
      seriesYear = 2015;
      if (!genre) genre = 'Magazine historique';
      if (!format) format = 'Culture Infos';
    }

    const result = { desc, season, format, genre, year, seriesYear, country, episodeNumber };
    detailCache.set(cleanLink, result);
    return result;
  } catch {
    detailCache.set(cleanLink, null);
    return null;
  }
}

async function fetchChannelSchedule(channel, date, refData) {
  const formattedDate = date.format('YYYY-MM-DD');
  const url = `https://www.programme-tv.net/programme/chaine/${formattedDate}/programme-${channel.siteId}.html`;
  try {
    const response = await axios.get(url, { headers: HTTP_HEADERS, timeout: 15000 });
    return await parseHtmlSchedule(channel, response.data, date, refData);
  } catch (err) {
    process.stderr.write(`Failed to fetch ${channel.id} for ${formattedDate}: ${err.message}\n`);
    return [];
  }
}

async function parseHtmlSchedule(channel, html, baseDate, refData) {
  const $ = cheerio.load(html);
  const rawItems = $('.mainBroadcastCard, .gridBroadcastCard, [class*="broadcastCard"]');
  const extracted = [];

  rawItems.each((_, el) => {
    const $el = $(el);
    const title = $el.find('.mainBroadcastCard-title, .gridBroadcastCard-title, h3, a[title]').first().text().trim();
    if (!title) return;

    const subTitle = $el.find('.mainBroadcastCard-subtitle, .gridBroadcastCard-subtitle, .subtitle').first().text().trim();
    const category = $el.find('.mainBroadcastCard-genre, .gridBroadcastCard-genre, .mainBroadcastCard-format, .genre').first().text().trim();
    const timeText = $el.find('.mainBroadcastCard-startingHour, .gridBroadcastCard-startingHour, time, .hour').first().text().trim();
    const imageSrc = extractImageSrc($el);
    const link = $el.find('a[href*="/programme/"]').first().attr('href');

    const timeMatch = timeText.match(/(\d{1,2})[h:](\d{2})/i);
    if (!timeMatch) return;

    const hours = parseInt(timeMatch[1], 10);
    const minutes = parseInt(timeMatch[2], 10);
    const startTime = baseDate.hour(hours).minute(minutes).second(0);

    extracted.push({
      title,
      subTitle: subTitle || null,
      category: category || 'General',
      startTime,
      imageSrc,
      link,
      hours
    });
  });

  const programs = [];

  for (const item of extracted) {
    let matchedDesc = null;
    let matchedSubtitle = item.subTitle;
    let matchedCategory = item.category;
    let matchedRating = null;
    let matchedCredits = null;

    const timeKey = `${channel.id}_${item.startTime.format('YYYYMMDDHHmm')}`;
    let refEntry = refData.timeMap.get(timeKey);

    if (!refEntry) {
      const offsets = [5, -5, 10, -10, 15, -15];
      for (const offset of offsets) {
        const shiftedKey = `${channel.id}_${item.startTime.add(offset, 'minute').format('YYYYMMDDHHmm')}`;
        if (refData.timeMap.has(shiftedKey)) {
          refEntry = refData.timeMap.get(shiftedKey);
          break;
        }
      }
    }

    if (!refEntry) {
      const dayKey = `${channel.id}_${item.startTime.format('YYYYMMDD')}_${cleanTitle(item.title)}`;
      refEntry = refData.titleMap.get(dayKey);
    }

    if (!refEntry) {
      const generalKey = `${channel.id}_${cleanTitle(item.title)}`;
      refEntry = refData.titleMap.get(generalKey);
    }

    if (refEntry) {
      matchedDesc = refEntry.desc;
      if (!matchedSubtitle && refEntry.subTitle) matchedSubtitle = refEntry.subTitle;
      if (item.category === 'General' && refEntry.category) matchedCategory = refEntry.category;
      matchedRating = refEntry.rating;
      matchedCredits = refEntry.credits;
    }

    let detailData = null;
    if (item.hours >= 20 && item.hours <= 23 && item.link) {
      detailData = await fetchDetailPageData(item.link);
      if (detailData && detailData.desc && detailData.desc.length > 30) {
        matchedDesc = detailData.desc;
      }
    } else if (item.link) {
      const clean = item.link.split('#')[0];
      if (!matchedDesc || clean.includes('/saisons/') || clean.includes('/cinema/')) {
        detailData = await fetchDetailPageData(item.link);
        if (detailData && detailData.desc && detailData.desc.length > 20) {
          matchedDesc = detailData.desc;
        }
      }
    }

    programs.push({
      channelId: channel.id,
      title: item.title,
      subTitle: matchedSubtitle,
      desc: matchedDesc,
      category: matchedCategory,
      format: detailData ? detailData.format : null,
      genre: detailData ? detailData.genre : null,
      year: detailData ? detailData.year : null,
      seriesYear: detailData ? detailData.seriesYear : null,
      country: detailData ? detailData.country : null,
      season: detailData ? detailData.season : null,
      episode: detailData ? detailData.episodeNumber : null,
      start: item.startTime,
      stop: item.startTime.add(60, 'minute'),
      image: item.imageSrc,
      rating: matchedRating,
      credits: matchedCredits
    });
  }

  for (let i = 0; i < programs.length - 1; i++) {
    programs[i].stop = programs[i + 1].start;
  }

  return programs;
}

function buildXml(channels, programs) {
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<!DOCTYPE tv SYSTEM "xmltv.dtd">\n';
  xml += '<tv source-info-name="TV Guide TNT+ Extractor" source-info-url="https://github.com/heberjeur/tv-guide">\n';

  for (const channel of channels) {
    xml += `  <channel id="${escapeXml(channel.id)}">\n`;
    xml += `    <display-name>${escapeXml(channel.name)}</display-name>\n`;
    if (channel.icon) {
      xml += `    <icon src="${escapeXml(channel.icon)}" />\n`;
    }
    xml += `  </channel>\n`;
  }

  for (const program of programs) {
    const startStr = formatXmltvDate(program.start);
    const stopStr = formatXmltvDate(program.stop);
    xml += `  <programme start="${startStr}" stop="${stopStr}" channel="${escapeXml(program.channelId)}">\n`;
    xml += `    <title lang="fr">${escapeXml(program.title)}</title>\n`;
    if (program.subTitle) {
      xml += `    <sub-title lang="fr">${escapeXml(program.subTitle)}</sub-title>\n`;
    }
    if (program.desc) {
      xml += `    <desc lang="fr">${escapeXml(program.desc)}</desc>\n`;
    }
    if (program.year) {
      xml += `    <date>${escapeXml(String(program.year))}</date>\n`;
    }
    if (program.seriesYear) {
      xml += `    <orig-date>${escapeXml(String(program.seriesYear))}</orig-date>\n`;
    }
    if (program.country) {
      xml += `    <country lang="fr">${escapeXml(program.country)}</country>\n`;
    }
    if (program.season != null || program.episode != null) {
      const s = program.season != null ? (program.season - 1) : '';
      const e = program.episode != null ? (program.episode - 1) : '';
      xml += `    <episode-num system="xmltv_ns">${s}.${e}.</episode-num>\n`;
    }
    const categories = [];
    if (program.category && program.category !== 'General') categories.push(program.category);
    if (program.format && !categories.includes(program.format)) categories.push(program.format);
    if (program.genre && !categories.includes(program.genre)) categories.push(program.genre);
    if (categories.length === 0) categories.push('General');
    for (const cat of categories) {
      xml += `    <category lang="fr">${escapeXml(cat)}</category>\n`;
    }
    if (program.image) {
      xml += `    <icon src="${escapeXml(program.image)}" />\n`;
    }
    if (program.rating) {
      xml += `    <rating system="CSA">\n`;
      xml += `      <value>${escapeXml(program.rating)}</value>\n`;
      xml += `    </rating>\n`;
    }
    if (program.credits) {
      const { presenters, actors, directors } = program.credits;
      const hasCredits = (presenters && presenters.length) || (actors && actors.length) || (directors && directors.length);
      if (hasCredits) {
        xml += `    <credits>\n`;
        if (directors) {
          for (const d of directors) xml += `      <director>${escapeXml(d)}</director>\n`;
        }
        if (actors) {
          for (const a of actors) xml += `      <actor>${escapeXml(a)}</actor>\n`;
        }
        if (presenters) {
          for (const p of presenters) xml += `      <presenter>${escapeXml(p)}</presenter>\n`;
        }
        xml += `    </credits>\n`;
      }
    }
    xml += `  </programme>\n`;
  }

  xml += '</tv>\n';
  return xml;
}

async function main() {
  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }

  process.stdout.write('Loading reference EPG data...\n');
  const refData = await loadXmltvReference();
  process.stdout.write(`Indexed ${refData.timeMap.size} reference descriptions.\n`);

  const today = dayjs().tz('Europe/Paris');
  const allPrograms = [];
  const batchSize = 4;

  for (let i = 0; i < CHANNELS.length; i += batchSize) {
    const batch = CHANNELS.slice(i, i + batchSize);
    const batchResults = await Promise.all(
      batch.map(async (channel) => {
        const channelPrograms = [];
        for (let dayOffset = -1; dayOffset < DAYS_TO_FETCH; dayOffset++) {
          const targetDate = today.add(dayOffset, 'day');
          const programs = await fetchChannelSchedule(channel, targetDate, refData);
          channelPrograms.push(...programs);
        }
        channelPrograms.sort((a, b) => a.start.valueOf() - b.start.valueOf());
        for (let j = 0; j < channelPrograms.length - 1; j++) {
          if (channelPrograms[j + 1].start.isAfter(channelPrograms[j].start)) {
            channelPrograms[j].stop = channelPrograms[j + 1].start;
          }
        }
        return channelPrograms;
      })
    );
    for (const res of batchResults) {
      allPrograms.push(...res);
    }
  }

  const xmlContent = buildXml(CHANNELS, allPrograms);
  fs.writeFileSync(OUTPUT_XML, xmlContent, 'utf-8');

  const gzippedContent = zlib.gzipSync(Buffer.from(xmlContent, 'utf-8'), { level: 9 });
  fs.writeFileSync(OUTPUT_GZ, gzippedContent);

  const assetGz = path.resolve(__dirname, '../../app/src/main/assets/epg_tnt.xml.gz');
  if (fs.existsSync(path.dirname(assetGz))) {
    fs.writeFileSync(assetGz, gzippedContent);
  }

  process.stdout.write(`Successfully generated ${allPrograms.length} TNT+ programs in ${OUTPUT_XML} and ${OUTPUT_GZ}\n`);
}

main().catch((err) => {
  process.stderr.write(`${err.message}\n`);
  process.exit(1);
});
