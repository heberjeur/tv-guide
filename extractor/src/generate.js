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
const DAYS_TO_FETCH = 11;

const HTTP_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
  'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
  'Accept-Language': 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7'
};

const detailCache = new Map();

const BOILERPLATE_PHRASES = [
  'Chaque jour, Laurent Mariotte propose des recettes simples',
  'Les prévisions météo pour la journée',
  'Les prévisions météo pour le lendemain',
  'Le bulletin météorologique international',
  'Un point quotidien sur l\'actualité météo',
  'Retrouvez tous vos programmes de nuit',
  'A suivre, des rediffusions des émissions de la chaîne',
  'Programmes de la nuit',
  'Fin des émissions',
  'Toutes les prévisions météo'
];

function sanitizeDescriptionText(text) {
  if (!text) return null;
  let cleaned = text.trim();
  for (const phrase of BOILERPLATE_PHRASES) {
    if (cleaned.toLowerCase().includes(phrase.toLowerCase())) {
      const reg = new RegExp('[^.\\n]*' + phrase.replace(/[.*+?^${}()|[\\]\\]/g, '\\$&') + '[^.\\n]*\\.?', 'gi');
      cleaned = cleaned.replace(reg, '').trim();
    }
  }
  cleaned = cleaned.replace(/[^.\n]*\b(?:télé[- ]loisirs|programme[- ]tv\.net)\b[^.\n]*\.?/gi, '').trim();
  cleaned = cleaned.replace(/\s{2,}/g, ' ').trim();
  if (cleaned.length < 15 || !/[a-zA-Z0-9]/.test(cleaned)) {
    return null;
  }
  return cleaned;
}

