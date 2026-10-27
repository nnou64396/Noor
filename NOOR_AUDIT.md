# Noor (نور) — Technical Audit

## Executive summary

This repository is an Android application built in Kotlin with Jetpack Compose, not a Flutter/React Native or web app. The app is structured around a single `MainActivity`, Compose navigation, Room persistence, DataStore settings, Retrofit/OkHttp network access, Media3 audio playback, and Google Play Services location. The repository contains a real implementation of core Islamic utilities: prayer-time loading, Qibla direction, offline Quran reading/search/bookmarks, Athkar corpus loading, Tasbih counting, and onboarding/settings.

The codebase is more mature than a placeholder and includes production-oriented configuration such as release signing support, lint gates, ProGuard rules for the prayer DTOs, and Android instrumentation tests. However, there are also clear gaps: no prayer-reminder scheduler, no user-selectable calculation method/region setting in the app, and no build verification possible in this environment because Java is not installed.

## 1) Actual technology stack and build system

### Confirmed stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM + repository/service-locator pattern
- Persistence: Room + DataStore Preferences
- HTTP: Retrofit + OkHttp + Gson
- Location: Google Play Services Fused Location Provider
- Audio: Media3 / ExoPlayer
- App entry: `MainActivity` and `NoorApplication`
- Build system: Gradle with Kotlin DSL

### Build configuration evidence

- `build.gradle.kts` sets the app plugin and Kotlin Compose/KSP plugins.
- `settings.gradle.kts` includes the app module and the custom `tools:quran` modules.
- `app/build.gradle.kts` declares:
  - `namespace = "dz.techvision.noor"`
  - `compileSdk = 37`
  - `targetSdk = 37`
  - `minSdk = 26`
  - Java 17 target
  - Compose enabled
  - KSP Room compiler
- `gradle/libs.versions.toml` records the dependency versions:
  - AGP `9.4.1`
  - Kotlin `2.2.10`
  - KSP `2.2.10-2.0.2`
  - Compose `1.12.0`
  - Room `2.8.5`
  - Retrofit `3.0.0`
  - OkHttp `4.12.0`
  - Media3 `1.11.1`
  - Play Services Location `21.3.0`

### Build/run capability

I attempted to validate the project with:

```bash
./gradlew :app:assembleDebug --console=plain
```

and the environment rejected it with:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

I also verified that `java` is unavailable in the current environment (`java : The term 'java' is not recognized`). This means the app cannot be compiled or run in this session, and any runtime claims beyond repository inspection are provisional rather than verified on-device.

## 2) Application architecture

### Main entry points and navigation

- Application bootstrap: [app/src/main/java/com/noor/islamic/NoorApplication.kt](app/src/main/java/com/noor/islamic/NoorApplication.kt)
- Activity entry: [app/src/main/java/com/noor/islamic/MainActivity.kt](app/src/main/java/com/noor/islamic/MainActivity.kt)
- Navigation graph: [app/src/main/java/com/noor/islamic/navigation/NoorNavHost.kt](app/src/main/java/com/noor/islamic/navigation/NoorNavHost.kt)
- Destinations and bottom-bar tabs: [app/src/main/java/com/noor/islamic/navigation/NoorDestinations.kt](app/src/main/java/com/noor/islamic/navigation/NoorDestinations.kt)

The app uses `NavHost` + Compose destinations for:

- Onboarding
- Home
- Quran
- Quran reader/search/bookmarks
- Prayer
- Athkar
- Tasbih
- Qibla
- Settings

### Dependency container and repositories

- Service locator: [app/src/main/java/com/noor/islamic/services/ServiceLocator.kt](app/src/main/java/com/noor/islamic/services/ServiceLocator.kt)

This object wires together:

- `SettingsDataStore`
- Room databases for prayer cache, Quran text, Quran progress, and practice state
- `AndroidLocationRepository`
- Prayer API provider and prayer repository
- Quran audio controller and Media3 engine
- Athkar corpus and Tasbih repositories

## 3) Core feature inventory and status

