# Android GUI for [AmneziaWG](https://amnezia.org/learn-more/31_amneziawg)

> **Note:** This is a fork of the original [amneziawg-android](https://github.com/amnezia-vpn/amneziawg-android) with additional features listed below. For the upstream version, please visit the [original repository](https://github.com/amnezia-vpn/amneziawg-android).

## Additional features

- **Root mode (no VPN API)** — Tunnel backend that uses root access to create TUN interfaces and configure routing via `iptables`/`ip route`, completely bypassing the Android VPN API. No VPN icon in the status bar, no VPN permission dialogs. All device traffic is routed through the tunnel.
- **Android 5.1+ support** — Minimum SDK lowered to 22 (Android 5.1 Lollipop).
- **macOS build fix** — Automatic detection of `shasum` vs `sha256sum` for Go tarball checksum verification during native build.

## Building

```
$ git clone --recurse-submodules https://github.com/amnezia-vpn/amneziawg-android
$ cd amneziawg-android
$ ./gradlew assembleRelease
```

macOS users may need [flock(1)](https://github.com/discoteq/flock).
