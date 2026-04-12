# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Android client for AmneziaWG — a WireGuard fork with additional obfuscation capabilities. Multi-module Gradle project with native code in Go and C.

**IMPORTANT: this is the `legacy/api19` branch** — a legacy build targeting **Android 4.4 / API 19 only**. For modern Android (5.0+), see the `master` branch.

> [!WARNING]
> **API 19 ONLY. Do not add, keep, or restore Android > 19 functionality in this branch.**
> If code, manifest entries, resources, dependencies, or lint suppressions are only useful for API 20+ / 21+ / 23+ / 24+ / 25+ / 28+ / 30+, remove them or replace them with an API19-safe no-op/simplification. Do not add runtime SDK checks merely to preserve newer-Android features.
> Before implementing, verify every Android API, manifest attribute, permission, support-library feature, and dependency against Android 4.4/API 19 compatibility. Preserve existing application functionality whenever it can be kept API19-safe; if a specific feature cannot be preserved for API19, explicitly report that loss and why.

## Build commands

```bash
# Clone (with submodules — required!)
git clone --recurse-submodules https://github.com/Gruven/amneziawg-android
git checkout legacy/api19

# Debug APK
./gradlew assembleDebug

# Release AAB (requires keystore)
./gradlew bundleRelease

# Release APK
./gradlew assembleRelease

# Unit tests
./gradlew test

# Tests for a specific module
./gradlew :tunnel:test
```

**macOS**: native code build requires `flock(1)` — install via `brew install discoteq/flock/flock`.

## Modules

- **`ui/`** — Android application (Kotlin). Activities, Fragments, ViewModels, resources. Package: `org.amnezia.awg`
- **`tunnel/`** — Tunnel library (Java). Configs, cryptography, VPN backends, JNI bindings to native code

## Architecture

**MVVM** with Android Data Binding. UI layer in Kotlin, tunnel logic in Java.

### UI module (`ui/src/main/java/org/amnezia/awg/`)
- `activity/` — Activities. `BaseActivity` — shared base class. `MainActivity`, `SettingsActivity`, `LogViewerActivity`, `TunnelCreatorActivity`, `TunnelToggleActivity`, `TaskerEditActivity`
- `fragment/` — Fragments. `TunnelListFragment`, `TunnelDetailFragment`, `TunnelEditorFragment`, `AppListDialogFragment`, `AddTunnelsSheet`, `ConfigNamingDialogFragment`
- `viewmodel/` — Proxy classes (`InterfaceProxy`, `PeerProxy`, `ConfigProxy`) for data binding
- `model/` — `TunnelManager`, `ObservableTunnel`, `ApplicationData`
- `preference/` — Custom preferences (`VersionPreference`, `ToolsInstallerPreference`, `ZipExporterPreference`, `KernelModuleEnablerPreference`)
- `widget/` — Custom views (`ToggleSwitch`, `KeyInputFilter`, `NameInputFilter`)
- `databinding/` — Observable collections and binding adapters
- `util/` — Helpers (`ErrorMessages`, `UserKnobs`, `AdminKnobs`, `ClipboardUtils`, `DownloadsFileSaver`, `QrCodeFromFileScanner`, `TunnelImporter`)
- `Application.kt` — App entry point, backend initialization
- `BootShutdownReceiver.kt` — Auto-start on boot
- `TaskerFireReceiver.kt` — Tasker plugin action receiver

**Note:** No Android TV/Leanback launcher path, no `QuickTileService`, and no AndroidX Biometric on this branch; these are not API19-only requirements.

### Tunnel module (`tunnel/src/main/java/org/amnezia/awg/`)
- `backend/` — `Backend` interface, `GoBackend` (primary, via JNI), `RootGoBackend` (root-based, no VPN API), `AwgQuickBackend` (alternative via root)
- `config/` — WireGuard/AmneziaWG config parsing (`Config`, `Interface`, `Peer`, `InetEndpoint`)
- `crypto/` — Curve25519, `Key`, `KeyPair`
- `util/` — `RootShell`, `ToolsInstaller`, `SharedLibraryLoader`

### Native code (`tunnel/tools/`)
- `libwg-go/` — Go WireGuard implementation (primary backend). JNI via `api-android.go` + `jni.c`. Go is downloaded automatically during build
- `tun-creator.c` — Helper binary executed as root to create TUN interface and pass fd via Unix socket (SCM_RIGHTS)
- `amneziawg-tools/` — C CLI tools implementation (git submodule)
- `elf-cleaner/` — Utility for .so compatibility with API < 21 (git submodule)
- `CMakeLists.txt` — NDK build configuration, produces `libwg-go.so`, `libwg.so`, `libwg-quick.so`, `libawg-tun-creator.so`

## Key build parameters

- `compileSdk`: 35, `minSdk`: 19, `targetSdk`: 19
- NDK: 25.2.9519653
- AGP: 9.1.0 (Kotlin is bundled with AGP, no separate Kotlin plugin)
- KAPT via `com.android.legacy-kapt` plugin
- Java: 17
- MultiDex enabled (`androidx.multidex`)
- Core library desugaring enabled (`desugar_jdk_libs`)
- ProGuard enabled in release (`ui/proguard-android-optimize.txt`)
- Version is set in `gradle.properties` (`versionName`, `versionCode`)

## Testing

Unit tests are in `tunnel/src/test/`. Config parsing tests (`ConfigTest.java`) and error handling tests (`BadConfigExceptionTest.java`). Test configs are in `tunnel/src/test/resources/`.

## CI/CD

GitHub Actions (`.github/workflows/`):
- `build.yml` — build workflow (manual dispatch)
- `tag.yml` — tagging workflow
- `release.yml` — release workflow
- `upload-assets.yml` — asset upload workflow

Release secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`.

## Git Submodules

The project uses submodules (`amneziawg-tools`, `elf-cleaner`). After cloning or switching branches: `git submodule update --init --recursive`.

## API compatibility notes

**This branch targets API 19 (Android 4.4 KitKat), not newer Android versions. Treat API 19 compatibility as a hard requirement for every change.** Key restrictions:
- No Android TV / Leanback launcher support (`LEANBACK_LAUNCHER` and related banner attrs are API 20/21+)
- No Quick Settings tiles (API 24+)
- No AndroidX Biometric / BiometricPrompt / Fingerprint feature path for this branch
- No `PowerManager.isIgnoringBatteryOptimizations()` (API 23+)
- No Doze mode / `dumpsys deviceidle` (API 23+)
- No `<queries>` package visibility block (API 30+)
- No `android:roundIcon` (API 25+) or `android:banner` (API 20/21+)
- No `android.permission.FOREGROUND_SERVICE` / `foregroundServiceType` additions for API19-only behavior
- `elf-cleaner` is required for .so compatibility with API < 21

When in doubt, prefer preserving the existing app behavior with an API19-safe implementation. If preservation is impossible because the feature is inherently newer-Android-only, prefer deletion or an API19-safe no-op over compatibility shims, and explicitly call out the removed/degraded functionality in the final report.
