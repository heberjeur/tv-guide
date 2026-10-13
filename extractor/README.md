# TV Guide EPG Extractor

Automated electronic program guide (EPG) generator for French TV channels.

## Features
- Generates standard XMLTV-compliant EPG files.
- Extracts program titles, subtitles, episode descriptions, genres, and icons.
- Automatically compresses output with GZIP (`epg_tnt.xml.gz`).
- Runs daily via GitHub Actions and deploys to GitHub Pages for instant offline syncing.

## Output
- Uncompressed: `output/epg_tnt.xml`
- Compressed: `output/epg_tnt.xml.gz`
