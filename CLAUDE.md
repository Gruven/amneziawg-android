# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Android client for AmneziaWG — a WireGuard fork with additional obfuscation capabilities. Multi-module Gradle project with native code in Go and C.

**This is the `legacy/api19` branch** — a legacy build targeting Android 4.4 (API 19). For modern Android (5.0+), see the `master` branch.

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
- `activity/` — Activities. `BaseActivity` — shared base class. `MainActivity` for phones, `TvMainActivity` for Android TV (Leanback). `SettingsActivity`, `LogViewerActivity`, `TunnelCreatorActivity`, `TunnelToggleActivity`, `TaskerEditActivity`
- `fragment/` — Fragments. `TunnelListFragment`, `TunnelDetailFragment`, `TunnelEditorFragment`, `AppListDialogFragment`, `AddTunnelsSheet`, `ConfigNamingDialogFragment`
- `viewmodel/` — Proxy classes (`InterfaceProxy`, `PeerProxy`, `ConfigProxy`) for data binding
- `model/` — `TunnelManager`, `ObservableTunnel`, `ApplicationData`
- `preference/` — Custom preferences (`VersionPreference`, `ToolsInstallerPreference`, `ZipExporterPreference`, `KernelModuleEnablerPreference`)
- `widget/` — Custom views (`ToggleSwitch`, `TvCardView`, `KeyInputFilter`, `NameInputFilter`)
- `databinding/` — Observable collections and binding adapters
- `util/` — Helpers (`ErrorMessages`, `UserKnobs`, `AdminKnobs`, `BiometricAuthenticator`, `ClipboardUtils`, `DownloadsFileSaver`, `QrCodeFromFileScanner`, `TunnelImporter`)
- `Application.kt` — App entry point, backend initialization
- `BootShutdownReceiver.kt` — Auto-start on boot
- `TaskerFireReceiver.kt` — Tasker plugin action receiver

**Note:** No `QuickTileService` on this branch (Quick Settings tiles require API 24+).

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

This branch targets API 19 (Android 4.4 KitKat). Key restrictions:
- No Quick Settings tiles (API 24+)
- No `PowerManager.isIgnoringBatteryOptimizations()` (API 23+)
- No Doze mode / `dumpsys deviceidle` (API 23+)
- `elf-cleaner` is required for .so compatibility with API < 21
