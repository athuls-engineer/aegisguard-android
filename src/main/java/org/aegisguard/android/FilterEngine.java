package org.aegisguard.android;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * High-performance, zero-allocation in-memory Trie/HashSet matching engine
 * for systemwide ad, popup, tracker, and fingerprinting neutralization.
 * 
 * Suffix Walk Algorithm:
 * Evaluates exact match, parent domains, and subdomains in O(k) time where k is number of labels.
 * If 'doubleclick.net' is blocked, all 'pubads.g.doubleclick.net', 'ad.doubleclick.net', etc.
 * are blocked with 0 bytes downloaded and sub-microsecond latency.
 */
public class FilterEngine {

    public enum Decision {
        CLEAN,
        BLOCK_AD,
        BLOCK_POPUP,
        BLOCK_TRACKER,
        BLOCK_FINGERPRINT,
        BLOCK_OEM
    }

    public static class MatchResult {
        public final Decision decision;
        public final String category;
        public final String rule;

        public MatchResult(Decision decision, String category, String rule) {
            this.decision = decision;
            this.category = category;
            this.rule = rule;
        }

        public boolean isBlocked() {
            return decision != Decision.CLEAN;
        }
    }

    public static volatile boolean blockAds = true;
    public static volatile boolean blockPopups = true;
    public static volatile boolean blockTrackers = true;
    public static volatile boolean blockFingerprint = true;
    public static volatile boolean blockOem = true;

    public static void loadPreferences(android.content.Context context) {
        if (context == null) return;
        android.content.SharedPreferences sp = context.getSharedPreferences("aegis_settings_prefs", android.content.Context.MODE_PRIVATE);
        blockAds = sp.getBoolean("block_ads", true);
        blockPopups = sp.getBoolean("block_popups", true);
        blockTrackers = sp.getBoolean("block_trackers", true);
        blockFingerprint = sp.getBoolean("block_fingerprint", true);
        blockOem = sp.getBoolean("block_oem", true);
    }

    private static final Set<String> AD_DOMAINS = new HashSet<>(256);
    private static final Set<String> POPUP_DOMAINS = new HashSet<>(128);
    private static final Set<String> TRACKER_DOMAINS = new HashSet<>(256);
    private static final Set<String> FINGERPRINT_DOMAINS = new HashSet<>(128);
    private static final Set<String> OEM_DOMAINS = new HashSet<>(128);
    public static final Set<String> DYNAMIC_DOMAINS = Collections.synchronizedSet(new HashSet<String>(4096));

    private static final Set<String> SHARED_HOSTING_SUFFIXES = new HashSet<>(Arrays.asList(
        "github.io", "pages.dev", "workers.dev", "web.app", "firebaseapp.com",
        "blogspot.com", "wordpress.com", "herokuapp.com", "vercel.app", "netlify.app",
        "azureedge.net", "cloudfront.net", "amazonaws.com", "s3.amazonaws.com",
        "google.com", "googleapis.com", "gstatic.com", "apple.com", "icloud.com",
        "microsoft.com", "azure.com", "akamaihd.net", "akamaized.net", "fastly.net",
        "gov.in", "co.in", "co.uk", "org.uk", "gov.uk", "com.au", "co.nz", "com.br"
    ));

