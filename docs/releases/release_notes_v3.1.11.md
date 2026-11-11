# AegisGuard v3.1.11 Release Notes

## Overview
AegisGuard v3.1.11 enables autonomous threat feed auto-updates by default to continuously capture newly emerging **ads, trackers, and fingerprinting** domains every 5 hours (plus automatic boot and connectivity catch-up), while enforcing multi-layer zero-breakage safeguards so working websites stay unaffected.

---

## What's Fixed & Improved in v3.1.11

### 1. Autonomous Multi-Feed Auto-Updates (On by Default)
- **Enabled Out of the Box**: `auto_update_threat_filters` now defaults to `true` across `DynamicFilterManager`, `BootReceiver`, `FilterUpdateReceiver`, and `SettingsActivity` (including one-time upgrade initialization so existing users also receive automatic updates unless manually disabled).
- **Network Recovery & 5-Hour Sync**: Added `DynamicFilterManager.syncIfStale()` inside `AegisVpnService.networkCallback.onAvailable` (throttled with a 5-minute cooldown) so updates recover automatically after offline/doze windows alongside the 5-hour periodic alarm.
- **Instant Cache Flush**: Immediately calls `DnsForwarder.clearCacheGlobal()` after hot-swapping new threat feeds so cached DNS lookups reflect updated protections right away.

### 2. Categorized Feeds for Ads, Trackers & Fingerprinting
- **Curated Live Feed Bundle (`DynamicFeedParser`)**:
  - Merges `AdGuardSDNSFilter` (parsed by `! Source name:` section headers into Ads, Trackers, and Fingerprinting/Cryptominers plus `@@||...^` unbreak exceptions), `AdAway` Android hosts (with automatic mirror fallback), and `AdGuard CNAME Disguised Ads`.
  - Automatically fails over to **HaGeZi Multi LIGHT** (`gitlab.com/hagezi/mirror`) if the primary feed is unreachable.
- **Expanded Safe Capacity**: Raised dynamic feed capacity from 35,000 to `250,000` rules so full Tracker and Fingerprinting sections ingest without truncation while preserving category toggle isolation (`blockAds`, `blockTrackers`, `blockFingerprint`).
- **Lock-Free Atomic Updates**: Swaps immutable snapshot sets atomically in `FilterEngine` and persists categorized rules (`A|`, `T|`, `F|`, `E|`) to disk via atomic temp-file writes (`aegis_dynamic_threat_cache_v311.txt`).

### 3. Multi-Layer Zero-Breakage Website Protection
- **Ingress & Evaluation Guards**: Every dynamically ingested rule is vetted against `FilterEngine.isEssentialService()`, `isProtectedWebsite()`, `GLOBAL_PROTECTED_ROOTS`, `SHARED_HOSTING_SUFFIXES`, public/govt/edu/banking TLDs, logistics tracking (`track.*`), URL-path filter exclusion, functional subdomain prefixes (`www`, `api`, `cdn`, `auth`, `login`, `pay`, `checkout`, etc.), and live upstream `@@||...^` unbreak exceptions.
- **Subdomain Safety**: Dynamic suffix matching automatically stops before shared hosting platforms (`cloudfront.net`, `github.io`, `googleapis.com`, etc.) and protected root domains.

---

## Verification Summary
| Check / Benchmark | Result in v3.1.11 |
|---|---|
| **Live Multi-Feed Sync Audit** | **183,275 active rules** (`75,549` ads, `107,625` trackers, `101` fingerprinting, `183` unbreak exceptions) |
| **Failover Feed (`HaGeZi Multi LIGHT`)** | **153,195 rules** parsed with **0 / 156** real websites broken |
| **Zero-Breakage Audit (Live Rules Active)** | **0 / 156** real websites broken (Google, YouTube/Morphe, banking/UPI, govt/edu, couriers, e-commerce, streaming, news, CDNs) |
| **AegisGuard 100k Stress Test** | **100,000 / 100,000 passed (100.00%)** in 1.09s |
| **TurtleCute / D3Ward / CanYouBlockIt / AdBlock-Tester** | **126/126**, **131/131**, **26/26**, **13/13** blocked (verified both standalone & with live feeds loaded) |
| **SuperAdblock Probes** | **475 / 482 blocked (98.5%)** |