| Feature | Status | Supporting evidence |
|---|---|---|
| Prayer times | Implemented and largely verified by code/tests | [PrayerViewModel.kt](app/src/main/java/com/noor/islamic/presentation/prayer/PrayerViewModel.kt), [PrayerRepositoryImpl.kt](app/src/main/java/com/noor/islamic/data/repository/PrayerRepositoryImpl.kt), [PrayerScreen.kt](app/src/main/java/com/noor/islamic/presentation/prayer/PrayerScreen.kt), [PrayerScreenTest.kt](app/src/androidTest/java/com/noor/islamic/PrayerScreenTest.kt) |
| Qibla direction | Implemented | [QiblaViewModel.kt](app/src/main/java/com/noor/islamic/presentation/qibla/QiblaViewModel.kt), [QiblaScreen.kt](app/src/main/java/com/noor/islamic/presentation/qibla/QiblaScreen.kt), [QiblaScreenTest.kt](app/src/androidTest/java/com/noor/islamic/QiblaScreenTest.kt) |
| Offline Quran text | Implemented and validated by test | [QuranRepositoryImpl.kt](app/src/main/java/com/noor/islamic/data/local/quran/QuranRepositoryImpl.kt), [QuranReaderDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/quran/QuranReaderDatabase.kt), [QuranAssetDatabaseTest.kt](app/src/androidTest/java/com/noor/islamic/QuranAssetDatabaseTest.kt) |
| Quran search/bookmarking/reading progress | Implemented | [QuranRepositoryImpl.kt](app/src/main/java/com/noor/islamic/data/local/quran/QuranRepositoryImpl.kt), [QuranUserStateDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/userstate/QuranUserStateDatabase.kt), [QuranReaderScreen.kt](app/src/main/java/com/noor/islamic/presentation/quran/QuranReaderScreen.kt) |
| Quran audio recitation | Implemented | [QuranAudioControllerImpl.kt](app/src/main/java/com/noor/islamic/data/audio/QuranAudioControllerImpl.kt), [QuranMediaSessionService.kt](app/src/main/java/com/noor/islamic/data/audio/QuranMediaSessionService.kt), [QuranReaderScreen.kt](app/src/main/java/com/noor/islamic/presentation/quran/QuranReaderScreen.kt) |
| Athkar / adhkar library | Implemented | [AssetAthkarCorpusSource.kt](app/src/main/java/com/noor/islamic/data/local/athkar/AssetAthkarCorpusSource.kt), [app/src/main/assets/athkar/adhkar.json](app/src/main/assets/athkar/adhkar.json) |
| Tasbih counter | Implemented | [TasbihScreen.kt](app/src/main/java/com/noor/islamic/presentation/tasbih/TasbihScreen.kt), [TasbihPracticeRepository.kt](app/src/main/java/com/noor/islamic/data/local/userdata/TasbihPracticeRepository.kt) |
| Settings + onboarding + localization | Implemented | [MainViewModel.kt](app/src/main/java/com/noor/islamic/presentation/MainViewModel.kt), [SettingsDataStore.kt](app/src/main/java/com/noor/islamic/data/local/datastore/SettingsDataStore.kt), [locales_config.xml](app/src/main/res/xml/locales_config.xml) |
| Prayer reminders / scheduling | Not found in the repository | Search found no `WorkManager`, `AlarmManager`, or persistent reminder scheduler. |
| User-selectable prayer calculation method / region settings | Partially implemented only via default constant; no settings UI found | [CalculationMethod.kt](app/src/main/java/com/noor/islamic/domain/model/CalculationMethod.kt) |
| Full release check / compile validation | Unable to verify in this environment | Gradle build blocked by missing Java installation |

## 4) Detailed feature audit

### A. Prayer Times

Status: Implemented with limitations.

What is present

- Prayer data is loaded through the public AlAdhan API via Retrofit in [PrayerApi.kt](app/src/main/java/com/noor/islamic/data/remote/prayer/PrayerApi.kt) and [PrayerApiProvider.kt](app/src/main/java/com/noor/islamic/data/remote/prayer/PrayerApiProvider.kt).
- The cache is in Room, keyed by date + coarse location + calculation method: [NoorDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/NoorDatabase.kt), [PrayerCacheKey.kt](app/src/main/java/com/noor/islamic/data/local/PrayerCacheKey.kt).
- The app uses `PrayerViewModel` to request fine/coarse location, load the schedule, and compute the next prayer countdown: [PrayerViewModel.kt](app/src/main/java/com/noor/islamic/presentation/prayer/PrayerViewModel.kt).
- `PrayerScheduleMath` handles next-prayer selection and countdown in the prayer’s timezone: [PrayerScheduleMath.kt](app/src/main/java/com/noor/islamic/domain/logic/PrayerScheduleMath.kt).
- The UI shows permission states, loading states, fallback cache states, and errors: [PrayerScreen.kt](app/src/main/java/com/noor/islamic/presentation/prayer/PrayerScreen.kt).

Evidence of limitations and gaps

- The calculation method is fixed in code to `MUSLIM_WORLD_LEAGUE` and the fiqh school is fixed to the standard Asr school: [CalculationMethod.kt](app/src/main/java/com/noor/islamic/domain/model/CalculationMethod.kt).
- There is no user-facing setting for region, method selection, or fiqh school in the repository.
- The feature is designed for a single live location fetch and does not appear to schedule prayer reminders or notifications.
- I could not verify live prayer accuracy against a device or external service in this environment.