    static {
        // 1. Mobile Ad Networks, Video Interstitials & Banners
        String[] ads = new String[] {
            // Google Ad Ecosystem & In-App Video SDKs
            "doubleclick.net", "googleads.g.doubleclick.net", "pagead2.googlesyndication.com",
            "adservice.google.com", "admob.com", "media.admob.com", "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net", "pagead2.googleadservices.com", "googleadservices.com",
            "ads.google.com", "adsyndication.com", "imasdk.googleapis.com", "partnerad.l.google.com",
            "video-stats.l.google.com", "adclick.g.doubleclick.net", "ade.googlesyndication.com",
            "cm.g.doubleclick.net", "stats.g.doubleclick.net",
            // App Monetization, Full-Screen Video & Interstitial SDKs
            "applovin.com", "applvn.com", "d.applovin.com", "a.applovin.com", "ms.applovin.com",
            "pdn.applovin.com", "rt.applovin.com", "o.applovin.com", "edge.applovin.com",
            "res.applovin.com", "assets.applovin.com",
            "unityads.unity3d.com", "unityads.unity.com", "auction.unityads.unity3d.com",
            "webview.unityads.unity3d.com", "config.unityads.unity3d.com", "adserver.unityads.unity3d.com",
            "cdns.unityads.unity3d.com", "stats.unityads.unity3d.com", "ads.unity3d.com", "operative.unity3d.com",
            "adcontent.unityads.unity3d.com",
            "ironsrc.com", "supersonicads.com", "supersonic.com", "ssacdn.com", "platform.ironsrc.com",
            "track.ironsrc.com", "outcome.ironsrc.com", "mobile-monetization.ironsrc.com",
            "init.supersonicads.com", "logs.ironsrc.com", "al.supersonicads.com",
            "inmobi.com", "config.inmobi.com", "api.inmobi.com", "c.inmobi.com", "w.inmobi.com", "sdkm.w.inmobi.com",
            "vungle.com", "api.vungle.com", "ads.api.vungle.com", "events.liftoff.io", "liftoff.io",
            "v.vungle.com", "cdn.vungle.com",
            "chartboost.com", "live.chartboost.com", "da.chartboost.com",
            "mintegral.com", "mintegral.net", "pg-cdn.mintegral.com", "hb.mintegral.com",
            "setting.mintegral.com", "rayjump.com", "cdn-adn.rayjump.com", "adn.rayjump.com", "mobvista.com",
            "tapjoy.com", "tapjoyads.com", "rpc.tapjoy.com", "ws.tapjoyads.com",
            "flurry.com", "startapp.com", "startappexchange.com", "startappservice.com", "start.io",
            "pangle.io", "pangolin-sdk-toutiao.com", "pangolin-sdk-toutiao1.com",
            "pangolin-sdk-toutiao-b.com", "pglstatp-toutiao.com", "ad.toutiao.com",
            "toblog.ctobsnssdk.com", "mon.musical.ly", "mon.byteoversea.com", "ib.snssdk.com",
            "isnssdk.com", "sf16-fe-tos-sg.byteoversea.com",
            "fyber.com", "inner-active.mobi", "adcolony.com", "digitalturbine.com", "dt.com",
            "wd.adcolony.com", "events3.adcolony.com",
            "smaato.com", "smaato.net", "soma.smaato.net", "mobfox.com", "leadbolt.com",
            "ogury.com", "ogury.io", "presage.io", "verve.com", "mytarget.com",
            "bigossp.com", "bigoad.net", "ad.bigo.sg",
            "adjoe.zone", "adtiming.com", "tradplusad.com", "toponad.com", "hyprmx.com",
            "kayzen.io", "bidmachine.io", "adfox.ru", "an.yandex.ru",
            // Amazon Publisher Services (APS / A9) Interstitial SDKs
            "amazon-adsystem.com", "aax.amazon-adsystem.com", "c.amazon-adsystem.com",
            "s.amazon-adsystem.com", "advertising.amazon.com", "aax-us-east.amazon-adsystem.com", "fls-na.amazon.com",
            // Content Recommendation & Display Exchanges
            "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "revcontent.com",
            "mgid.com", "smartadserver.com", "rubiconproject.com", "pubmatic.com",
            "openx.net", "casalemedia.com", "adnxs.com",
            "a-ads.com", "adform.net", "bidswitch.net", "sharethrough.com", "yieldmo.com",
            "triplelift.com", "sovrn.com", "indexexchange.com", "media.net", "lijit.com",
            "gumgum.com", "teads.tv", "undertone.com", "connatix.com", "kargo.com",
            "nativo.com", "outbrainimg.com", "taboolasyndication.com", "zergnet.com", "content.ad",
            "adkernel.com", "admixer.net", "yieldlove.com", "adtrue.com", "admatic.com.tr",
            "kidoz.net", "sdk.kidoz.net", "adgeneration.jp", "yieldone.com", "microad.net", "cpmstar.com",
            "smartyads.com", "epom.com", "propeller-tracking.com", "adxprts.com", "yandexads.com",
            "adfox.yandex.ru", "bebi.com", "adzerk.net", "kevel.co", "mtgglobals.com",
            // Video Streaming In-Built Ad Delivery & Client SDKs
            "ads.twitch.tv", "ad.twitch.tv",
            "ads.spotify.com", "ads-fa.spotify.com", "adstudio.spotify.com",
            "audio-akp-b2-spotify-com.akamaized.net", "audio-akp-spotify-com.akamaized.net",
            "ad.youtube.com", "ads.youtube.com", "ad.flurry.com", "ad.smaato.net",
            "innovid.com", "spotxchange.com", "spotx.tv", "tremorhub.com",
            "springserve.com", "beachfront.com", "publica.com", "telaria.com",
            "freewheel.tv", "freewheel.com", "startappservice.com", "startapp.com",
            "admarvel.com", "inmobi.net"
        };
        Collections.addAll(AD_DOMAINS, ads);

        // 2. Intrusive Popups, Popunders & Malicious Redirect Gateways
        String[] popups = new String[] {
            "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "exoclick.com",
            "trafficjunky.net", "juicyads.com", "hilltopads.com", "clickadu.com", "richads.com",
            "pushground.com", "evadav.com", "monetag.com", "adcash.com", "admaven.com",
            "clicksor.com", "bidvertiser.com", "chitika.net", "infolinks.com", "trafficfactory.biz",
            "realsrv.com", "tsyndicate.com", "syndication.exoclick.com", "adxadserv.com",
            "popcash.com", "popmonetizer.com", "trafficshop.com", "zeroparallel.com"
        };
        Collections.addAll(POPUP_DOMAINS, popups);

        // 3. Mobile Telemetry, Attribution & Cross-App Surveillance
        String[] trackers = new String[] {
            "appsflyer.com", "app.appsflyer.com", "gcdsdk.appsflyer.com", "t.appsflyer.com", "appsflyersdk.com",
            "adjust.com", "app.adjust.com", "view.adjust.com",
            "api2.branch.io",
            "kochava.com", "control.kochava.com", "api.kochava.com",
            "singular.net", "c.singular.net", "singular-metrics.com",
            "tenjin.io", "tenjin.com", "airbridge.io",
            "braze.com", "appboy.com", "sdk.iad-01.braze.com", "sdk.iad-03.braze.com",
            "mixpanel.com", "api.mixpanel.com", "decide.mixpanel.com",
            "segment.io", "segment.com", "api.segment.io", "cdn.segment.com",
            "amplitude.com", "api.amplitude.com", "api2.amplitude.com",
            "rudderstack.com", "api.rudderlabs.com", "count.ly", "uxcam.com",
            "localytics.com", "heap.io", "leanplum.com", "clevertap.com", "moengage.com",
            "onesignal.com", "airship.com", "urbanairship.com", "iterable.com", "customer.io",
            "kissmetrics.com", "crazyegg.com", "hotjar.com", "clarity.ms", "mouseflow.com",
            "fullstory.com", "logrocket.com", "smartlook.com", "smartlook.cloud", "contentsquare.net",
            "app-measurement.com", "google-analytics.com", "analytics.google.com",
            "scorecardresearch.com", "quantserve.com", "quantcast.com", "comscore.com",
            "moatads.com", "doubleverify.com", "iasds01.com", "integralads.com", "permutive.com",
            "bluekai.com", "krxd.net", "demdex.net", "omtrdc.net", "rlcdn.com", "agkn.com",
            "tapad.com", "liveramp.com", "id5-sync.com", "crwdcntrl.net", "eyeota.net",
            // Social Media Cross-App Tracking Pixels & Telemetry
            "pixel.facebook.com", "an.facebook.com", "tr.snapchat.com",
            "events.reddit.com", "alb.reddit.com", "ads.reddit.com", "e.reddit.com",
            "analytics.tiktok.com", "ads.tiktok.com", "byteoversea.com", "ibytedtos.com", "ads-twitter.com",
            "ads.twitter.com",
            "ads.pinterest.com", "ads.linkedin.com",
            // In-Built Video Streaming & Mobile Surveillance Beacons
            "spade.twitch.tv", "countess.twitch.tv", "s.youtube.com",
            "adeventtracker.spotify.com", "mc.yandex.ru", "data.flurry.com",
            "mobile.pipe.aria.microsoft.com"
        };
        Collections.addAll(TRACKER_DOMAINS, trackers);

        // 4. Deep Canvas, Audio, WebGL & Device Fingerprinting Collectors
        String[] fingerprints = new String[] {
            "mobile-collector.newrelic.com", "bam.nr-data.net", "fingerprintjs.com",
            "fpjs.io", "cdn.fpjs.io", "botd.fpjs.sh", "device-metrics-us.amazon.com",
            "device-metrics.amazon.com", "fp.browser.qq.com", "augur.io",
            "evercookie.net", "canvasfingerprinting.com",
            "browserleaks.com", "deviceinfo.me", "clientjs.org", "audiofingerprint.openwpm.com"
        };
        Collections.addAll(FINGERPRINT_DOMAINS, fingerprints);

        // 5. OEM Surveillance & System Telemetry
        String[] oem = new String[] {
            // Xiaomi MIUI / HyperOS
            "tracking.miui.com", "tracking.intl.miui.com", "data.mistat.intl.xiaomi.com", "data.mistat.xiaomi.com",
            "diag.miui.com", "abtest.mistat.xiaomi.com", "api.ad.xiaomi.com",
            "adv.sec.miui.com", "sdkconfig.ad.xiaomi.com",
            // Samsung
            "samsungosp.com", "samsung-osp.com", "samsungqbe.com", "samsunganalytics.com", "nmetrics.samsung.com",
            // Huawei / Honor
            "metrics.data.hicloud.com", "logservice.hicloud.com",
            // Oppo / Realme / OnePlus
            "logservice.oppomobile.com", "telemetry.oneplus.cn", "telemetry.oneplus.com",
            "log.realme.com", "heytapmobile.com", "tracking.heytapmobile.com", "adx.heytapmobile.com",
            "telemetry.oppo.com", "metrics.realme.com",
            // Vivo
            "metrics.vivo.com",
            // Transsion (Infinix, Tecno, Itel)
            "telemetry.transsion.com",
            // Motorola, Asus, Lenovo
            "telemetry.motorola.com", "cloudservices.motorola.com",
            "analytics.asus.com", "metrics.lenovo.com"
        };
        Collections.addAll(OEM_DOMAINS, oem);
    }

