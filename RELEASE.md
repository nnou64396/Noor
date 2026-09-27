# Release

## Building

```bash
./gradlew :app:assembleRelease   # APK
./gradlew :app:bundleRelease     # AAB for Play
```

Both apply R8 with code shrinking and resource shrinking. Lint is a hard gate
(`abortOnError = true`), and `assembleRelease` runs `lintVitalRelease`, so a
release cannot be produced while lint errors exist.

## Signing

No keystore, key password, or store password is committed to this repository, and
none is needed to build. `assembleRelease` without credentials produces
`app/build/outputs/apk/release/app-release-unsigned.apk`; `bundleRelease` produces
an unsigned `.aab`. The app is **not** distributed from this repository as-is.

A release owner must supply a real upload/production key out of band. Supply the
four values below, either as environment variables or in the untracked
`local.properties` at the repository root. Environment variables take precedence.

| Name | Meaning |
| --- | --- |
| `NOOR_RELEASE_STORE_FILE` | Path to the keystore. Absolute, or relative to the repository root. |
| `NOOR_RELEASE_STORE_PASSWORD` | Keystore password. |
| `NOOR_RELEASE_KEY_ALIAS` | Key alias inside the keystore. |
| `NOOR_RELEASE_KEY_PASSWORD` | Key password for that alias. |

`local.properties` is already gitignored, and the same is true for `*.jks`,
`*.keystore`, `*.p12`, `*.pfx`, and `*.key`. Never commit a keystore or a
password, and never paste a password into a tracked file.

Two failure modes are deliberate:

- **Partially configured** (some of the four set, others missing) fails the build
  and names the missing variables. It never silently downgrades to an unsigned
  release.
- **Store file missing** fails the build, rather than emitting an unsigned
  artifact that could be mistaken for a real one.

With all four set, the outputs become `app-release.apk` and `app-release.aab`
instead of the `-unsigned` APK; an unsigned `.aab` keeps the plain name, so
confirm the signed artifact is what you are about to upload. Debug builds never
read these values and never require credentials.

## Versioning

`versionCode` and `versionName` live in the `defaultConfig` block of
`app/build.gradle.kts` (`1` / `0.1.0`). Bump `versionCode` for every upload; a
replayed code is rejected by Google Play.

## R8 and Gson

Gson binds JSON keys to Kotlin properties by field name at runtime, which R8
cannot see. The AlAdhan response DTOs in
`app/src/main/java/com/noor/islamic/data/remote/prayer/TimingsDto.kt` are
therefore listed explicitly in `app/proguard-rules.pro`. Retrofit's bundled
consumer rules keep the service interface, not the response types, so without
those rules a minified release silently fails to parse prayer times while unit
tests keep passing against unminified classes.

**Any new DTO added to the remote layer must be added to that keep list.** After
changing it, confirm the DTO survived minification:

```bash
./gradlew :app:assembleRelease
grep TimingsResponse app/build/outputs/mapping/release/seeds.txt
```

An empty result means the class was stripped or renamed.