function normalizeForComparison(str) {
  return (str || '')
    .toLowerCase()
    .replace(/œ/g, 'oe')
    .replace(/æ/g, 'ae')
    .replace(/[’']/g, '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '');
}

function resolveDescriptions(source1, source2) {
  const clean1 = sanitizeDescriptionText(source1);
  const clean2 = sanitizeDescriptionText(source2);

  if (!clean1 && !clean2) return { primary: null, secondary: null };
  if (!clean1) return { primary: clean2, secondary: null };
  if (!clean2) return { primary: clean1, secondary: null };

  const norm1 = normalizeForComparison(clean1);
  const norm2 = normalizeForComparison(clean2);

  if (norm1 === norm2 || norm1.startsWith(norm2) || norm2.startsWith(norm1) || norm1.includes(norm2) || norm2.includes(norm1)) {
    const longer = clean1.length >= clean2.length ? clean1 : clean2;
    return { primary: longer, secondary: null };
  }

  return { primary: clean1, secondary: clean2 };
}

function cleanTitle(str) {
  return (str || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '');
}

function areTitlesCompatible(titleA, titleB) {
  if (!titleA || !titleB) return false;
  const cleanA = cleanTitle(titleA);
  const cleanB = cleanTitle(titleB);
  if (!cleanA || !cleanB) return false;
  if (cleanA === cleanB) return true;
  if (cleanA.length >= 4 && cleanB.length >= 4) {
    if (cleanA.includes(cleanB) || cleanB.includes(cleanA)) return true;
  }
  const extractWords = (t) => (t || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .split(/[^a-z0-9]+/)
    .filter(w => w.length >= 3);
  const wordsA = extractWords(titleA);
  const wordsB = extractWords(titleB);
  if (wordsA.length > 0 && wordsB.length > 0) {
    const common = wordsA.filter(w => wordsB.includes(w));
    if (common.length >= 2 || (wordsA.length === 1 && common.length === 1 && wordsB.length <= 2)) {
      return true;
    }
  }
  return false;
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

function parseXmltvDate(dateStr) {
  if (!dateStr || dateStr.length < 14) return null;
  const raw = dateStr.trim();
  const year = parseInt(raw.substring(0, 4), 10);
  const month = parseInt(raw.substring(4, 6), 10) - 1;
  const day = parseInt(raw.substring(6, 8), 10);
  const hour = parseInt(raw.substring(8, 10), 10);
  const minute = parseInt(raw.substring(10, 12), 10);
  const second = parseInt(raw.substring(12, 14), 10);
  return dayjs().tz('Europe/Paris').year(year).month(month).date(day).hour(hour).minute(minute).second(second);
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
  const channelProgramsMap = new Map();

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

  const registerEntry = (channel, start, stop, desc, subTitle, title, rating, category, credits, season, episode, year, seriesYear, country, icon, channelMap, isUnreleased = false, isRerun = false, hasSubtitles = false, isHd = false) => {
    if (!channel || !start || !title) return;
    const entry = {
      channelId: channel,
      title,
      subTitle: subTitle || null,
      desc: desc || null,
      category: category || null,
      rating: rating || null,
      credits,
      season,
      episode,
      year,
      seriesYear,
      country,
      icon,
      isUnreleased,
      isRerun,
      hasSubtitles,
      isHd,
      start: parseXmltvDate(start),
      stop: parseXmltvDate(stop)
    };

    const targetChannels = [channel];
    const mapped = channelMap.get(channel);
    if (mapped && mapped !== channel) targetChannels.push(mapped);

    for (const ch of targetChannels) {
      if (entry.desc && entry.desc.length > 5) {
        const timeKey = `${ch}_${start.substring(0, 12)}`;
        timeMap.set(timeKey, entry);

        const dayKey = `${ch}_${start.substring(0, 8)}_${cleanTitle(title)}`;
        titleMap.set(dayKey, entry);

        const generalKey = `${ch}_${cleanTitle(title)}`;
        if (!titleMap.has(generalKey)) {
          titleMap.set(generalKey, entry);
        }
      }

      if (entry.start) {
        const progDayKey = `${ch}_${start.substring(0, 8)}`;
        if (!channelProgramsMap.has(progDayKey)) {
          channelProgramsMap.set(progDayKey, []);
        }
        channelProgramsMap.get(progDayKey).push({ ...entry, channelId: ch });
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
      const stop = $xml(el).attr('stop');
      const desc = $xml(el).find('desc').text().trim();
      const subTitle = $xml(el).find('sub-title').text().trim();
      const title = $xml(el).find('title').text().trim();
      const rating = $xml(el).find('rating value').text().trim();
      const category = $xml(el).find('category').first().text().trim();
      const dateText = $xml(el).find('date').text().trim();
      const origDateText = $xml(el).find('orig-date').text().trim();
      const country = $xml(el).find('country').text().trim() || null;
      const icon = $xml(el).find('icon').attr('src') || null;

      const year = dateText ? parseInt(dateText.substring(0, 4), 10) : null;
      const seriesYear = origDateText ? parseInt(origDateText.substring(0, 4), 10) : null;

      let season = null;
      let episode = null;
      const epNumText = $xml(el).find('episode-num[system="xmltv_ns"]').text().trim();
      if (epNumText) {
        const parts = epNumText.split('.');
        if (parts[0]) season = parseInt(parts[0], 10) + 1;
        if (parts[1]) episode = parseInt(parts[1], 10) + 1;
      }

      const presenters = $xml(el).find('credits presenter').map((_, p) => $xml(p).text().trim()).get();
      const actors = $xml(el).find('credits actor').map((_, a) => $xml(a).text().trim()).get();
      const directors = $xml(el).find('credits director').map((_, d) => $xml(d).text().trim()).get();

      const isUnreleased = $xml(el).find('premiere').length > 0;
      const isRerun = $xml(el).find('previously-shown').length > 0;
      const hasSubtitles = $xml(el).find('subtitles').length > 0;
      const isHd = $xml(el).find('video quality').text().toUpperCase().includes('HD');

      registerEntry(channel, start, stop, desc, subTitle, title, rating, category, { presenters, actors, directors }, season, episode, year, seriesYear, country, icon, channelMap, isUnreleased, isRerun, hasSubtitles, isHd);
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
        const stop = $xml(el).attr('stop');
        const desc = $xml(el).find('desc').text().trim();
        const subTitle = $xml(el).find('sub-title').text().trim();
        const title = $xml(el).find('title').text().trim();
        const rating = $xml(el).find('rating value').text().trim();
        const category = $xml(el).find('category').first().text().trim();
        const dateText = $xml(el).find('date').text().trim();
        const origDateText = $xml(el).find('orig-date').text().trim();
        const country = $xml(el).find('country').text().trim() || null;
        const icon = $xml(el).find('icon').attr('src') || null;

        const year = dateText ? parseInt(dateText.substring(0, 4), 10) : null;
        const seriesYear = origDateText ? parseInt(origDateText.substring(0, 4), 10) : null;

        let season = null;
        let episode = null;
        const epNumText = $xml(el).find('episode-num[system="xmltv_ns"]').text().trim();
        if (epNumText) {
          const parts = epNumText.split('.');
          if (parts[0]) season = parseInt(parts[0], 10) + 1;
          if (parts[1]) episode = parseInt(parts[1], 10) + 1;
        }

        const presenters = $xml(el).find('credits presenter').map((_, p) => $xml(p).text().trim()).get();
        const actors = $xml(el).find('credits actor').map((_, a) => $xml(a).text().trim()).get();
        const directors = $xml(el).find('credits director').map((_, d) => $xml(d).text().trim()).get();

        const isUnreleased = $xml(el).find('premiere').length > 0;
        const isRerun = $xml(el).find('previously-shown').length > 0;
        const hasSubtitles = $xml(el).find('subtitles').length > 0;
        const isHd = $xml(el).find('video quality').text().toUpperCase().includes('HD');

        registerEntry(channel, start, stop, desc, subTitle, title, rating, category, { presenters, actors, directors }, season, episode, year, seriesYear, country, icon, channelMap, isUnreleased, isRerun, hasSubtitles, isHd);
      }
    });
  } catch (err) {
    process.stderr.write(`Failed to load extended xmltv reference: ${err.message}\n`);
  }

  return { timeMap, titleMap, channelProgramsMap };
}

async function fetchDetailPageData(link) {
  if (!link || (!link.startsWith('/programme/') && !link.startsWith('/cinema/') && !link.startsWith('/series/'))) return null;
  const cleanLink = link.split('#')[0];
  if (detailCache.has(cleanLink)) return detailCache.get(cleanLink);

  const fullUrl = `https://www.programme-tv.net${cleanLink}`;
  try {
    const res = await axios.get(fullUrl, { headers: HTTP_HEADERS, timeout: 8000 });
    const $page = cheerio.load(res.data);
    let desc = null;
    let season = null;
    let format = null;
    let genre = null;
    let year = null;
    let seriesYear = null;
    let country = null;
    let episodeNumber = null;
    let seriesDesc = null;
    const directors = [];

    const anchor = link.includes('#') ? link.split('#')[1].split('-')[0] : '';

    $page('script[type="application/ld+json"]').each((_, s) => {
      try {
        const json = JSON.parse($page(s).html());
        if (json.description && json.description.length > 20 && !json.description.endsWith('...') && !json.description.endsWith('…')) {
          desc = json.description.trim();
        }
        if (json.seasonNumber) {
          season = parseInt(json.seasonNumber, 10);
        }
        if (json.datePublished) {
          const matchY = String(json.datePublished).match(/(\d{4})/);
          if (matchY) year = parseInt(matchY[1], 10);
        }
        if (json.director) {
          const dirs = Array.isArray(json.director) ? json.director : [json.director];
          for (const d of dirs) {
            const name = typeof d === 'string' ? d : d?.name;
            if (name && typeof name === 'string' && name.trim()) {
              directors.push(name.trim());
            }
          }
        }
        if (json.episode && Array.isArray(json.episode)) {
          let epIdx = -1;
          for (let i = 0; i < json.episode.length; i++) {
            const e = json.episode[i];
            if (anchor && e.url && e.url.includes(anchor)) {
              epIdx = i;
              if (e.description && e.description.length > 20 && !e.description.endsWith('...') && !e.description.endsWith('…')) {
                desc = e.description.trim();
              }
              if (e.episodeNumber) episodeNumber = parseInt(e.episodeNumber, 10);
              break;
            }
          }
          if (episodeNumber == null && epIdx >= 0) {
            episodeNumber = epIdx + 1;
          }
        }
      } catch {}
    });

    let pageSyn = $page('.synopsis-text').first().text().replace(/\s+/g, ' ').trim();
    if (!pageSyn) {
      const synEl = $page('.synopsis, .program-summary').first().clone();
      synEl.find('button, .synopsis-action, a, [class*="action"], [class*="btn"]').remove();
      pageSyn = synEl.text().replace(/\s+/g, ' ').replace(/^R[ée]sum[ée]\s+[^:]*:\s*/i, '').replace(/Voir plusmoins/gi, '').trim();
    }
    if (pageSyn && pageSyn.length > 20 && !pageSyn.endsWith('...') && !pageSyn.endsWith('…')) {
      desc = pageSyn;
    }

    if (!desc) {
      const og = $page('meta[property="og:description"]').attr('content');
      if (og && og.length > 20 && !og.includes('Consultez la grille') && !og.endsWith('...') && !og.endsWith('…')) {
        desc = og.trim();
      }
    }

    const overviewTitle = $page('.overview-title').text().trim();
    const seasonMatch = overviewTitle.match(/Saison\s*(\d+)/i) || cleanLink.match(/saisons\/(\d+)/i);
    if (!season && seasonMatch) {
      season = parseInt(seasonMatch[1], 10);
    }

    let detailIsUnreleased = false;
    let detailIsRerun = false;
    let detailIsLive = false;
    let detailHasSubtitles = $page('.mainBroadcastCard-deaf, svg.icon-deaf, [aria-label*="Malentendant"]').length > 0 || /deaf/i.test($page('.overview, .synopsis').html() || '');
    let detailHasAudioDescription = $page('.mainBroadcastCard-audioDescription, svg.icon-audio-description, [aria-label*="Audio description"]').length > 0 || /audio-description|audiodescription/i.test($page('.overview, .synopsis').html() || '');
    let detailIsHd = false;
    let detailRating = null;

    $page('.overview-stickerListItem').each((_, li) => {
      const text = $page(li).text().replace(/\s+/g, ' ').trim();
      if (/in[ée]dit/i.test(text)) detailIsUnreleased = true;
      if (/rediff/i.test(text)) detailIsRerun = true;
      if (/\bdirect\b/i.test(text)) detailIsLive = true;
      if (/\b(st|sous[- ]titr|malentendant)\b/i.test(text)) detailHasSubtitles = true;
      if (/\b(ad|audiodescription)\b/i.test(text)) detailHasAudioDescription = true;
      if (/\b(hd|4k|uhd)\b/i.test(text)) detailIsHd = true;
      const m = text.match(/(-10|-12|-16|-18)/);
      if (m && !detailRating) detailRating = m[1];

      if (text.startsWith('Format :') && !format) {
        format = text.replace('Format :', '').trim();
      }
      if (text.startsWith('Genre :') && !genre) {
        genre = text.replace('Genre :', '').trim();
      }
      if ((text.startsWith('Sortie :') || text.startsWith('Année :')) && !year) {
        const matchY = text.match(/(\d{4})/);
        if (matchY) year = parseInt(matchY[1], 10);
      }
      if (text.startsWith('Pays :') && !country) {
        country = text.replace('Pays :', '').trim();
      }
      if (text.startsWith('De :') || text.startsWith('Réalisateur :') || text.startsWith('Réalisateurs :') || text.startsWith('Réalisation :')) {
        const dText = text.replace(/^(De|R[ée]alisateur|R[ée]alisateurs|R[ée]alisation)\s*:\s*/i, '').trim();
        if (dText) {
          const names = dText.split(/[,;&]|\bet\b/i).map(d => d.trim()).filter(d => d.length > 1);
          directors.push(...names);
        }
      }
    });

    if (!detailRating) {
      const csaAlt = $page('img[alt*="déconseillé"], img[src*="csa"], [class*="csa"]').attr('alt') || '';
      if (/moins de 10/i.test(csaAlt)) detailRating = '-10';
      else if (/moins de 12/i.test(csaAlt)) detailRating = '-12';
      else if (/moins de 16/i.test(csaAlt)) detailRating = '-16';
      else if (/moins de 18/i.test(csaAlt)) detailRating = '-18';
    }

    if (cleanLink.includes('/saisons/')) {
      const parentSeriesLink = cleanLink.replace(/\/saisons\/\d+.*$/, '/');
      try {
        const pRes = await axios.get(`https://www.programme-tv.net${parentSeriesLink}`, {
          headers: HTTP_HEADERS,
          timeout: 6000
        });
        const $pPage = cheerio.load(pRes.data);
        $pPage('.overview-stickerListItem').each((_, li) => {
          const text = $pPage(li).text().replace(/\s+/g, ' ').trim();
          if ((text.startsWith('Sortie :') || text.startsWith('Année :')) && !seriesYear) {
            const m = text.match(/(\d{4})/);
            if (m) seriesYear = parseInt(m[1], 10);
          }
          if (text.startsWith('Genre :') && !genre) {
            genre = text.replace('Genre :', '').trim();
          }
          if (text.startsWith('Format :') && !format) {
            format = text.replace('Format :', '').trim();
          }
          if (text.startsWith('De :') || text.startsWith('Réalisateur :') || text.startsWith('Réalisateurs :') || text.startsWith('Réalisation :')) {
            const dText = text.replace(/^(De|R[ée]alisateur|R[ée]alisateurs|R[ée]alisation)\s*:\s*/i, '').trim();
            if (dText) {
              const names = dText.split(/[,;&]|\bet\b/i).map(d => d.trim()).filter(d => d.length > 1);
              directors.push(...names);
            }
          }
        });

        let pSeriesSyn = $pPage('.synopsis-text').first().text().replace(/\s+/g, ' ').trim();
        if (!pSeriesSyn) {
          const synEl = $pPage('.synopsis, .program-summary').first().clone();
          synEl.find('button, .synopsis-action, a, [class*="action"], [class*="btn"]').remove();
          pSeriesSyn = synEl.text().replace(/\s+/g, ' ').replace(/^R[ée]sum[ée]\s+[^:]*:\s*/i, '').replace(/Voir plusmoins/gi, '').trim();
        }
        if (!pSeriesSyn) {
          const og = $pPage('meta[property="og:description"]').attr('content');
          if (og && og.length > 20 && !og.includes('Consultez la grille') && !og.endsWith('...') && !og.endsWith('…')) {
            pSeriesSyn = og.trim();
          }
        }
        if (pSeriesSyn && pSeriesSyn.length > 20 && !pSeriesSyn.endsWith('...') && !pSeriesSyn.endsWith('…')) {
          seriesDesc = pSeriesSyn;
        }
      } catch {}
    }

    const uniqueDirectors = Array.from(new Set(directors));
    const result = { desc, seriesDesc, season, format, genre, year, seriesYear, country, episodeNumber, directors: uniqueDirectors, isUnreleased: detailIsUnreleased, isRerun: detailIsRerun, isLive: detailIsLive, hasSubtitles: detailHasSubtitles, hasAudioDescription: detailHasAudioDescription, isHd: detailIsHd, rating: detailRating };
    detailCache.set(cleanLink, result);
    return result;
  } catch {
    detailCache.set(cleanLink, null);
    return null;
  }
}

async function fetchWithRetry(url, options, maxRetries = 2) {
  let attempt = 0;
  while (attempt <= maxRetries) {
    try {
      return await axios.get(url, options);
    } catch (err) {
      attempt++;
      if (attempt > maxRetries) throw err;
      await new Promise(r => setTimeout(r, 1000 * attempt));
    }
  }
}

async function fetchChannelSchedule(channel, date, refData) {
  const formattedDate = date.format('YYYY-MM-DD');
  const dateKey8 = date.format('YYYYMMDD');
  const fallbackPrograms = refData.channelProgramsMap.get(`${channel.id}_${dateKey8}`) || [];

  const url = `https://www.programme-tv.net/programme/chaine/${formattedDate}/programme-${channel.siteId}.html`;
  try {
    const response = await fetchWithRetry(url, { headers: HTTP_HEADERS, timeout: 15000 });
    const programs = await parseHtmlSchedule(channel, response.data, date, refData);
    if (programs.length < 3 && fallbackPrograms.length > 0) {
      return fallbackPrograms;
    }
    return programs;
  } catch (err) {
    process.stderr.write(`Failed to fetch ${channel.id} for ${formattedDate}: ${err.message}. Using fallback.\n`);
    return fallbackPrograms;
  }
}

async function parseHtmlSchedule(channel, html, baseDate, refData) {
  const $ = cheerio.load(html);
  const rawItems = $('.mainBroadcastCard, .gridBroadcastCard, [class*="broadcastCard"]');
  const extracted = [];

  let dayOffset = 0;
  let lastHour = -1;

  rawItems.each((_, el) => {
    const $el = $(el);
    const title = $el.find('.mainBroadcastCard-title, .gridBroadcastCard-title, h3, a[title]').first().text().trim();
    if (!title) return;

    const subTitle = $el.find('.mainBroadcastCard-subtitle, .gridBroadcastCard-subtitle, .subtitle').first().text().trim();
    const category = $el.find('.mainBroadcastCard-genre, .gridBroadcastCard-genre, .mainBroadcastCard-format, .genre').first().text().trim();
    const timeText = $el.find('.mainBroadcastCard-startingHour, .gridBroadcastCard-startingHour, time, .hour').first().text().trim();
    const imageSrc = extractImageSrc($el);
    const link = $el.find('a[href*="/programme/"], a[href*="/cinema/"], a[href*="/series/"]').first().attr('href');

    const stickers = [];
    $el.find('.mainBroadcastCard-sticker, .gridBroadcastCard-sticker, [class*="sticker"], [class*="badge"], [class*="c-badge"], .tag').each((_, s) => {
      const txt = $(s).text().trim();
      if (txt) stickers.push(txt);
    });

    const isRerun = $el.find('.mainBroadcastCard-rebroadcast').length > 0 || /rediff/i.test($el.text()) || stickers.some(s => /rediff/i.test(s));
    const isLive = $el.find('.mainBroadcastCard-live').length > 0 || stickers.some(s => /\bdirect\b/i.test(s));
    const isUnreleased = !isRerun && ($el.find('.mainBroadcastCard-new, .c-badge-new').length > 0 || stickers.some(s => /in[ée]dit/i.test(s)));
    const hasSubtitles = $el.find('.mainBroadcastCard-deaf, [class*="-deaf"], [aria-label*="Malentendant"], svg.icon-deaf').length > 0 || /deaf/i.test($el.html()) || stickers.some(s => /\b(st|sous[- ]titr|malentendant)\b/i.test(s));
    const hasAudioDescription = $el.find('.mainBroadcastCard-audioDescription, [class*="-audioDescription"], [aria-label*="Audio description"], svg.icon-audio-description').length > 0 || /audio-description|audiodescription/i.test($el.html()) || stickers.some(s => /\b(ad|audiodescription)\b/i.test(s));
    const isHd = stickers.some(s => /\bhd\b/i.test(s)) || /hd|4k|uhd/i.test($el.text());

    let cardRating = null;
    for (const s of stickers) {
      const m = s.match(/(-10|-12|-16|-18)/);
      if (m) { cardRating = m[1]; break; }
    }
    if (!cardRating) {
      const csaAlt = $el.find('img[alt*="déconseillé"], img[src*="csa"], [class*="csa"]').attr('alt') || '';
      if (/moins de 10/i.test(csaAlt)) cardRating = '-10';
      else if (/moins de 12/i.test(csaAlt)) cardRating = '-12';
      else if (/moins de 16/i.test(csaAlt)) cardRating = '-16';
      else if (/moins de 18/i.test(csaAlt)) cardRating = '-18';
    }

    const timeMatch = timeText.match(/(\d{1,2})[h:](\d{2})/i);
    if (!timeMatch) return;

    const hours = parseInt(timeMatch[1], 10);
    const minutes = parseInt(timeMatch[2], 10);
    if (lastHour >= 20 && hours < 6) {
      dayOffset = 1;
    }
    lastHour = hours;
    const startTime = baseDate.add(dayOffset, 'day').hour(hours).minute(minutes).second(0);

    extracted.push({
      title,
      subTitle: subTitle || null,
      category: category || 'General',
      startTime,
      imageSrc,
      link,
      hours,
      isUnreleased,
      isRerun,
      isLive,
      hasSubtitles,
      hasAudioDescription,
      isHd,
      rating: cardRating
    });
  });

  const programs = [];

  for (const item of extracted) {
    let matchedDesc = null;
    let matchedSubtitle = item.subTitle;
    let matchedCategory = item.category;
    let matchedRating = null;
    let matchedCredits = null;
    let matchedSeason = null;
    let matchedEpisode = null;
    let matchedYear = null;
    let matchedSeriesYear = null;
    let matchedCountry = null;

    const timeKey = `${channel.id}_${item.startTime.format('YYYYMMDDHHmm')}`;
    let refEntry = refData.timeMap.get(timeKey);
    if (refEntry && !areTitlesCompatible(refEntry.title, item.title)) {
      refEntry = null;
    }

    if (!refEntry) {
      const offsets = [5, -5, 10, -10, 15, -15];
      for (const offset of offsets) {
        const shiftedKey = `${channel.id}_${item.startTime.add(offset, 'minute').format('YYYYMMDDHHmm')}`;
        if (refData.timeMap.has(shiftedKey)) {
          const candidate = refData.timeMap.get(shiftedKey);
          if (areTitlesCompatible(candidate.title, item.title)) {
            refEntry = candidate;
            break;
          }
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
      matchedSeason = refEntry.season;
      matchedEpisode = refEntry.episode;
      matchedYear = refEntry.year;
      matchedSeriesYear = refEntry.seriesYear;
      matchedCountry = refEntry.country;
    }

    let detailData = null;
    const isPrimeTime = item.hours >= 20 && item.hours <= 23;
    const isTruncated = !matchedDesc ||
      matchedDesc.endsWith('...') ||
      matchedDesc.endsWith('…') ||
      (!/[.!?\"»\)]$/.test(matchedDesc.trim()) && matchedDesc.length > 50) ||
      matchedDesc.includes('Toutes les infos de casting et de diffusions TV') ||
      matchedDesc.length < 180;
    const isSpecial = item.link && (item.link.includes('/cinema/') || item.link.includes('/series/') || item.link.includes('/saisons/'));
    if (item.link && (isPrimeTime || isTruncated || isSpecial)) {
      detailData = await fetchDetailPageData(item.link);
    }

    let primaryDesc = null;
    let secondaryDesc = null;

    const baseResolved = resolveDescriptions(matchedDesc, detailData ? detailData.desc : null);
    primaryDesc = baseResolved.primary;
    secondaryDesc = baseResolved.secondary;

    if (!secondaryDesc && detailData && detailData.seriesDesc) {
      const cleanSeries = sanitizeDescriptionText(detailData.seriesDesc);
      if (cleanSeries && cleanSeries.length > 20) {
        const normPrimary = normalizeForComparison(primaryDesc || '');
        const normSeries = normalizeForComparison(cleanSeries);
        if (normPrimary !== normSeries && !normPrimary.includes(normSeries) && !normSeries.includes(normPrimary)) {
          if (!primaryDesc) {
            primaryDesc = cleanSeries;
          } else {
            secondaryDesc = cleanSeries;
          }
        }
      }
    }

    const finalDirectors = (matchedCredits && matchedCredits.directors) ? [...matchedCredits.directors] : [];
    if (detailData && detailData.directors) {
      for (const d of detailData.directors) {
        if (!finalDirectors.includes(d)) finalDirectors.push(d);
      }
    }
    const finalCredits = {
      presenters: (matchedCredits && matchedCredits.presenters) || [],
      actors: (matchedCredits && matchedCredits.actors) || [],
      directors: finalDirectors
    };

    const isRerun = item.isRerun || (detailData && detailData.isRerun) || (refEntry && refEntry.isRerun) || false;
    const isUnreleased = !isRerun && ((detailData && detailData.isUnreleased) || item.isUnreleased || (refEntry && refEntry.isUnreleased) || false);
    const isLive = !isRerun && ((detailData && detailData.isLive) || item.isLive || false);
    const hasSubtitles = (detailData && detailData.hasSubtitles) || item.hasSubtitles || (refEntry && refEntry.hasSubtitles) || false;
    const hasAudioDescription = (detailData && detailData.hasAudioDescription) || item.hasAudioDescription || false;
    const isHd = (detailData && detailData.isHd) || item.isHd || (refEntry && refEntry.isHd) || false;
    const finalRating = (detailData && detailData.rating) || item.rating || matchedRating || null;

    programs.push({
      channelId: channel.id,
      title: item.title,
      subTitle: matchedSubtitle,
      desc: primaryDesc,
      secondaryDesc: secondaryDesc,
      category: matchedCategory,
      format: detailData ? detailData.format : null,
      genre: detailData ? detailData.genre : null,
      year: detailData && detailData.year ? detailData.year : matchedYear,
      seriesYear: detailData && detailData.seriesYear ? detailData.seriesYear : matchedSeriesYear,
      country: detailData && detailData.country ? detailData.country : matchedCountry,
      season: detailData && detailData.season != null ? detailData.season : matchedSeason,
      episode: detailData && detailData.episodeNumber != null ? detailData.episodeNumber : matchedEpisode,
      start: item.startTime,
      stop: item.startTime.add(60, 'minute'),
      image: item.imageSrc,
      rating: finalRating,
      credits: finalCredits,
      isUnreleased,
      isRerun,
      isLive,
      hasSubtitles,
      hasAudioDescription,
      isHd
    });
  }

  for (let i = 0; i < programs.length - 1; i++) {
    programs[i].stop = programs[i + 1].start;
  }

  return programs;
}

function resolvePrimaryParent(rawCategories, title = '', desc = '', season = null, episode = null) {
  const text = [title, ...rawCategories, (desc || '').substring(0, 120)].join(' ').toLowerCase();

  if (/\b(jt|journal|m[ée]t[ée]o|d[ée]bat|politique|[ée]mission politique|actualit[ée]|information|soir 3|12\.45|19\.45|le 20h|le 13h|culture infos|[ée]dition sp[ée]ciale|soci[ée]t[ée]|interview|en direct)\b/i.test(text) ||
      /magazine d'information|journal t[ée]l[ée]vis[ée]|flash info/i.test(text)) {
    return 'Info';
  }

  if (/\b(sport|football|foot|rugby|tennis|cyclisme|tour de france|basket|handball|volley|athl[ée]tisme|natation|boxe|mma|formule 1|f1|moto|automobile|hippisme|courses? hippiques?|golf|ski|omnisport)\b/i.test(text) ||
      /magazine sportif|championnat|coupe du monde|ligue 1|ligue des champions/i.test(text)) {
    return 'Sport';
  }

  if (/\b(jeunesse|animation|dessin anim[ée]|manga|anime|enfant|enfants|ado|junior|cartoon|pour enfants)\b/i.test(text) ||
      /s[ée]rie d'animation|programme jeunesse/i.test(text)) {
    return 'Jeunesse';
  }

  if (/\b(jeu|jeux|loterie|loto|tirage|euromillions|eurodreams|k[ée]no|quizz?|talk[- ]show|t[ée]l[ée]r[ée]alit[ée]|vari[ée]t[ée]|humour|spectacle|gala|culinaire|cuisine|gastronomie|t[ée]l[ée][- ]achat|mode|d[ée]co|relooking|sketch|stand[- ]up|cirque|magie|one[- ]man[- ]show|people|c[ée]r[ée]monie|divertissement)\b/i.test(text)) {
    return 'Divertissement';
  }

  if (/\b(t[ée]l[ée]film|film|court[- ]m[ée]trage|long[- ]m[ée]trage|cin[ée]ma|biopic|western)\b/i.test(text) ||
      /com[ée]die romantique|drame psychologique/i.test(text)) {
    return 'Film';
  }

  if (/\b(s[ée]rie|feuilleton|soap|sitcom|telenovela|t[ée]l[ée]novela|mini[- ]s[ée]rie|fiction|policier|drame|thriller|science[- ]fiction|fantastique)\b/i.test(text) ||
      /s[ée]rie tv|s[ée]rie dramatique|s[ée]rie polici[èe]re|com[ée]die dramatique/i.test(text) ||
      (season != null || episode != null)) {
    return 'Série';
  }

  if (/\b(documentaire|docu|d[ée]couverte|reportage|enqu[êe]te|animalier|animaux|histoire|civilisation|nature|sciences?|g[ée]opolitique|voyage|faits divers)\b/i.test(text) ||
      /docu[- ]fiction|grands reportages/i.test(text)) {
    return 'Documentaire';
  }

  if (/\b(musique|concert|clip|op[ée]ra|ballet|danse|chanson|pop|rock|jazz|live musical|festival musical)\b/i.test(text) ||
      /vari[ée]t[ée]s? musicales?/i.test(text)) {
    return 'Musique';
  }

  return 'Autre';
}

function sanitizeCategories(rawCategories, primaryParent) {
  const parentNames = ['Film', 'Série', 'Divertissement', 'Sport', 'Info', 'Documentaire', 'Jeunesse', 'Musique', 'Autre'];
  const cleaned = [];

  for (let cat of rawCategories) {
    if (!cat) continue;
    cat = cat.trim();
    if (!cat || cat.toLowerCase() === 'general' || cat.toLowerCase() === 'autre') continue;

    if (/jeunesses[ée]rie/i.test(cat)) {
      cat = "Série d'animation";
    }

    const matchesOtherParent = parentNames.some(p => p.toLowerCase() === cat.toLowerCase());
    if (matchesOtherParent) continue;

    if (primaryParent === 'Documentaire' && /^s[ée]rie( tv| dramatique)?$/i.test(cat)) continue;
    if (primaryParent === 'Film' && /^s[ée]rie$/i.test(cat)) continue;
    if (primaryParent === 'Jeunesse' && /^(sport|info|musique|s[ée]rie)$/i.test(cat)) continue;

    if (!cleaned.includes(cat)) {
      cleaned.push(cat);
    }
  }

  return [primaryParent, ...cleaned];
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
    if (program.secondaryDesc) {
      xml += `    <desc-more lang="fr">${escapeXml(program.secondaryDesc)}</desc-more>\n`;
      xml += `    <desc lang="fr-more">${escapeXml(program.secondaryDesc)}</desc>\n`;
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
    const rawCategories = [];
    if (program.category && program.category !== 'General') rawCategories.push(program.category);
    if (program.format && !rawCategories.includes(program.format)) rawCategories.push(program.format);
    if (program.genre && !rawCategories.includes(program.genre)) rawCategories.push(program.genre);
    if (program.isRerun && !rawCategories.includes('Rediffusion')) rawCategories.push('Rediffusion');
    if (program.isLive && !rawCategories.includes('Direct')) rawCategories.push('Direct');
    if (program.hasAudioDescription && !rawCategories.includes('Audiodescription')) rawCategories.push('Audiodescription');

    const primaryParent = resolvePrimaryParent(rawCategories, program.title || '', program.desc || '', program.season, program.episode);
    const categories = sanitizeCategories(rawCategories, primaryParent);

    for (const cat of categories) {
      xml += `    <category lang="fr">${escapeXml(cat)}</category>\n`;
    }
    if (program.image) {
      xml += `    <icon src="${escapeXml(program.image)}" />\n`;
    }
    if (program.isUnreleased) {
      xml += `    <premiere />\n`;
    }
    if (program.isRerun) {
      xml += `    <previously-shown />\n`;
    }
    if (program.hasSubtitles) {
      xml += `    <subtitles type="teletext" />\n`;
    }
    if (program.hasAudioDescription) {
      xml += `    <audio-description />\n`;
    }
    if (program.isHd) {
      xml += `    <video>\n      <quality>HD</quality>\n    </video>\n`;
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
  process.stdout.write(`Indexed ${refData.timeMap.size} reference descriptions and ${refData.channelProgramsMap.size} channel-day blocks.\n`);

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
        const deduped = [];
        for (const prog of channelPrograms) {
          if (deduped.length === 0) {
            deduped.push(prog);
            continue;
          }
          const prev = deduped[deduped.length - 1];
          if (Math.abs(prog.start.diff(prev.start, 'minute')) < 5) {
            if ((prog.desc || '').length > (prev.desc || '').length) {
              deduped[deduped.length - 1] = prog;
            }
            continue;
          }
          if (prog.start.isBefore(prev.start)) {
            continue;
          }
          deduped.push(prog);
        }
        for (let j = 0; j < deduped.length - 1; j++) {
          deduped[j].stop = deduped[j + 1].start;
        }
        return deduped;
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