### B. Qibla direction

Status: Implemented.

Evidence

- Permission-managed location + heading sensor lifecycle: [QiblaViewModel.kt](app/src/main/java/com/noor/islamic/presentation/qibla/QiblaViewModel.kt)
- Compass UI with permission requests and app-settings fallback: [QiblaScreen.kt](app/src/main/java/com/noor/islamic/presentation/qibla/QiblaScreen.kt)
- Bearing calculation: [QiblaMath.kt](app/src/main/java/com/noor/islamic/domain/logic/QiblaMath.kt)
- Sensor provider: [HeadingSensor.kt](app/src/main/java/com/noor/islamic/data/local/sensor/HeadingSensor.kt)

This is a real implementation rather than placeholder UI; the code calls the sensor and computes a bearing from the user’s location.

### C. Quran

Status: Implemented and validated by repository-based tests.

Evidence

- The database is prebuilt from source data and loaded as a Room database asset: [tools/quran/importer/build.gradle.kts](tools/quran/importer/build.gradle.kts)
- The app exposes a Room-backed Quran repository: [QuranRepositoryImpl.kt](app/src/main/java/com/noor/islamic/data/local/quran/QuranRepositoryImpl.kt)
- The offline reader UI and navigation are real: [QuranReaderScreen.kt](app/src/main/java/com/noor/islamic/presentation/quran/QuranReaderScreen.kt), [QuranScreen.kt](app/src/main/java/com/noor/islamic/presentation/quran/QuranScreen.kt)
- The test confirms the asset opens and serves reads, including 114 surahs and FTS4 prefix search: [QuranAssetDatabaseTest.kt](app/src/androidTest/java/com/noor/islamic/QuranAssetDatabaseTest.kt)

This is one of the strongest implemented areas of the app.

### D. Quran audio recitation

Status: Implemented.

Evidence

- Shared playback controller: [QuranAudioControllerImpl.kt](app/src/main/java/com/noor/islamic/data/audio/QuranAudioControllerImpl.kt)
- Media3 service and notification integration: [QuranMediaSessionService.kt](app/src/main/java/com/noor/islamic/data/audio/QuranMediaSessionService.kt)
- Reciter catalog and audio URLs: [EveryAyahReciterCatalog.kt](app/src/main/java/com/noor/islamic/data/audio/EveryAyahReciterCatalog.kt)
- Reader requests notification permission before media playback when required: [QuranReaderScreen.kt](app/src/main/java/com/noor/islamic/presentation/quran/QuranReaderScreen.kt)

The audio stack is real and app-scoped. It is more mature than a static placeholder, though I could not verify live streaming playback in this environment.

### E. Athkar / adhkar

Status: Implemented.

Evidence

- Bundled corpus in asset JSON: [app/src/main/assets/athkar/adhkar.json](app/src/main/assets/athkar/adhkar.json)
- Asset loader and validation before display: [AssetAthkarCorpusSource.kt](app/src/main/java/com/noor/islamic/data/local/athkar/AssetAthkarCorpusSource.kt)
- UI index + category browsing: [AthkarScreen.kt](app/src/main/java/com/noor/islamic/presentation/athkar/AthkarScreen.kt), [AthkarCategoryScreen.kt](app/src/main/java/com/noor/islamic/presentation/athkar/AthkarCategoryScreen.kt)

This appears to be an actual localized, offline corpus with progress tracking rather than a mock screen.

### F. Tasbih / dhikr counter

Status: Implemented.

Evidence

- Counter and target UI: [TasbihScreen.kt](app/src/main/java/com/noor/islamic/presentation/tasbih/TasbihScreen.kt)
- Repository layer: [TasbihPracticeRepository.kt](app/src/main/java/com/noor/islamic/data/local/userdata/TasbihPracticeRepository.kt)

This is a fully represented local-use feature; it stores counter state in Room/SQLite and exposes preset/custom counts.

### G. Settings and onboarding

Status: Implemented.

Evidence

- Settings are persisted via DataStore: [SettingsDataStore.kt](app/src/main/java/com/noor/islamic/data/local/datastore/SettingsDataStore.kt)
- Onboarding is applied before the Home screen is shown: [NoorApp.kt](app/src/main/java/com/noor/islamic/navigation/NoorApp.kt), [MainViewModel.kt](app/src/main/java/com/noor/islamic/presentation/MainViewModel.kt)
- Locales include Arabic, English, and French: [locales_config.xml](app/src/main/res/xml/locales_config.xml)

This is a working settings architecture. It is not a placeholder.

### H. Notification/reminder functionality

