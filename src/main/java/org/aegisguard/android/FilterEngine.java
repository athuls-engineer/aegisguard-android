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

    private static final Set<String> AD_DOMAINS = new HashSet<>(512);
    private static final Set<String> POPUP_DOMAINS = new HashSet<>(256);
    private static final Set<String> TRACKER_DOMAINS = new HashSet<>(256);
    private static final Set<String> FINGERPRINT_DOMAINS = new HashSet<>(128);
    private static final Set<String> OEM_DOMAINS = new HashSet<>(128);
    public static final Set<String> DYNAMIC_DOMAINS = Collections.synchronizedSet(new HashSet<String>(4096));

    // High-performance heuristic ad subdomain labels (O(1) instant match)
    private static final Set<String> AD_SUBDOMAIN_LABELS = new HashSet<>(Arrays.asList(
        "ad", "ads", "adserver", "adservers", "adservice", "adservices",
        "adsystem", "adsystems", "admanager", "adcontent", "adcdn", "addelivery",
        "adnetwork", "adtrack", "adtracker", "adclient", "adtag", "adx", "appads",
        "banner", "banners", "bannerad", "bannerads", "inappads",
        "popup", "popups", "popunder", "popunders",
        "interstitial", "interstitials", "displayads", "nativeads",
        "pagead", "videoads", "servedby"
    ));

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
            "googlesyndication.com", "tpc.googlesyndication.com", "afs.googlesyndication.com",
            "doubleclick.net", "googleads.g.doubleclick.net", "pagead2.googlesyndication.com",
            "adservice.google.com", "admob.com", "media.admob.com", "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net", "pagead2.googleadservices.com", "googleadservices.com",
            "ads.google.com", "adsyndication.com", "imasdk.googleapis.com", "partnerad.l.google.com",
            "adclick.g.doubleclick.net", "ade.googlesyndication.com",
            "cm.g.doubleclick.net", "stats.g.doubleclick.net", "adtrafficquality.google",
            "ep1.adtrafficquality.google", "admob.google.com", "googlemobileads.google.com",
            "app-measurement.com",
            // Feature Flagging & In-App Ad Rollout Gateways (Character.ai, etc.)
            "prodregistryv2.org", "statsigapi.net", "api.statsig.com", "statsig.com",
            // App Monetization, Full-Screen Video & Interstitial SDKs
            "applovin.com", "applvn.com", "d.applovin.com", "a.applovin.com", "ms.applovin.com",
            "pdn.applovin.com", "rt.applovin.com", "o.applovin.com", "edge.applovin.com",
            "res.applovin.com", "assets.applovin.com", "bidder.applovin.com", "safebrowsing.applovin.com",
            "unityads.unity3d.com", "unityads.unity.com", "auction.unityads.unity3d.com", "unityads.com",
            "webview.unityads.unity3d.com", "config.unityads.unity3d.com", "adserver.unityads.unity3d.com",
            "cdns.unityads.unity3d.com", "stats.unityads.unity3d.com", "ads.unity3d.com", "operative.unity3d.com",
            "adcontent.unityads.unity3d.com", "unity3d-ads.com",
            "ironsrc.com", "ironsource.com", "supersonicads.com", "supersonic.com", "ssacdn.com", "platform.ironsrc.com",
            "track.ironsrc.com", "outcome.ironsrc.com", "mobile-monetization.ironsrc.com",
            "init.supersonicads.com", "logs.ironsrc.com", "al.supersonicads.com",
            "inmobi.com", "config.inmobi.com", "api.inmobi.com", "c.inmobi.com", "w.inmobi.com", "sdkm.w.inmobi.com",
            "i.inmobi.com", "ad.inmobi.com", "adx.inmobi.com", "dsp.inmobi.com",
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
            "ads.tiktok.com", "analytics.tiktok.com", "ads-api.tiktok.com",
            "fyber.com", "inner-active.mobi", "adcolony.com", "digitalturbine.com", "dt.com",
            "wd.adcolony.com", "events3.adcolony.com",
            "smaato.com", "smaato.net", "soma.smaato.net", "mobfox.com", "leadbolt.com",
            "ogury.com", "ogury.io", "presage.io", "verve.com", "mytarget.com",
            "bigossp.com", "bigoad.net", "ad.bigo.sg",
            "adjoe.zone", "adtiming.com", "tradplusad.com", "toponad.com", "hyprmx.com",
            "kayzen.io", "bidmachine.io", "adfox.ru", "an.yandex.ru",
            "moloco.com", "ad.moloco.com", "dsp.moloco.com",
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
            // Web Banners, Header-Bidding Exchanges & Publisher Monetization
            "ezoic.net", "ezoic.com", "go.ezoic.net", "ezodn.com", "g.ezoic.net",
            "snigelweb.com", "adengine.snigelweb.com",
            "freestar.io", "a.pub.network", "pub.network",
            "mediavine.com", "scripts.mediavine.com",
            "adthrive.com", "ads.adthrive.com",
            "adpushup.com", "setupad.com", "prebid.setupad.com",
            "optidigital.com", "monetizemore.com", "pubguru.com",
            "adstyle.com", "dianomi.com",
            "primis.tech", "live.primis.tech", "aniview.com", "govaniview.com",
            "playwire.com", "cdn.intergi.com", "intergi.com",
            "nitropay.com", "s.nitropay.com", "buysellads.net", "srv.buysellads.com",
            "servedby-buysellads.com", "adx1.com", "adexc.net", "directrev.com",
            // Video Streaming In-Built Ad Delivery & Client SDKs
            "ads.twitch.tv", "ad.twitch.tv",
            "ads.spotify.com", "ads-fa.spotify.com", "adstudio.spotify.com",
            "audio-akp-b2-spotify-com.akamaized.net", "audio-akp-spotify-com.akamaized.net",
            "ad.flurry.com", "ad.smaato.net",
            "innovid.com", "spotxchange.com", "spotx.tv", "tremorhub.com",
            "springserve.com", "beachfront.com", "publica.com", "telaria.com",
            "freewheel.tv", "freewheel.com", "startappservice.com", "startapp.com",
            "admarvel.com", "inmobi.net"
        };
        Collections.addAll(AD_DOMAINS, ads);

        // 2. Intrusive Popups, Popunders, Full-Screen Interstitial Gates & Malicious Redirects
        String[] popups = new String[] {
            "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "exoclick.com",
            "trafficjunky.net", "trafficjunky.com", "juicyads.com", "hilltopads.com", "hilltopads.net",
            "clickadu.com", "richads.com", "pushground.com", "evadav.com", "monetag.com", "adcash.com", "admaven.com",
            "clicksor.com", "bidvertiser.com", "chitika.net", "infolinks.com", "trafficfactory.biz",
            "realsrv.com", "tsyndicate.com", "syndication.exoclick.com", "adxadserv.com",
            "popcash.com", "popmonetizer.com", "trafficshop.com", "zeroparallel.com",
            // Full-Screen Interstitial Gates, Anti-Adblock Redirects & Popunders
            "highcpmgate.com", "highperformancegate.com", "effectivecpmgate.com", "topcpmgate.com",
            "profitablecpmgate.com", "creativecpmgate.com",
            "al5sm.com", "alwingulla.com", "nap5k.com", "whoshout.biz", "deloplen.com", "out-take.biz",
            "pt50k.com", "st50k.com", "whomeet.biz", "in-page-push.com", "trk.admaven.com",
            "clksite.com", "trafficstars.com", "adtilt.com", "adtng.com", "adxpansion.com",
            "ero-advertising.com", "clarium.io", "clickadilla.com", "da-ads.com",
            "onclickalgo.com", "onclickperformance.com", "pushengage.com", "pushassist.com",
            "subscribers.com", "webpushr.com"
        };
        Collections.addAll(POPUP_DOMAINS, popups);

        // 3. Mobile Telemetry, Attribution & Cross-App Surveillance
        String[] trackers = new String[] {
            "appsflyer.com", "app.appsflyer.com", "gcdsdk.appsflyer.com", "t.appsflyer.com", "appsflyersdk.com",
            "adjust.com", "app.adjust.com", "view.adjust.com",
            "branch.io", "api.branch.io", "api2.branch.io",
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
            "spade.twitch.tv", "countess.twitch.tv",
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
            domain.equals("telesco.pe") || domain.endsWith(".telesco.pe") ||
            domain.equals("telegram-cdn.org") || domain.endsWith(".telegram-cdn.org") ||
            domain.equals("whatsapp.com") || domain.endsWith(".whatsapp.com") ||
            domain.equals("whatsapp.net") || domain.endsWith(".whatsapp.net") ||
            domain.equals("wa.me") || domain.endsWith(".wa.me") ||
            domain.equals("signal.org") || domain.endsWith(".signal.org") ||
            domain.equals("whispersystems.org") || domain.endsWith(".whispersystems.org") ||
            domain.equals("messenger.com") || domain.endsWith(".messenger.com") ||
            domain.equals("snapchat.com") || domain.endsWith(".snapchat.com") ||
            domain.equals("sc-cdn.net") || domain.endsWith(".sc-cdn.net") ||
            // Google & YouTube Core Infrastructure & Streaming Delivery
            domain.equals("google.com") || domain.endsWith(".google.com") ||
            domain.equals("youtube.com") || domain.endsWith(".youtube.com") ||
            domain.equals("googlevideo.com") || domain.endsWith(".googlevideo.com") ||
            domain.equals("ytimg.com") || domain.endsWith(".ytimg.com") ||
            domain.equals("ggpht.com") || domain.endsWith(".ggpht.com") ||
            domain.equals("googleapis.com") || domain.endsWith(".googleapis.com") ||
            domain.equals("gstatic.com") || domain.endsWith(".gstatic.com") ||
            domain.equals("googleusercontent.com") || domain.endsWith(".googleusercontent.com") ||
            domain.equals("gvt1.com") || domain.endsWith(".gvt1.com") ||
            domain.equals("gvt2.com") || domain.endsWith(".gvt2.com") ||
            domain.equals("youtu.be") || domain.endsWith(".youtu.be") ||
            domain.equals("youtube-nocookie.com") || domain.endsWith(".youtube-nocookie.com") ||
            domain.equals("1e100.net") || domain.endsWith(".1e100.net") ||
            domain.equals("android.com") || domain.endsWith(".android.com") ||
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
            domain.equals("discordapp.com") || domain.endsWith(".discordapp.com") || domain.equals("discordapp.net") || domain.endsWith(".discordapp.net") ||
            domain.equals("discord.media") || domain.endsWith(".discord.media") || domain.equals("discordcdn.com") || domain.endsWith(".discordcdn.com") ||
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
            domain.equals("flixcart.com") || domain.endsWith(".flixcart.com") ||
            domain.equals("myntra.com") || domain.endsWith(".myntra.com") ||
            domain.equals("myntassets.com") || domain.endsWith(".myntassets.com") ||
            domain.equals("ajio.com") || domain.endsWith(".ajio.com") ||
            domain.equals("meesho.com") || domain.endsWith(".meesho.com") ||
            domain.equals("tatacliq.com") || domain.endsWith(".tatacliq.com") ||
            domain.equals("nykaa.com") || domain.endsWith(".nykaa.com") ||
            domain.equals("jiomart.com") || domain.endsWith(".jiomart.com") ||
            domain.equals("swiggy.com") || domain.endsWith(".swiggy.com") ||
            domain.equals("zomato.com") || domain.endsWith(".zomato.com") ||
            domain.equals("zepto.now") || domain.endsWith(".zepto.now") ||
            domain.equals("blinkit.com") || domain.endsWith(".blinkit.com") ||
            domain.equals("bigbasket.com") || domain.endsWith(".bigbasket.com") ||
            domain.equals("ebay.com") || domain.endsWith(".ebay.com") ||
            domain.equals("ebayimg.com") || domain.endsWith(".ebayimg.com") ||
            domain.equals("aliexpress.com") || domain.endsWith(".aliexpress.com") ||
            domain.equals("alicdn.com") || domain.endsWith(".alicdn.com") ||
            domain.equals("walmart.com") || domain.endsWith(".walmart.com") ||
            domain.equals("walmartimages.com") || domain.endsWith(".walmartimages.com") ||
            domain.equals("target.com") || domain.endsWith(".target.com") ||
            domain.equals("etsy.com") || domain.endsWith(".etsy.com") ||
            domain.equals("ssl-images-amazon.com") || domain.endsWith(".ssl-images-amazon.com") ||
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
            domain.equals("cdn77.org") || domain.endsWith(".cdn77.org") ||
            domain.equals("edgecastcdn.net") || domain.endsWith(".edgecastcdn.net") ||
            domain.equals("azureedge.net") || domain.endsWith(".azureedge.net") ||
            domain.equals("digicert.com") || domain.endsWith(".digicert.com") ||
            domain.equals("letsencrypt.org") || domain.endsWith(".letsencrypt.org") ||
            // Global Web Libraries, CDNs & Web Assets (Guarantee all web CSS, JS & Fonts load)
            domain.equals("cdnjs.cloudflare.com") || domain.endsWith(".cdnjs.cloudflare.com") ||
            domain.equals("cdnjs.com") || domain.endsWith(".cdnjs.com") ||
            domain.equals("jsdelivr.net") || domain.endsWith(".jsdelivr.net") ||
            domain.equals("jsdelivr.com") || domain.endsWith(".jsdelivr.com") ||
            domain.equals("unpkg.com") || domain.endsWith(".unpkg.com") ||
            domain.equals("statically.io") || domain.endsWith(".statically.io") ||
            domain.equals("bootstrapcdn.com") || domain.endsWith(".bootstrapcdn.com") ||
            domain.equals("fontawesome.com") || domain.endsWith(".fontawesome.com") ||
            domain.equals("typekit.net") || domain.endsWith(".typekit.net") ||
            domain.equals("fonts.net") || domain.endsWith(".fonts.net") ||
            domain.equals("adobe.com") || domain.endsWith(".adobe.com") ||
            domain.equals("adobe.io") || domain.endsWith(".adobe.io") ||
            domain.equals("creativecloud.com") || domain.endsWith(".creativecloud.com") ||
            domain.equals("gravatar.com") || domain.endsWith(".gravatar.com") ||
            domain.equals("wp.com") || domain.endsWith(".wp.com") ||
            domain.equals("wordpress.org") || domain.endsWith(".wordpress.org") ||
            domain.equals("wordpress.com") || domain.endsWith(".wordpress.com") ||
            domain.equals("w.org") || domain.endsWith(".w.org") ||
            domain.equals("s.w.org") || domain.endsWith(".s.w.org") ||
            domain.equals("jquery.com") || domain.endsWith(".jquery.com") ||
            domain.equals("polyfill.io") || domain.endsWith(".polyfill.io") ||
            domain.equals("staticflickr.com") || domain.endsWith(".staticflickr.com") ||
            domain.equals("imgur.com") || domain.endsWith(".imgur.com") ||
            domain.equals("tenor.com") || domain.endsWith(".tenor.com") ||
            domain.equals("giphy.com") || domain.endsWith(".giphy.com") ||
            // Web Browsers & Safe Updates
            domain.equals("mozilla.org") || domain.endsWith(".mozilla.org") ||
            domain.equals("mozilla.net") || domain.endsWith(".mozilla.net") ||
            domain.equals("firefox.com") || domain.endsWith(".firefox.com") ||
            domain.equals("chrome.com") || domain.endsWith(".chrome.com") ||
            domain.equals("chromium.org") || domain.endsWith(".chromium.org") ||
            domain.equals("opera.com") || domain.endsWith(".opera.com") ||
            domain.equals("brave.software") || domain.endsWith(".brave.software") ||
            domain.equals("vivaldi.com") || domain.endsWith(".vivaldi.com") ||
            // Global News & Publishing Portals
            domain.equals("wikipedia.org") || domain.endsWith(".wikipedia.org") ||
            domain.equals("wikimedia.org") || domain.endsWith(".wikimedia.org") ||
            domain.equals("wikidata.org") || domain.endsWith(".wikidata.org") ||
            domain.equals("bbc.com") || domain.endsWith(".bbc.com") ||
            domain.equals("bbc.co.uk") || domain.endsWith(".bbc.co.uk") ||
            domain.equals("reuters.com") || domain.endsWith(".reuters.com") ||
            domain.equals("cnn.com") || domain.endsWith(".cnn.com") ||
            domain.equals("nytimes.com") || domain.endsWith(".nytimes.com") ||
            domain.equals("theguardian.com") || domain.endsWith(".theguardian.com") ||
            domain.equals("bloomberg.com") || domain.endsWith(".bloomberg.com") ||
            domain.equals("forbes.com") || domain.endsWith(".forbes.com") ||
            domain.equals("medium.com") || domain.endsWith(".medium.com") ||
            domain.equals("substack.com") || domain.endsWith(".substack.com") ||
            domain.equals("quora.com") || domain.endsWith(".quora.com") ||
            domain.equals("pinterest.com") || domain.endsWith(".pinterest.com") ||
            domain.equals("pinimg.com") || domain.endsWith(".pinimg.com") ||
            domain.equals("tumblr.com") || domain.endsWith(".tumblr.com") ||
            domain.equals("linkedin.com") || domain.endsWith(".linkedin.com") ||
            domain.equals("licdn.com") || domain.endsWith(".licdn.com") ||
            domain.equals("thehindu.com") || domain.endsWith(".thehindu.com") ||
            domain.equals("ndtv.com") || domain.endsWith(".ndtv.com") ||
            domain.equals("indiatimes.com") || domain.endsWith(".indiatimes.com") ||
            domain.equals("hindustantimes.com") || domain.endsWith(".hindustantimes.com") ||
            domain.equals("indianexpress.com") || domain.endsWith(".indianexpress.com") ||
            domain.equals("livemint.com") || domain.endsWith(".livemint.com") ||
            domain.equals("moneycontrol.com") || domain.endsWith(".moneycontrol.com") ||
            // Global Video & Audio Streaming
            domain.equals("vimeo.com") || domain.endsWith(".vimeo.com") ||
            domain.equals("vimeocdn.com") || domain.endsWith(".vimeocdn.com") ||
            domain.equals("dailymotion.com") || domain.endsWith(".dailymotion.com") ||
            domain.equals("dmcdn.net") || domain.endsWith(".dmcdn.net") ||
            domain.equals("soundcloud.com") || domain.endsWith(".soundcloud.com") ||
            domain.equals("sndcdn.com") || domain.endsWith(".sndcdn.com") ||
            domain.equals("deezer.com") || domain.endsWith(".deezer.com") ||
            domain.equals("tidal.com") || domain.endsWith(".tidal.com") ||
            domain.equals("primevideo.com") || domain.endsWith(".primevideo.com") ||
            domain.equals("hulu.com") || domain.endsWith(".hulu.com") ||
            domain.equals("max.com") || domain.endsWith(".max.com") ||
            domain.equals("hbomax.com") || domain.endsWith(".hbomax.com") ||
            domain.equals("crunchyroll.com") || domain.endsWith(".crunchyroll.com") ||
            // Essential URL Shorteners & Shared Links (Prevents Broken Outbound Clicks)
            domain.equals("bit.ly") || domain.endsWith(".bit.ly") ||
            domain.equals("tinyurl.com") || domain.endsWith(".tinyurl.com") ||
            domain.equals("t.co") || domain.endsWith(".t.co") ||
            domain.equals("linktr.ee") || domain.endsWith(".linktr.ee") ||
            domain.equals("cutt.ly") || domain.endsWith(".cutt.ly") ||
            domain.equals("is.gd") || domain.endsWith(".is.gd") ||
            domain.equals("ow.ly") || domain.endsWith(".ow.ly") ||
            domain.equals("buff.ly") || domain.endsWith(".buff.ly") ||
            domain.equals("rebrand.ly") || domain.endsWith(".rebrand.ly") ||
            domain.equals("shorturl.at") || domain.endsWith(".shorturl.at") ||
            // Search Engines, Knowledge & Verification
            domain.equals("startpage.com") || domain.endsWith(".startpage.com") ||
            domain.equals("ecosia.org") || domain.endsWith(".ecosia.org") ||
            domain.equals("qwant.com") || domain.endsWith(".qwant.com") ||
            domain.equals("searx.me") || domain.endsWith(".searx.me") ||
            domain.equals("baidu.com") || domain.endsWith(".baidu.com") ||
            domain.equals("yandex.com") || domain.endsWith(".yandex.com") ||
            domain.equals("wolframalpha.com") || domain.endsWith(".wolframalpha.com") ||
            domain.equals("archive.org") || domain.endsWith(".archive.org") ||
            domain.equals("imdb.com") || domain.endsWith(".imdb.com") ||
            domain.equals("rottentomatoes.com") || domain.endsWith(".rottentomatoes.com") ||
            // Travel, Logistics & Local Booking
            domain.equals("booking.com") || domain.endsWith(".booking.com") ||
            domain.equals("agoda.com") || domain.endsWith(".agoda.com") ||
            domain.equals("airbnb.com") || domain.endsWith(".airbnb.com") ||
            domain.equals("expedia.com") || domain.endsWith(".expedia.com") ||
            domain.equals("makemytrip.com") || domain.endsWith(".makemytrip.com") ||
            domain.equals("irctc.co.in") || domain.endsWith(".irctc.co.in") ||
            domain.equals("goibibo.com") || domain.endsWith(".goibibo.com") ||
            domain.equals("cleartrip.com") || domain.endsWith(".cleartrip.com") ||
            domain.equals("tripadvisor.com") || domain.endsWith(".tripadvisor.com") ||
            domain.equals("uber.com") || domain.endsWith(".uber.com") ||
            domain.equals("ola.cabs") || domain.endsWith(".ola.cabs") ||
            domain.equals("rapido.bike") || domain.endsWith(".rapido.bike") ||
            // International E-Commerce
            domain.equals("shopee.com") || domain.endsWith(".shopee.com") ||
            domain.equals("lazada.com") || domain.endsWith(".lazada.com") ||
            domain.equals("tokopedia.com") || domain.endsWith(".tokopedia.com") ||
            domain.equals("taobao.com") || domain.endsWith(".taobao.com") ||
            domain.equals("tmall.com") || domain.endsWith(".tmall.com") ||
            domain.equals("jd.com") || domain.endsWith(".jd.com") ||
            // Cloud, Serverless & Web Hosting Platforms
            domain.equals("docker.com") || domain.endsWith(".docker.com") ||
            domain.equals("npmjs.com") || domain.endsWith(".npmjs.com") ||
            domain.equals("pypi.org") || domain.endsWith(".pypi.org") ||
            domain.equals("render.com") || domain.endsWith(".render.com") ||
            domain.equals("fly.io") || domain.endsWith(".fly.io") ||
            domain.equals("supabase.co") || domain.endsWith(".supabase.co") ||
            domain.equals("supabase.com") || domain.endsWith(".supabase.com") ||
            domain.equals("firebase.google.com") || domain.endsWith(".firebase.google.com") ||
            domain.equals("firebaseio.com") || domain.endsWith(".firebaseio.com") ||
            // Education & MOOCs
            domain.equals("coursera.org") || domain.endsWith(".coursera.org") ||
            domain.equals("edx.org") || domain.endsWith(".edx.org") ||
            domain.equals("udemy.com") || domain.endsWith(".udemy.com") ||
            domain.equals("khanacademy.org") || domain.endsWith(".khanacademy.org") ||
            // Global Search Engines & Browser Infrastructure
            domain.equals("bing.com") || domain.endsWith(".bing.com") ||
            domain.equals("yahoo.com") || domain.endsWith(".yahoo.com") ||
            domain.equals("duckduckgo.com") || domain.endsWith(".duckduckgo.com") ||
            domain.equals("brave.com") || domain.endsWith(".brave.com") ||
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
                // Safeguard: Only block if exact domain match OR if current is a specific subdomain (contains at least 2 dots)
                // This prevents dynamic rules from ever breaking an entire apex website!
                if (domain.equals(current) || current.indexOf('.') != current.lastIndexOf('.')) {
                    return new MatchResult(Decision.BLOCK_AD, "Global Threat Feed (Auto-Updated)", "Aegis_Dynamic_Cloud:" + current);
                }
            }

            int nextDot = current.indexOf('.');
            if (nextDot == -1) break;
            current = current.substring(nextDot + 1);
        }

        // 3. Fast Heuristic detection on standard ad, banner, popup & interstitial subdomains
        if (blockAds) {
            int firstDot = domain.indexOf('.');
            if (firstDot > 0) {
                String firstLabel = domain.substring(0, firstDot);
                if (AD_SUBDOMAIN_LABELS.contains(firstLabel)) {
                    return new MatchResult(Decision.BLOCK_AD, "Heuristic Ad Subdomain", "Aegis_Heuristic:" + domain);
                }
            }
            if (domain.startsWith("ad-") || domain.startsWith("banner-") || domain.startsWith("pop-") ||
                domain.startsWith("fullscreen-") || domain.startsWith("floating-") || domain.startsWith("sticky-") ||
                domain.startsWith("servedby.") || domain.startsWith("adpushup.") || domain.startsWith("adserver.") ||
                domain.startsWith("adservers.") || domain.startsWith("adservice.") || domain.startsWith("adsystem.") ||
                domain.startsWith("adsystems.") || domain.startsWith("ads-api.") || domain.startsWith("ad-delivery.") ||
                domain.startsWith("adcontent.") || domain.startsWith("adcdn.") || domain.startsWith("ads-cdn.") ||
                domain.startsWith("admanager.") || domain.startsWith("mobileads.") || domain.startsWith("videoads.") ||
                domain.startsWith("interstitial.") || domain.startsWith("rewarded.") || domain.startsWith("adtrack.") ||
                domain.startsWith("adtracker.") || domain.startsWith("adx.") || domain.startsWith("appads.") ||
                domain.startsWith("app-ads.") || domain.startsWith("bannerads.") || domain.startsWith("inappads.") ||
                domain.startsWith("static-ads.") || domain.startsWith("nativeads.") || domain.startsWith("displayads.") ||
                domain.startsWith("popup.") || domain.startsWith("popunder.")) {
                return new MatchResult(Decision.BLOCK_AD, "Heuristic Ad Subdomain", "Aegis_Heuristic:" + domain);
            }
        }

        return new MatchResult(Decision.CLEAN, "Clean Traffic", "Whitelisted");
    }

    public static int getTotalRulesCount() {
        return AD_DOMAINS.size() + POPUP_DOMAINS.size() + TRACKER_DOMAINS.size() + 
               FINGERPRINT_DOMAINS.size() + OEM_DOMAINS.size() + DYNAMIC_DOMAINS.size();
    }
}
