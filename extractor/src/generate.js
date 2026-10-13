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

const HTTP_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
  'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
  'Accept-Language': 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7'
};

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

async function fetchChannelSchedule(channel, date) {
  const formattedDate = date.format('YYYY-MM-DD');
  const url = `https://www.programme-tv.net/programme/chaine/${formattedDate}/programme-${channel.siteId}.html`;
  try {
    const response = await axios.get(url, { headers: HTTP_HEADERS, timeout: 15000 });
    return parseHtmlSchedule(channel, response.data, date);
  } catch {
    return [];
  }
}

function parseHtmlSchedule(channel, html, baseDate) {
  const $ = cheerio.load(html);
  const programs = [];
  const items = $('.mainBroadcastCard, .gridBroadcastCard, [class*="broadcastCard"]');

  items.each((_, el) => {
    const $el = $(el);
    const title = $el.find('.mainBroadcastCard-title, .gridBroadcastCard-title, h3, a[title]').first().text().trim();
    if (!title) return;

    const subTitle = $el.find('.mainBroadcastCard-subtitle, .gridBroadcastCard-subtitle, .subtitle').first().text().trim();
    const desc = $el.find('.mainBroadcastCard-summary, .gridBroadcastCard-summary, p, .synopsis').first().text().trim();
    const category = $el.find('.mainBroadcastCard-genre, .gridBroadcastCard-genre, .genre').first().text().trim();
    const timeText = $el.find('.mainBroadcastCard-startingHour, .gridBroadcastCard-startingHour, time, .hour').first().text().trim();
    const imageSrc = $el.find('img').first().attr('src') || $el.find('img').first().attr('data-src') || '';

    const timeMatch = timeText.match(/(\d{1,2})[h:](\d{2})/i);
    if (!timeMatch) return;

    const hours = parseInt(timeMatch[1], 10);
    const minutes = parseInt(timeMatch[2], 10);
    const startTime = baseDate.hour(hours).minute(minutes).second(0);

    programs.push({
      channelId: channel.id,
      title,
      subTitle: subTitle || null,
      desc: desc || null,
      category: category || 'General',
      start: startTime,
      stop: startTime.add(60, 'minute'),
      image: imageSrc.startsWith('//') ? `https:${imageSrc}` : imageSrc
    });
  });

  for (let i = 0; i < programs.length - 1; i++) {
    programs[i].stop = programs[i + 1].start;
  }

  return programs;
}

function buildXml(channels, programs) {
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<!DOCTYPE tv SYSTEM "xmltv.dtd">\n';
  xml += '<tv source-info-name="TV Guide Enriched Extractor" source-info-url="https://github.com/heberjeur/tv-guide">\n';

  for (const channel of channels) {
    xml += `  <channel id="${escapeXml(channel.id)}">\n`;
    xml += `    <display-name>${escapeXml(channel.name)}</display-name>\n`;
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
    if (program.category) {
      xml += `    <category lang="fr">${escapeXml(program.category)}</category>\n`;
    }
    if (program.image) {
      xml += `    <icon src="${escapeXml(program.image)}" />\n`;
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

  const today = dayjs().tz('Europe/Paris');
  const allPrograms = [];

  for (const channel of CHANNELS) {
    for (let dayOffset = 0; dayOffset < 3; dayOffset++) {
      const targetDate = today.add(dayOffset, 'day');
      const channelPrograms = await fetchChannelSchedule(channel, targetDate);
      allPrograms.push(...channelPrograms);
    }
  }

  const xmlContent = buildXml(CHANNELS, allPrograms);
  fs.writeFileSync(OUTPUT_XML, xmlContent, 'utf-8');

  const gzippedContent = zlib.gzipSync(Buffer.from(xmlContent, 'utf-8'), { level: 9 });
  fs.writeFileSync(OUTPUT_GZ, gzippedContent);
}

main().catch((err) => {
  process.stderr.write(`${err.message}\n`);
  process.exit(1);
});
