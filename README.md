# NOOR (نور)

A quiet Islamic companion for Android, bringing the Quran, prayer times, Qibla direction, Athkar, and Tasbih into a modern, privacy-focused experience.

## Features

- **Quran Reading**: Offline Quran text with full surah and ayah access, search, bookmarks, and reading progress tracking
- **Quran Audio**: Verse-by-verse recitation streaming from multiple reciters with lockscreen media controls
- **Prayer Times**: Accurate daily prayer times with automatic location detection and configurable calculation methods
- **Qibla Direction**: Compass-based Qibla direction finder with sensor integration
- **Athkar**: Morning and evening supplications with progress tracking
- **Tasbih**: Digital dhikr counter with custom targets and session history
- **Localization**: Arabic, English, and French language support
- **Theme System**: Light, dark, and system theme options with a premium blue design

## Technology Stack

- **Language**: Kotlin 2.2.10
- **UI**: Jetpack Compose 1.12.0 with Material 3
- **Architecture**: MVVM with Repository pattern and Service Locator
- **Persistence**: Room 2.8.5 + DataStore Preferences 1.2.1
- **Networking**: Retrofit 3.0.0 + OkHttp 4.12.0
- **Location**: Google Play Services Location 21.3.0
- **Audio**: Media3/ExoPlayer 1.11.1
- **Build**: Gradle 9.6.0 with Kotlin DSL, Android Gradle Plugin 9.4.1
- **Target SDK**: 37 (Android 14+)
- **Minimum SDK**: 26 (Android 8.0+)

## Prerequisites

- Java 17
- Android SDK with compileSdk 37
- Android Studio Hedgehog (2023.1.1) or later

## Building

The project uses the Gradle wrapper for consistent builds.

### Debug Build

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew :app:assembleRelease   # APK
./gradlew :app:bundleRelease     # AAB for Play Store
```

Release builds apply R8 code shrinking and resource shrinking. Lint is configured as a hard gate (`abortOnError = true`), so a release cannot be produced while lint errors exist.

### Signing

No keystore or passwords are committed to this repository. The build is designed to produce unsigned artifacts when signing credentials are not configured:

- `assembleRelease` without credentials produces `app-release-unsigned.apk`
- `bundleRelease` without credentials produces an unsigned `.aab`

To sign a release, configure these values either as environment variables or in the untracked `local.properties` file:

| Environment Variable | Meaning |
|---------------------|---------|
| `NOOR_RELEASE_STORE_FILE` | Path to the keystore (absolute or relative to repository root) |
| `NOOR_RELEASE_STORE_PASSWORD` | Keystore password |
| `NOOR_RELEASE_KEY_ALIAS` | Key alias inside the keystore |
| `NOOR_RELEASE_KEY_PASSWORD` | Key password for that alias |

See [RELEASE.md](RELEASE.md) for detailed signing instructions.

## Testing

### Unit Tests

```bash
./gradlew :app:test
```

### Instrumentation Tests

```bash
./gradlew :app:connectedAndroidTest
```

## Privacy and Security

- **Location Privacy**: Prayer cache uses SHA-256 hashing of location data; raw coordinates are never stored in cleartext
- **Network Logging**: HTTP logging is enabled only for debuggable builds and redacts location parameters and credential headers
- **Backup Rules**: Cloud backup and device transfer are configured to exclude the prayer cache; only user bookmarks, reading progress, and practice counters are preserved
- **Release Signing**: All signing credentials are externalized and never committed to version control

## Third-Party Content

### Quran Text
- **Source**: Tanzil Quran Text - Uthmani v1.1 (tanzil.net)
- **Attribution**: CC BY 3.0 with attribution requirement
- **Status**: Text is bundled as an offline database

### Quran Audio
- **Source**: EveryAyah (everyayah.com) and VerseByVerseQuran.com
- **Attribution**: Streaming requires link-back to source site
- **Status**: Audio is streamed on-demand; no local distribution

### Athkar/Adhkar
- **Source**: Based on "Hisn Al-Muslim" (حصن المسلم) by Sa'id bin Wahf Al-Qahtani
- **Attribution**: rn0x/Adhkar-json repository
- **Status**: Content is bundled as an offline JSON dataset

## Status

**Version**: 0.1.0 (pre-release)

This is an initial release of NOOR. The core features are implemented and functional, but the app is still under active development.

## Contributing

Contributions are welcome. Please ensure any pull requests:
- Pass all existing tests
- Follow the existing code style and architecture
- Include appropriate tests for new features
- Update documentation as needed

## License

**IMPORTANT**: This repository does not yet have a code license. Until a license is added, all rights are reserved by default. Please contact the project owner before using, modifying, or distributing this code.

The third-party content listed above has its own licensing requirements that are independent of any future code license chosen for this repository.