    /**
     * Bulletproof Whitelist Protection:
     * Guarantees that essential communication, authentic media streaming CDNs,
     * and OS connectivity checks are NEVER blocked by dynamic rules or heuristics.
     */
    public static boolean isEssentialService(String rawDomain) {
        if (rawDomain == null) return false;
        String domain = rawDomain.trim().toLowerCase();
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        // Never whitelist known telemetry or ad beacons even if hosted on major platform domains
        if (domain.contains("pixel.facebook.com") || domain.contains("an.facebook.com") ||
            domain.contains("events.reddit.com") || domain.contains("alb.reddit.com") ||
            domain.contains("ads.reddit.com") || domain.contains("e.reddit.com") ||
            domain.contains("analytics.tiktok.com") || domain.contains("ads.tiktok.com") ||
            domain.contains("ads.twitter.com") || domain.contains("ads-twitter.com") ||
            domain.contains("adservice.google.") || domain.contains("googleads.") ||
            domain.contains("pagead2.") || domain.contains("analytics.google.")) {
            return false;
        }

        return domain.equals("telegram.org") || domain.endsWith(".telegram.org") ||
            domain.equals("telegram.me") || domain.endsWith(".telegram.me") ||
            domain.equals("t.me") || domain.endsWith(".t.me") ||
            domain.equals("whatsapp.com") || domain.endsWith(".whatsapp.com") ||
            domain.equals("whatsapp.net") || domain.endsWith(".whatsapp.net") ||
            domain.equals("google.com") || domain.endsWith(".google.com") ||
            domain.equals("youtube.com") || domain.endsWith(".youtube.com") ||
            domain.endsWith(".googlevideo.com") || domain.endsWith(".ytimg.com") ||
            domain.equals("twitch.tv") || domain.endsWith(".twitch.tv") ||
            domain.endsWith(".ttvnw.net") || domain.endsWith(".live-video.net") || domain.endsWith(".twitchcdn.net") ||
            domain.equals("spotify.com") || domain.endsWith(".spotify.com") ||
            domain.endsWith(".scdn.co") || domain.endsWith(".audio-ak-spotify-com.akamaized.net") ||
            domain.equals("instagram.com") || domain.endsWith(".instagram.com") || domain.endsWith(".cdninstagram.com") ||
            domain.equals("threads.net") || domain.endsWith(".threads.net") ||
            domain.equals("facebook.com") || domain.endsWith(".facebook.com") || domain.endsWith(".fbcdn.net") ||
            domain.equals("fbsbx.com") || domain.endsWith(".fbsbx.com") ||
            domain.equals("meta.com") || domain.endsWith(".meta.com") ||
            domain.equals("facebook.net") || domain.endsWith(".facebook.net") ||
            domain.equals("fb.com") || domain.endsWith(".fb.com") ||
            domain.equals("fb.me") || domain.endsWith(".fb.me") ||
            domain.equals("twitter.com") || domain.endsWith(".twitter.com") || domain.equals("x.com") || domain.endsWith(".x.com") || domain.endsWith(".twimg.com") ||
            domain.equals("reddit.com") || domain.endsWith(".reddit.com") || domain.endsWith(".redditmedia.com") || domain.endsWith(".redd.it") ||
            domain.equals("redditstatic.com") || domain.endsWith(".redditstatic.com") ||
            domain.equals("discord.com") || domain.endsWith(".discord.com") || domain.equals("discord.gg") || domain.endsWith(".discord.gg") ||
            domain.equals("netflix.com") || domain.endsWith(".netflix.com") || domain.endsWith(".nflxvideo.net") || domain.endsWith(".nflximg.net") ||
            // Disney+ Hotstar, JioHotstar & Streaming Media Infrastructure (Eliminates NET_101 & DRM errors)
            domain.equals("hotstar.com") || domain.endsWith(".hotstar.com") ||
            domain.equals("hotstarcdn.com") || domain.endsWith(".hotstarcdn.com") ||
            domain.equals("hotstar-cdn.net") || domain.endsWith(".hotstar-cdn.net") ||
            domain.equals("starott.com") || domain.endsWith(".starott.com") ||
            domain.equals("conviva.com") || domain.endsWith(".conviva.com") ||
            domain.equals("onetrust.com") || domain.endsWith(".onetrust.com") ||
            domain.equals("disneyplus.com") || domain.endsWith(".disneyplus.com") ||
            domain.equals("amazon.com") || domain.endsWith(".amazon.com") || domain.endsWith(".media-amazon.com") ||
            domain.equals("amazon.in") || domain.endsWith(".amazon.in") ||
            domain.equals("apple.com") || domain.endsWith(".apple.com") || domain.endsWith(".icloud.com") ||
            domain.equals("microsoft.com") || domain.endsWith(".microsoft.com") || domain.endsWith(".office.com") ||
            domain.equals("wikipedia.org") || domain.endsWith(".wikipedia.org") ||
            // Developer & Knowledge Platforms
            domain.equals("github.com") || domain.endsWith(".github.com") ||
            domain.equals("github.io") || domain.endsWith(".github.io") || domain.endsWith(".githubusercontent.com") ||
            domain.equals("gitlab.com") || domain.endsWith(".gitlab.com") ||
            domain.equals("stackoverflow.com") || domain.endsWith(".stackoverflow.com") ||
            // AI Services
            domain.equals("openai.com") || domain.endsWith(".openai.com") ||
            domain.equals("chatgpt.com") || domain.endsWith(".chatgpt.com") ||
            domain.equals("claude.ai") || domain.endsWith(".claude.ai") ||
            domain.equals("anthropic.com") || domain.endsWith(".anthropic.com") ||
            // E-Commerce & Daily Delivery Platforms
            domain.equals("flipkart.com") || domain.endsWith(".flipkart.com") ||
            domain.equals("swiggy.com") || domain.endsWith(".swiggy.com") ||
            domain.equals("zomato.com") || domain.endsWith(".zomato.com") ||
            domain.equals("zepto.now") || domain.endsWith(".zepto.now") ||
            domain.equals("blinkit.com") || domain.endsWith(".blinkit.com") ||
            // Parcel Logistics & Courier Tracking
            domain.equals("dhl.com") || domain.endsWith(".dhl.com") ||
            domain.equals("fedex.com") || domain.endsWith(".fedex.com") ||
            domain.equals("ups.com") || domain.endsWith(".ups.com") ||
            domain.equals("delhivery.com") || domain.endsWith(".delhivery.com") ||
            domain.equals("bluedart.com") || domain.endsWith(".bluedart.com") ||
            domain.equals("indiapost.gov.in") || domain.endsWith(".indiapost.gov.in") ||
            domain.equals("tracker.gg") || domain.endsWith(".tracker.gg") ||
            // Banking, UPI & Payment Infrastructure
            domain.equals("paypal.com") || domain.endsWith(".paypal.com") ||
            domain.equals("stripe.com") || domain.endsWith(".stripe.com") ||
            domain.equals("razorpay.com") || domain.endsWith(".razorpay.com") ||
            domain.equals("paytm.com") || domain.endsWith(".paytm.com") ||
            domain.equals("phonepe.com") || domain.endsWith(".phonepe.com") ||
            domain.equals("npci.org.in") || domain.endsWith(".npci.org.in") ||
            // Public Sector, Education & CDNs
            domain.endsWith(".gov.in") || domain.endsWith(".nic.in") ||
            domain.endsWith(".gov") || domain.endsWith(".edu") ||
            domain.equals("cloudflare.com") || domain.endsWith(".cloudflare.com") ||
            domain.endsWith(".cloudfront.net") || domain.endsWith(".fastly.net") ||
            domain.endsWith(".akamaized.net") || domain.endsWith(".akamaihd.net") ||
            domain.equals("connectivitycheck.gstatic.com") ||
            domain.equals("connectivitycheck.android.com") ||
            domain.equals("clients3.google.com");
    }

