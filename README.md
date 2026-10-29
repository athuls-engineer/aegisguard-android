# AegisGuard Android

**Local Android DNS sinkhole & ad blocker using VpnService — zero root required.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-teal.svg)](https://developer.android.com)
[![Release](https://img.shields.io/badge/Release-v3.1.4-emerald.svg)](https://github.com/athuls-engineer/aegisguard-android/releases)

---

## ⚡ Overview

**AegisGuard** is an open-source Android ad blocker and privacy filter built using Android's native `VpnService` API. 

It establishes a local loopback VPN interface to intercept outbound DNS requests (UDP port 53) directly on-device. Requests matching known advertising and tracker domains are sinkholed to `0.0.0.0`, while legitimate queries are forwarded to secure upstream DNS resolvers (such as Cloudflare or Google Public DNS).

Because filtering happens purely at the DNS level without proxying full HTTPS payload traffic, AegisGuard preserves device battery life, avoids breaking TLS certificates, and requires **no root access**.

---

## 🚀 Features

* **Zero Root Required**: Runs entirely in user space via Android `VpnService` without unlocking bootloaders or tripping Play Integrity.
* **On-Device DNS Parsing**: Native Java UDP packet parser extracting and validating DNS query headers and domain labels.
* **In-Memory Blocklist**: Fast in-memory hash set of known ad, telemetry, and malware domains with wildcard matching.
* **Upstream Resolver Racing**: Resolves non-blocked queries against secure upstream DNS providers.
* **Battery-Conscious Architecture**: Zero TLS re-encryption overhead; only port 53 DNS datagrams are inspected while general data packets route normally.

---

## 🛠️ Project Structure

```
aegisguard-android/
├── src/main/java/org/aegisguard/android/   # Core VPN service & packet engine
│   ├── AegisVpnService.java                # Android VpnService implementation
│   ├── DnsPacketParser.java                # Native UDP DNS datagram parser
│   └── FilterEngine.java                   # In-memory domain matcher
├── src/main/res/                           # Android UI layouts and assets
├── tools/                                  # Stress test & verification scripts
├── assets/branding/                        # App icons and design assets
└── build_apk.ps1                           # Portable build script (AAPT2 + D8)
```

---

## 🔨 Building from Source

### Prerequisites
* Java JDK 17
* Android SDK (build-tools & platforms)

### Autonomous Build
```powershell
# Build signed release APK
.\build_apk.ps1
```
The compiled release APK will be placed in `build/outputs/AegisGuard-release.apk`.

---

## 🤝 Authors & Contributors

* **Athul S** ([@athuls-engineer](https://github.com/athuls-engineer)) — Creator & Android Systems Engineer
* **Google Gemini** (AI Pair Programmer) — Co-developer assisting with DNS packet parsing logic, stress test suites, AAPT2/D8 build pipeline, and bug fixes.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.
