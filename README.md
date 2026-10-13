# TV Guide

A native, offline-first Android TV Guide application for French television channels. Designed with zero tracking, zero cookies, zero WebViews, and strict adherence to open-source privacy standards (F-Droid and Google Play compliant).

## Features
- **100% Offline-First**: All EPG schedules, channels, programs, and episode descriptions are stored locally in SQLite via Room.
- **Rich Episode Details**: Displays episode titles, season and episode numbers, detailed synopses, and credits.
- **Alerts and Notifications**: Set custom reminders before programs air, with system notifications and alarm scheduling.
- **Calendar Integration**: Synchronize program reminders directly with the Android system calendar.
- **Channel Packages**: Choose between Enriched Community, Free Terrestrial (TNT), Extended, or All Channels.
- **Automated EPG Generation**: Integrated GitHub Actions pipeline in `extractor/` that automatically updates rich XMLTV feeds daily and publishes to GitHub Pages.

## Architecture
- **Language**: Kotlin 2.0 (Pure idiomatic Kotlin, 100% English identifiers and types)
- **UI**: Jetpack Compose with Material 3 (Dark theme, fluid animations)
- **Database**: Room Database with SQL indices, full-text search, and offline caching
- **Background Work**: Android WorkManager for periodic background synchronization and alarm management
- **Dependency Injection**: Dagger Hilt
- **Network & Parsing**: OkHttp with streaming `XmlPullParser` and native GZIP decompression

## Project Structure
```
TV Guide/
├── app/                  # Android application module
│   ├── src/main/java/    # Clean architecture: data, domain, ui
│   └── src/main/res/     # Resources, themes, drawables, strings
├── extractor/            # Node.js EPG extractor for rich metadata
│   ├── src/              # Scraper and XMLTV generator
│   └── output/           # Generated epg_tnt.xml and epg_tnt.xml.gz
└── .github/workflows/    # Automated CI/CD daily EPG generation
```

## Building
```bash
./gradlew assembleFastDebug
```
or for release:
```bash
./gradlew assembleRelease
```