Status: Not found in the repository.

Evidence

- No `WorkManager`, `AlarmManager`, `NotificationManager`, or scheduled reminder implementation is present in `app/src/main/java`.
- The search results show media-notification support only for Quran playback, not prayer reminders.

This is a real missing feature relative to the app’s stated value proposition.

## 5) Storage and data model inventory

The application persists data in a few local stores:

- Prayer timing cache: Room database (`noor.db`) with Room schema version 2: [NoorDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/NoorDatabase.kt)
- Quran text and FTS/search data: Room asset-backed DB: [QuranReaderDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/quran/QuranReaderDatabase.kt)
- Quran user state (bookmarks/progress): [QuranUserStateDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/userstate/QuranUserStateDatabase.kt)
- Practice state (Athkar + Tasbih sessions): [PracticeStateDatabase.kt](app/src/main/java/com/noor/islamic/data/local/db/practice/PracticeStateDatabase.kt)
- Settings and theme/language state: DataStore: [SettingsDataStore.kt](app/src/main/java/com/noor/islamic/data/local/datastore/SettingsDataStore.kt)

This is a coherent offline-first design, with the Quran text and Athkar corpus being bundled assets rather than network-driven content.

## 6) Testing and QA infrastructure

The project contains both unit and instrumentation coverage:

- Android Compose UI tests: [HomeScreenTest.kt](app/src/androidTest/java/com/noor/islamic/HomeScreenTest.kt), [PrayerScreenTest.kt](app/src/androidTest/java/com/noor/islamic/PrayerScreenTest.kt), [QiblaScreenTest.kt](app/src/androidTest/java/com/noor/islamic/QiblaScreenTest.kt), [TasbihScreenTest.kt](app/src/androidTest/java/com/noor/islamic/TasbihScreenTest.kt), [MainActivitySmokeTest.kt](app/src/androidTest/java/com/noor/islamic/MainActivitySmokeTest.kt)
- Quran asset database test: [QuranAssetDatabaseTest.kt](app/src/androidTest/java/com/noor/islamic/QuranAssetDatabaseTest.kt)
- Unit test dependencies exist in the Gradle catalog and are included in the app module.

Despite this, the repo is not currently verifiable end-to-end in this environment because Java/Android build tools are unavailable.

## 7) Security and privacy notes

Evidence suggests the project is aware of privacy risks:

- Prayer cache avoids storing raw latitude/longitude and instead hashes rounded values in a digest: [PrayerCacheKey.kt](app/src/main/java/com/noor/islamic/data/local/PrayerCacheKey.kt)
- OkHttp request logging redacts latitude/longitude and credential headers: [NetworkModule.kt](app/src/main/java/com/noor/islamic/data/remote/network/NetworkModule.kt)
- Release signing is externalized and not committed to the repository: [RELEASE.md](RELEASE.md), [app/build.gradle.kts](app/build.gradle.kts)

This is a better-than-average privacy posture for a location-aware app.

## 8) Main strengths

- Coherent architecture and real code paths, not a stubbed app
- Kotlin + Compose stack is modern and maintainable
- Offline-first design is appropriate for Quran and prayer cache
- Strong use of Room/Datastore/Media3/Compose patterns
- Release documentation is thoughtful and security-aware
- QA structure exists, including test files and asset-validation tests

## 9) Main gaps and risks

1. No prayer-reminder scheduler or notification feature is implemented.
2. Prayer calculation method is hardcoded instead of being user-configurable by geography or preference.
3. The app may not be buildable in a fresh environment without Java/Android SDK setup.
4. The repository is mostly code and tests, but there is no full product roadmap or QA runbook beyond the app code.
5. Runtime verification remains impossible without a device/emulator and the missing Java toolchain.

## 10) Prioritized recommendations

1. Add a proper reminder system with WorkManager or AlarmManager for daily prayer alerts, respecting Android battery/Doze policies.
2. Expose prayer method, region, and school settings in the settings UI, defaulting to the current behavior but allowing user override.
3. Add CI validation that builds the app in a Java-configured environment, not only local IDE execution.
4. Cover more edge cases around timezone/day-boundary transitions, location denial, no-network fallback, and permission upgrades.
5. Add a short product README explaining app capabilities, data flows, and how to run/build the project on a real machine.

## Final assessment

The repository is not a mock project or empty shell. It is a real Kotlin Android app with a credible Compose architecture, local offline data, remote prayer integration, audio playback, and substantial Islamic utility features. The strongest areas are Quran access and prayer-time handling. The largest product gap is a lack of prayer-notification/reminder functionality and user-configurable regional calculation settings. The major verification limitation is environmental: the project cannot be compiled in this session because Java is absent.

