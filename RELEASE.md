# Release Guide

## Building

```bash
./gradlew :app:assembleRelease   # APK
./gradlew :app:bundleRelease     # AAB for distribution
```

Both apply R8 with code shrinking and resource shrinking. Lint is a hard gate (`abortOnError = true`).

## Signing

No keystore, key password, or store password is committed to this repository. Supply the four values below, either as environment variables or in the untracked `local.properties` at the repository root:

| Name | Meaning |
| --- | --- |
| `NOOR_RELEASE_STORE_FILE` | Path to the keystore. Absolute, or relative to the repository root. |
| `NOOR_RELEASE_STORE_PASSWORD` | Keystore password. |
| `NOOR_RELEASE_KEY_ALIAS` | Key alias inside the keystore. |
| `NOOR_RELEASE_KEY_PASSWORD` | Key password for that alias. |

## Production Signing Certificate Fingerprint

The production signing certificate SHA-256 fingerprint is:
`6c4e68e4f8a1bc982a09611886f0eda25893642e24f14fa77642ea1a6b3ef83d`

## Versioning

`versionCode` and `versionName` live in the `defaultConfig` block of `app/build.gradle.kts`. Increment `versionCode` monotonically for every release.

## Application Updates

Application updates are distributed exclusively through officially supported app stores (such as Google Play or other official distribution channels). In-app check for updates directs users to the official store listing.

## Publishing a New Release

1. Increment `versionCode` and update `versionName` in `app/build.gradle.kts`.
2. Commit changes and push to `main`.
3. Create and push a version tag starting with `v` (e.g., `git tag v1.1.0 && git push origin v1.1.0`).
4. The GitHub Actions workflow (`.github/workflows/release.yml`) automatically builds the signed APK/AAB and attaches release assets to the official GitHub Release.