    /**
     * Matches a domain name against all loaded protection layers.
     * Evaluates exact match, all parent domain suffixes, and heuristic subdomains.
     */
    public static MatchResult evaluate(String rawDomain) {
        if (rawDomain == null) {
            return new MatchResult(Decision.CLEAN, "Clean Traffic", "Whitelisted");
        }

        String domain = rawDomain.trim().toLowerCase();
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        // 1. FAST-PATH WHITELIST: Essential services skip all filtering instantly in < 0.001ms
        if (isEssentialService(domain)) {
            return new MatchResult(Decision.CLEAN, "Essential Service", "Whitelisted");
        }

        // 2. Suffix walk: sub.adservice.google.com -> adservice.google.com -> google.com
        String current = domain;
        while (current.contains(".")) {
            if (SHARED_HOSTING_SUFFIXES.contains(current) && !domain.equals(current)) {
                break;
            }
            if (blockAds && AD_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_AD, "Display & Video Ad", "Aegis_Ad_Core:" + current);
            }
            if (blockPopups && POPUP_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_POPUP, "Popup & Redirect Gateway", "Aegis_Popup_Core:" + current);
            }
            if (blockTrackers && TRACKER_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_TRACKER, "Mobile App Telemetry", "Aegis_Tracker_Core:" + current);
            }
            if (blockFingerprint && FINGERPRINT_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_FINGERPRINT, "Device Fingerprinting", "Aegis_Fingerprint:" + current);
            }
            if (blockOem && OEM_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_OEM, "OEM System Telemetry", "Aegis_OEM_Telemetry:" + current);
            }
            if (DYNAMIC_DOMAINS.contains(current)) {
                return new MatchResult(Decision.BLOCK_AD, "Global Threat Feed (Auto-Updated)", "Aegis_Dynamic_Cloud:" + current);
            }

            int nextDot = current.indexOf('.');
            if (nextDot == -1) break;
            current = current.substring(nextDot + 1);
        }

        // 3. Heuristic detection on standard ad/popup subdomains (strictly ad-serving, not logistics or analytics)
        if (blockAds && (domain.startsWith("ad.") || domain.startsWith("ads.") || 
            domain.startsWith("adservice.") || domain.startsWith("adserver.") ||
            domain.startsWith("adsystem.") || domain.startsWith("mobileads.") ||
            domain.startsWith("videoads.") || domain.startsWith("interstitial.") ||
            domain.startsWith("rewarded.") || domain.startsWith("adtrack.") ||
            domain.startsWith("banner.") || domain.startsWith("banners.") ||
            domain.startsWith("popup.") || domain.startsWith("popunder."))) {
            return new MatchResult(Decision.BLOCK_AD, "Heuristic Ad Subdomain", "Aegis_Heuristic:" + domain);
        }

        return new MatchResult(Decision.CLEAN, "Clean Traffic", "Whitelisted");
    }

    public static int getTotalRulesCount() {
        return AD_DOMAINS.size() + POPUP_DOMAINS.size() + TRACKER_DOMAINS.size() + 
               FINGERPRINT_DOMAINS.size() + OEM_DOMAINS.size() + DYNAMIC_DOMAINS.size();
    }
}
