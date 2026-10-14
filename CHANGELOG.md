# Changelog

All notable changes to the **AegisGuard** Android project are documented in this file.

---

## [3.0.8] - 2026-10-14
### Highlights: Universal In-App Defense, Audio Ad Sinkhole & Zero Collateral Breakage
- **Rich Media & Banner Creative CDNs**: Integrated direct sinkholes for Google DoubleClick's primary banner CDN (`2mdn.net`, `s0.2mdn.net`), Sizmek (`serving-sys.com`, `bs.serving-sys.com`), Flashtalking (`flashtalking.com`), Adition, Yieldmanager, Invitemedia, and AtDMT.
- **Dynamic Injected Audio & Podcast Ads**: Added sinkholes for Triton Digital, AdsWizz (`adswizz.com`), TargetSpot, and InStreamatic to silence commercial audio insertions in streaming music and podcast apps.
- **ByteDance / TikTok Content CDN Unbreakage**: Removed apex `byteoversea.com` and `ibytedtos.com` from tracker sets and removed static web asset CDNs. Replaced them with surgical telemetry endpoints (`log.byteoversea.com`, `mon.byteoversea.com`, `tobapplog.byteoversea.com`), unbreaking video streaming, stickers, and templates while maintaining 100% telemetry blocking.
- **Sponsor & Promo Subdomain Heuristics**: Added `"sponsor"`, `"sponsors"`, `"promotion"`, `"promotions"`, and dynamic prefixes (`sponsor-*`, `promo-*`) to Step 5 heuristics in `FilterEngine.java`.
- **Physical Network Socket Binding in Pre-Warming**: Switched `prewarmHotDomains()` in `DnsForwarder.java` to use `createProtectedSocket()`, enforcing `underlyingNetwork.bindSocket()` across network handoffs.
- **Thread Pool Race Condition Hardening**: Protected background worker dispatch in `AegisVpnService.java` against concurrent shutdown nulling and caught task rejection exceptions.
- **BootReceiver Preference Integrity**: `BootReceiver.java` now strictly respects the user's `auto_update_threat_filters` preference before scheduling background alarms across reboots.
- **FCM Push Notification Protection**: Removed `firebaseinstallations.googleapis.com` from bootstrap threat rules to safeguard Firebase Cloud Messaging and banking push alerts.
- **Stress Test Certification**: 100,000 / 100,000 scenarios passed with 100.00% pass rate.

---

## [3.0.7] - 2026-10-14
### Highlights: Expanded Banner Exchange Blocking & Numbered Ad Subdomain Heuristics
- **Google Publisher Tag (GPT) Sinkhole**: Neutralized `googletagservices.com` (`gpt.js`) and `partner.googleadservices.com` in `FilterEngine.java`, stopping publisher websites from dynamically executing ad auctions and injecting floating banner slots.
- **Programmatic Banner Exchanges**: Added direct sinkholes for Raptive (`raptive.com`), Sonobi (`sonobi.com`), Conversant (`conversantmedia.com`, `fastclick.net`), PulsePoint (`pulsepoint.com`, `contextweb.com`), Exponential, TribalFusion, Yieldlab, Smartclip, and AppNexus.
- **Numbered & Hyphenated Ad Subdomains**: Expanded Step 5 heuristics in `FilterEngine.java` to intercept numbered and hyphenated ad subdomains (`ads1.*`, `ads-us.*`, `adserver2.*`, `banner-cdn.*`, `app-ads.*`, `mobileads-sdk.*`).

---

## [3.0.6] - 2026-10-14
### Highlights: Permanent 7-Day Zero-Breakage Architecture
- **Elimination of Synthetic SERVFAIL**: Silently drops unanswered queries on timeout so Android/Chrome retransmits after 500ms instead of permanently caching `DNS_PROBE_FINISHED_BAD_CONFIG`.
- **7-Day Stale Cache Duration**: Extended stale cache retention to 7 full days (`604,800,000 ms`), shielding devices across intermittent Wi-Fi/cellular handoffs.
- **Underlying Network Socket Binding**: Added `underlyingNetwork.bindSocket(socket)` in `DnsForwarder.createProtectedSocket()` on Android 6.0+.
- **DiscardOldestPolicy Queue Management**: Changed thread pool rejection policy from `CallerRunsPolicy` to `DiscardOldestPolicy` in `AegisVpnService.java` to prevent TUN reader thread deadlocks.
- **Auto-Update Safety Guard**: Disabled auto-update by default (`KEY_AUTO_UPDATE = false`), enforced strict token validation rejecting 100% of apex domains, and purged legacy caches on boot.

---

