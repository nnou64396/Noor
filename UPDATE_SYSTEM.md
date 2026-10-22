# NOOR Store-Independent Update Foundation

This document describes the lightweight, store-independent update check foundation built into the NOOR Android application.

## Overview

NOOR includes a flexible update detection foundation that can check a remote, HTTPS-hosted JSON manifest for newer app versions. It is designed to be store-agnostic, non-intrusive, and resilient to network or server failures.

## Update Manifest Schema

The update service expects a static JSON file hosted at a designated HTTPS endpoint.

### JSON Format Specification

```json
{
  "latestVersion": "0.2.0",
  "latestVersionCode": 2,
  "minimumSupportedVersionCode": 1,
  "releaseNotes": {
    "en": "Bug fixes, performance improvements, and updated prayer timing logic.",
    "ar": "إصلاح الأخطاء وتحسين الأداء وتحديث حساب مواقيت الصلاة.",
    "fr": "Corrections de bugs, améliorations des performances et mise à jour des heures de prière."
  },
  "downloadUrl": "https://example.com/noor/releases/noor-0.2.0.apk",
  "publishedAt": "2026-03-01T12:00:00Z"
}
```

### Field Definitions

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `latestVersion` | String | Yes | Display version string (e.g. `"0.2.0"`). |
| `latestVersionCode` | Long / Int | Yes | Numeric Android `versionCode` of the latest release. Used for version comparisons. |
| `minimumSupportedVersionCode` | Long / Int | Yes | Lowest `versionCode` still supported without a recommended update notice. |
| `releaseNotes` | Object (Map) | No | Map of language codes (`"en"`, `"ar"`, `"fr"`) to localized release note strings. |
| `downloadUrl` | String | No | URL where the user can download the updated app or view release page. |
| `publishedAt` | String | No | ISO-8601 timestamp string representing when the update was published. |

---

## How Version Checks Work

### Version Code Comparisons
Version checks compare numeric `versionCode` values (`defaultConfig.versionCode` in Gradle) rather than parsing version name strings. This avoids bugs with string formatting.

Let `currentVersionCode` be the installed app's numeric version code:

1. **`currentVersionCode >= latestVersionCode`**:
   - Status: `UpToDate`. The app is on the latest version.
2. **`currentVersionCode < minimumSupportedVersionCode`**:
   - Status: `UpdateRequired`. A critical or recommended update is available. Prominent non-blocking update notification is shown.
3. **`currentVersionCode < latestVersionCode`** (and `>= minimumSupportedVersionCode`):
   - Status: `UpdateAvailable`. An optional update is available. Dismissible banner is shown on the Home screen.
4. **Endpoint Unconfigured or Null**:
   - Status: `Disabled`. No network calls are made.
5. **Network Failure / Offline / Malformed JSON**:
   - Status: `CheckUnavailable`. Failures are caught safely and never crash the app or block core features.

---

## Configuration & Activation

### Default Behavior (Prepared / Unconfigured)
By default, no remote manifest URL is configured (`MANIFEST_URL = null`). In this state:
* Remote checking is **disabled**.
* The app operates entirely offline without making update HTTP requests.
* UI indicates the application is up to date or update checking is unconfigured.

### Enabling Remote Updates
To enable update checking, configure the manifest URL in `UpdateRepositoryImpl.kt` or supply it via `local.properties` / build configuration:

```kotlin
// Example in UpdateRepositoryImpl.kt or ServiceLocator.kt
const val MANIFEST_URL = "https://raw.githubusercontent.com/nnou64396/Noor/main/release-manifest.json"
```

---

## Security & Package Installation Rules

1. **Explicit User Consent**: NOOR never automatically downloads or silently installs APK files.
2. **External Link Delegation**: When the user taps "Get Update", the application delegates the `downloadUrl` to the Android system browser (`Intent.ACTION_VIEW`), allowing the user to inspect and approve downloads.
3. **Compatible Signing Requirement**: Android enforces that any APK updated over an existing installation must be signed with the exact same signing key/keystore. Updates downloaded outside Google Play must match the installed app's signature.
4. **Privacy**: Update requests make plain GET calls without transmitting user coordinates, credentials, or telemetry.

---

## Future Store Integration Paths

When a specific distribution channel is selected in the future, store-specific update handling can be added as follows:

* **Google Play**: Integrate `com.google.android.play:app-update` (In-App Updates API) into `UpdateRepository`.
* **RuStore**: Integrate RuStore App Update SDK (`ru.rustore.sdk:appupdate`).
* **GitHub Releases**: Parse release tags from GitHub API (`https://api.github.com/repos/user/repo/releases/latest`) into the same `AppUpdateInfo` model.
