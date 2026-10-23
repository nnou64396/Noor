# NOOR (نور)

NOOR is a quiet, privacy-focused Islamic companion application for Android, bringing the Quran, prayer times, Qibla direction, Athkar, and Tasbih into a modern user experience built with Jetpack Compose and Material 3.

## Features

- **Quran Reading**: Offline access to the Uthmani text with full surah browsing, FTS prefix search, bookmarks, and reading progress tracking.
- **Quran Audio**: Verse-by-verse recitation streaming from multiple reciters with system media session controls and foreground service support.
- **Prayer Times**: Daily prayer times with automatic location detection via Google Play Services Fused Location Provider and offline caching.
- **Qibla Direction**: Compass-based direction finder integrating device sensors and geographic bearing calculations.
- **Athkar**: Morning, evening, and daily supplications with interactive repetition tracking and bundled offline content.
- **Tasbih**: Digital dhikr counter with customizable session targets and persistent history.
- **Localization**: Full support for Arabic, English, and French interfaces.
- **Theme System**: Light, dark, and system theme options.

## Build Requirements

- Java 17
- Android SDK with compileSdk 37
- Gradle 9.6.0 (via Gradle wrapper)

### Building Debug and Release Builds

```bash
# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APK
./gradlew :app:assembleRelease

# Build Release App Bundle (AAB)
./gradlew :app:bundleRelease
```

## Third-Party Content & Attributions

### Quran Text
- **Source**: Tanzil Quran Text - Uthmani v1.1 ([tanzil.net](https://tanzil.net))
- **License**: Licensed under Creative Commons Attribution 3.0 Unported (CC BY 3.0).
- **Status**: Bundled as an offline SQLite database asset.

### Quran Audio
- **Source**: EveryAyah ([everyayah.com](https://everyayah.com)) and VerseByVerseQuran.com
- **Terms**: Streaming requires attribution and link-back to the source site.
- **Status**: Streamed on-demand; no local audio distribution.

### Athkar / Adhkar
- **Source**: Based on *Hisn Al-Muslim* (حصن المسلم) by Sa'id bin Wahf Al-Qahtani, sourced via [rn0x/Adhkar-json](https://github.com/rn0x/Adhkar-json).
- **Usage Statement**: The source repository publicly grants free use ("هذه الملفات بدون رخصة استخدام، يحق لك فعل ما تريد بها مجانا وبدون أي مسائلة قانونية").
- **Status**: Bundled as an offline JSON dataset with in-app attribution.

## License

This repository does not grant a license to use, copy, modify, or redistribute the NOOR source code. All rights are reserved by the copyright holder, except where applicable law or third-party licenses provide otherwise.
