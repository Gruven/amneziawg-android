# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Android client for AmneziaWG — a WireGuard fork with additional obfuscation capabilities. Multi-module Gradle project with native code in Go and C.

Build scripts use Kotlin DSL (`*.gradle.kts`). The project currently consists of two Gradle modules: `:ui` and `:tunnel`.

## Build commands

```bash
# Clone (with submodules — required!)
git clone --recurse-submodules https://github.com/Gruven/amneziawg-android

# Debug APK
./gradlew assembleDebug

# Release AAB
./gradlew bundleRelease

# Release APK
./gradlew assembleRelease

# Google Play-flavoured release build type
./gradlew assembleGoogleplay

# Unit tests
./gradlew test

# Tests for a specific module
./gradlew :tunnel:test
```

**macOS**: native code build requires `flock(1)` — install via `brew install discoteq/flock/flock`.

Release signing in CI/local release builds uses these environment variables or secrets:
`KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and optionally `ANDROID_KEY_PASSWORD`.

## Modules

- **`ui/`** — Android application module (Kotlin + Android resources). Namespace/application ID comes from `amneziawgPackageName` in `gradle.properties` and currently resolves to `org.amnezia.awg`
- **`tunnel/`** — Android library module (mostly Java) with config parsing, cryptography, VPN/root backends, JNI bindings, and native build integration

## Architecture

**MVVM** with Android Data Binding and ViewBinding. UI layer is primarily Kotlin; tunnel/core logic is primarily Java.

### UI module (`ui/src/main/java/org/amnezia/awg/`)
- `activity/` — Activities including `MainActivity`, `TvMainActivity`, `SettingsActivity`, `TunnelCreatorActivity`, `TunnelToggleActivity`, `TaskerEditActivity`, and `LogViewerActivity`
- `fragment/` — Fragments and sheets for tunnel list/details/editor flow (`TunnelListFragment`, `TunnelDetailFragment`, `TunnelEditorFragment`, `AddTunnelsSheet`, etc.)
- `viewmodel/` — Proxy classes (`InterfaceProxy`, `PeerProxy`, `ConfigProxy`) used by data binding
- `model/` — Tunnel state and list management (`ObservableTunnel`, `TunnelManager`, comparators)
- `configStore/` — Persistent tunnel config storage abstractions
- `databinding/`, `preference/`, `util/`, `widget/` — Binding adapters, custom preferences, utility classes, and reusable UI widgets
- `Application.kt` — App entry point, backend initialization
- `QuickTileService.kt` — Quick Settings tile
- `BootShutdownReceiver.kt` — Auto-start on boot
- `TaskerFireReceiver.kt` — Tasker plugin action receiver
- `activity/TaskerEditActivity.kt` — Tasker plugin configuration UI

### Tunnel module (`tunnel/src/main/java/org/amnezia/awg/`)
- `GoBackend.java` — JNI bridge class exposing native `libwg-go` entry points
- `backend/` — Backend abstraction and implementations: `GoBackend` (primary VPN backend), `RootGoBackend`, `AwgQuickBackend`, plus root helpers/services and tunnel statistics/status APIs
- `config/` — WireGuard/AmneziaWG config parsing (`Config`, `Interface`, `Peer`, `InetEndpoint`, `InetNetwork`, validation/errors)
- `crypto/` — Curve25519, `Key`, `KeyPair`, format validation
- `util/` — Shared library loading, root shell execution, tool installation, nullness annotations

### Native code (`tunnel/tools/`)
- `libwg-go/` — Go userspace implementation (primary backend). JNI via `api-android.go` + `jni.c`
- `tun-creator.c` — Helper binary executed as root to create TUN interface and pass fd via Unix socket (SCM_RIGHTS)
- `amneziawg-tools/` — C CLI tools implementation (git submodule)
- `elf-cleaner/` — Utility for .so compatibility with API < 21 (git submodule)
- `ndk-compat/` — Small NDK compatibility shim used by native build
- `CMakeLists.txt` — NDK build configuration, produces `libwg-go.so`, `libwg.so`, `libwg-quick.so`, `libawg-tun-creator.so`

## Key build parameters

- `compileSdk`: 35, `minSdk`: 21, `targetSdk`: 35
- Android Gradle Plugin: 9.1.0
- Gradle wrapper: 9.3.1
- NDK: 26.1.10909125
- Java: 17
- App build features: Data Binding, ViewBinding, BuildConfig, ABI splits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) with universal APK enabled
- Core library desugaring is enabled in `ui`
- Release builds enable minification and resource shrinking; ProGuard config is `ui/proguard-android-optimize.txt`
- Version and package identifiers are set in `gradle.properties` via `amneziawgVersionName`, `amneziawgVersionCode`, and `amneziawgPackageName`

## Testing

Unit tests are in `tunnel/src/test/`. Current coverage is focused on config parsing and error handling:
- `ConfigTest.java`
- `BadConfigExceptionTest.java`

Fixture configs live in `tunnel/src/test/resources/` (`working.conf`, malformed configs, missing sections/attributes, invalid keys/values, etc.).

## CI/CD

GitHub Actions workflows:
- `.github/workflows/build.yml` — reusable/manual Android build workflow for `debug_apk`, `release_apk`, or `release_aab`
- `.github/workflows/release.yml` — creates GitHub Releases for version tags and attaches signed release APK artifacts
- `.github/workflows/tag.yml` — creates and pushes a version tag, defaulting to `v${amneziawgVersionName}`
- `.github/workflows/upload-assets.yml` — rebuilds and uploads APK assets to an existing release if needed

Relevant release/build secrets:
- `CHECKOUT_TOKEN`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Git Submodules

The project uses submodules in `tunnel/tools/` (`amneziawg-tools`, `elf-cleaner`). After cloning or switching branches: `git submodule update --init --recursive`.