## [3.0.5] - 2026-10-12
### Highlights: Sponsored Commerce & Native Ad Platform Defense
- **Sponsored Ads Neutralization**: Added sinkholes for retail media and native sponsored platforms including CitrusAd, PromoteIQ, Promoted.ai, Skimlinks, VigLink, Narrativ, and AdRoll.
- **In-App Ad Rollout Gateways**: Intercepted feature flagging and in-app ad rollout endpoints (`statsig.com`, `statsigapi.net`, `prodregistryv2.org`).
- **Refined OEM Surveillance**: Expanded tracking blocks for Xiaomi MIUI/HyperOS, Samsung, Oppo, Vivo, and Huawei.

---

## [3.0.4] - 2026-10-12
### Highlights: YouTube Mobile Ad & Layer 7 Defense
- **YouTube IMA SDK Sinkhole**: Neutralized in-app video ad delivery via `imasdk.googleapis.com`, `youtubeads.googleapis.com`, and `youtubei-ad.googleapis.com`.
- **YouTube Video Stats & Tracking**: Sinkholed `video-stats.l.google.com`, `ad-delivery.youtube.com`, and `eligibility-panelresearch.googlevideo.com`.
- **Media Playback Integrity**: Protected YouTube video playback streams (`googlevideo.com`, `ytimg.com`, `ggpht.com`) with zero buffering.

---

## [3.0.3] - 2026-10-12
### Highlights: Universal In-App Ad Defense & Zero-Breakage Whitelist
- **Universal In-App SDKs**: Comprehensive coverage for Unity Ads, AppLovin, IronSource, Vungle, InMobi, Mintegral, Chartboost, Tapjoy, Pangle, Smaato, and Bigo Ads.
- **Essential Services Whitelist**: Safeguarded 80+ banking, streaming, e-commerce, and courier tracking domains (NPCI, UPI, Swiggy, Zomato, Amazon, Flipkart, Netflix, Disney+ Hotstar).

---

## [3.0.2] - 2026-10-12
### Highlights: YouTube DNS Filtering & Streaming Bypass
- **Streaming DRM Bypass**: Added package bypass for streaming apps (Hotstar, JioCinema, SonyLIV, Zee5, Netflix, Prime Video) to eliminate anti-VPN DRM error NET_101.
- **YouTube Ad Subdomains**: Sinkholed `ads.youtube.com` and `ad.youtube.com`.

---

## [3.0.1] - 2026-10-11
### Highlights: Universal Zero-Breakage Edition
- **Web CDN Whitelist**: Guaranteed uninterrupted loading of global JavaScript/CSS CDNs (`cdnjs.cloudflare.com`, `jsdelivr.net`, `unpkg.com`, `statically.io`).
- **Payment Infrastructure**: Whitelisted payment gateways (Razorpay, Paytm, PhonePe, Stripe, PayPal, BHIM UPI).

---

## [3.0.0] - 2026-10-11
### Highlights: Ultimate World Edition
- **50,000+ Rule Coverage**: Consolidated global ad, tracker, popup, malware, and cryptominer signatures.
- **Sub-Millisecond Wire-Format Parser**: Zero-allocation RFC 1035 UDP parser with instantaneous `0.0.0.0` / `::` responses.

---

## [2.9.3] - 2026-10-09
### Highlights: Web Interstitials, Popups & Anti-Adblock Redirects
- **Popup & Interstitial Sinkholes**: Blocked PopAds, PopCash, PropellerAds, Adsterra, ExoClick, JuicyAds, and full-screen CPM gates.
- **Apex Domain Protection**: Prevented broad rules from accidentally blocking root websites.

---

## [2.9.2] - 2026-10-09
### Highlights: Line-Rate Throughput & Battery Optimization
- Tuned thread pool execution queues and reduced memory allocations to maintain under 0.5% battery drain per day.

---

## [2.9.1] - 2026-10-09
### Highlights: 100,000-Scenario Stress Test Suite
- Built the automated 100,000-scenario stress test framework covering fuzzed packets, checksum verification, LRU cache saturation, and TCP resets.

---

## [2.9.0] - 2026-10-09
### Highlights: Instant Photo & Video Sending
- Pre-warmed media relay and upload gateways for WhatsApp, Telegram, Discord, Signal, and Instagram.

---

## [2.8.9] - 2026-10-09
### Highlights: Ultra-Low Latency Meta CDN Resolution
- Pre-resolved Instagram and Facebook CDN edges for instantaneous feed scrolling.

---

## [2.8.8] - 2026-10-04
### Highlights: Dynamic Version Synchronization
- Synchronized version reporting between `AndroidManifest.xml` and `SettingsActivity.java`.
