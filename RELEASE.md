# Release & Automatic Update Guide

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

## Trusted Production Signing Certificate Fingerprint

The trusted production signing certificate SHA-256 fingerprint configured for secure in-app APK verification is:
`6c4e68e4f8a1bc982a09611886f0eda25893642e24f14fa77642ea1a6b3ef83d`

## Versioning

`versionCode` and `versionName` live in the `defaultConfig` block of `app/build.gradle.kts`. Increment `versionCode` monotonically for every release.

## Automatic Update System via GitHub Releases

NOOR includes a secure, store-independent automatic update system that integrates with GitHub Releases (`https://api.github.com/repos/nnou64396/Noor/releases`).

### How It Works

1. **Release Discovery**: The app queries the GitHub Releases API for the latest stable non-draft, non-prerelease release.
2. **Metadata & Manifest**: Releases can include an attached `noor-update.json` manifest specifying `versionName`, `versionCode`, `minimumSupportedVersionCode`, `sha256`, and release notes. If absent, release tag and APK assets are parsed directly.
3. **Secure Download**: The APK is downloaded over HTTPS into app-private cache with progress tracking and size limits.
4. **Integrity & Signature Verification**:
   - Incremental SHA-256 calculation matches the published release checksum.
   - Package name is verified as `com.noor.islamic`.
   - Version code is verified as newer than the installed app.
   - Signing certificate is verified against the trusted production fingerprint (`6c4e68e4f8a1bc982a09611886f0eda25893642e24f14fa77642ea1a6b3ef83d`).
5. **Installation**: Secure `FileProvider` shares the verified APK with Android's package installer (`ACTION_VIEW`).
6. **Background Checks**: WorkManager performs periodic background update checks (every 12 hours with network constraints).

## Publishing a New Release

1. Increment `versionCode` and update `versionName` in `app/build.gradle.kts`.
2. Commit changes and push to `main`.
3. Create and push a version tag starting with `v` (e.g., `git tag v1.1.0 && git push origin v1.1.0`).
4. The GitHub Actions workflow (`.github/workflows/release.yml`) automatically builds the signed APK/AAB, computes the SHA-256 checksum, generates `noor-update.json`, and attaches all assets to the official GitHub Release.
