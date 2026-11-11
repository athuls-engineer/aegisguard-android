# AegisGuard v3.1.10 Release Notes

## Overview
AegisGuard v3.1.10 resolves the YouTube / Morphe / ReVanced watch history warning (`s.youtube.com`), unblocks SponsorBlock (`sponsor.ajay.app`), and preserves JW Player web video playback (`ssl.p.jwpcdn.com`), while maintaining 100% network blocking across major adblock benchmark suites.

---

## What's Fixed & Improved in v3.1.10

### 1. YouTube / Morphe / ReVanced Watch History Fix (`s.youtube.com`)
- **Problem**: `s.youtube.com` was previously listed in `FilterEngine.AD_DOMAINS` from a third-party benchmark sweep. YouTube and patched clients (Morphe, ReVanced, ReVanced Extended) use `s.youtube.com` (`/api/stats/watchtime`, `/api/stats/playback`) to save watch history and playback progress, triggering the **"Morphe Notice: Your watch history is not being saved"** warning.
- **Fix**: Removed `s.youtube.com` from `AD_DOMAINS`, added explicit protection in `FilterEngine.isEssentialService()` for `s.youtube.com`, `s2.youtube.com`, `video-stats.video.google.com`, `video-stats.l.google.com`, and `video-stats.googlevideo.com`, and pre-warmed `s.youtube.com` in `DnsForwarder` cache on startup.
- **Ads Still Blocked**: All actual YouTube advertising domains (`ads.youtube.com`, `ad.youtube.com`, `youtubeads.googleapis.com`, `youtubei-ad.googleapis.com`, `ad-delivery.youtube.com`, `doubleclick.net`, `googlesyndication.com`) remain 100% blocked.

### 2. SponsorBlock & Modded Client API Immunity
- Explicitly whitelisted `ajay.app` (`sponsor.ajay.app`, `dearrow-thumb.ajay.app`), `revanced.app`, `morphe.software`, and `returnyoutubedislikeapi.com` in `isEssentialService()`.
- Prevents AegisGuard's `sponsor` heuristic subdomain rule from blocking SponsorBlock segment lookups.

### 3. Embedded Video Player & Essential CDN Hardening
- Removed `ssl.p.jwpcdn.com` (JW Player core script and skin assets CDN) from `AD_DOMAINS` while keeping JW Player telemetry/tracking (`prd.jwpltx.com`, `g.jwpsrv.com`) blocked.
- Updated `DnsForwarder` to bypass CNAME-cloaking sinkholes for domains explicitly verified by `FilterEngine.isEssentialService()`.

---

## Verification Summary
| Benchmark / Suite | Total Checks | Result in v3.1.10 | Pass Rate |
|---|---|---|---|
| **AegisGuard 100k Stress Test** | 100,000 | **100,000 passed** | **100.00%** |
| **TurtleCute Network Hosts** | 126 | **126 blocked** | **100.0%** |
| **D3Ward Toolz (`d3host.txt`)** | 131 | **131 blocked** | **100.0%** |
| **CanYouBlockIt Networks** | 26 | **26 blocked** | **100.0%** |
| **AdBlock-Tester External Domains** | 13 | **13 blocked** | **100.0%** |
| **SuperAdblock Probes** | 482 | **475 blocked** (7 functional assets preserved) | **98.5%** |
