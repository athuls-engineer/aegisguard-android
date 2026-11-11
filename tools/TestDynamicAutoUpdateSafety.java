package org.aegisguard.android;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive verification suite for Autonomous Threat Feed Auto-Updates:
 * 1. Downloads & parses real live upstream feeds (AdGuardSDNSFilter, AdAway, CNAME Ads + HaGeZi Light failover).
 * 2. Audits 155+ essential services, banks, govt/edu portals, couriers, news, streaming, CDNs for 0% breakage.
 * 3. Adversarial feed-poisoning test (attempts to inject functional/apex/real websites -> must be rejected).
 * 4. Category isolation test (Ads vs Trackers vs Fingerprinting toggles + @@|| unbreak exceptions).
 */
public class TestDynamicAutoUpdateSafety {

    private static final List<String> REAL_WORLD_PROTECTED_WEBSITES = Arrays.asList(
        // Search, AI & Productivity
        "google.com", "www.google.com", "accounts.google.com", "mail.google.com", "drive.google.com",
        "maps.google.com", "play.google.com", "scholar.google.com", "translate.google.com",
        "duckduckgo.com", "www.bing.com", "search.yahoo.com", "en.wikipedia.org", "upload.wikimedia.org",
        "openai.com", "chatgpt.com", "claude.ai", "anthropic.com", "github.com", "raw.githubusercontent.com",
        "adguardteam.github.io", "gitlab.com", "adaway.org", "stackoverflow.com",

        // YouTube / Morphe / Video Players / Captchas
        "www.youtube.com", "m.youtube.com", "youtubei.googleapis.com", "s.youtube.com", "s2.youtube.com",
        "video-stats.l.google.com", "sponsor.ajay.app", "returnyoutubedislikeapi.com",
        "redirector.googlevideo.com", "jnn-pa.googleapis.com", "mtalk.google.com",
        "firebaseinstallations.googleapis.com", "fcmregistrations.googleapis.com",
        "ssl.p.jwpcdn.com", "a.hcaptcha.com", "hcaptcha.com", "challenges.cloudflare.com", "www.recaptcha.net",

        // Banking, UPI & Payments
        "sbi.co.in", "onlinesbi.sbi", "www.hdfcbank.com", "www.icicibank.com", "axisbank.com",
        "kotak.com", "pnbindia.in", "bankofbaroda.in", "canarabank.com", "federalbank.co.in",
        "npci.org.in", "bhimupi.org.in", "paytm.com", "phonepe.com", "razorpay.com",
        "stripe.com", "paypal.com", "cred.club", "zerodha.com", "groww.in",
        "visa.com", "mastercard.com", "chase.com",

        // Government & Education
        "gov.in", "www.gov.in", "www.uidai.gov.in", "incometax.gov.in", "www.irctc.co.in",
        "irctc.co.in", "parivahan.gov.in", "digilocker.gov.in", "epfindia.gov.in", "indiapost.gov.in",
        "nptel.ac.in", "mit.edu", "www.stanford.edu", "usa.gov", "www.gov.uk", "www.ox.ac.uk", "www.nhs.uk",

        // Parcel Logistics & Travel
        "track.delhivery.com", "track.bluedart.com", "track.fedex.com", "tracking.dtdc.in",
        "aftership.com", "dhl.com", "ups.com", "uber.com", "olacabs.com", "makemytrip.com",
        "booking.com", "airbnb.com", "redbus.in",

        // Messaging & Social
        "whatsapp.com", "web.whatsapp.com", "telegram.org", "web.telegram.org", "signal.org",
        "discord.com", "cdn.discordapp.com", "instagram.com", "www.facebook.com", "x.com",
        "www.reddit.com", "oauth.reddit.com", "www.linkedin.com", "zoom.us", "slack.com",

        // E-Commerce & Daily Apps
        "www.amazon.in", "www.amazon.com", "www.flipkart.com", "www.myntra.com", "ajio.com",
        "meesho.com", "www.swiggy.com", "www.zomato.com", "www.zepto.now", "blinkit.com",
        "www.ebay.com", "www.walmart.com", "bookmyshow.com", "nykaa.com",

        // Streaming & News
        "www.hotstar.com", "www.netflix.com", "open.spotify.com", "www.twitch.tv",
        "www.primevideo.com", "www.sonyliv.com", "www.zee5.com", "soundcloud.com", "cricbuzz.com",
        "ad.nl", "www.ad.nl", "thehindu.com", "www.thehindu.com", "timesofindia.indiatimes.com",
        "ndtv.com", "www.bbc.com", "www.bbc.co.uk", "www.nytimes.com", "www.theguardian.com", "www.reuters.com",

        // Cloud, CDNs & Shared Hosting
        "cloudfront.net", "d111111abcdef8.cloudfront.net", "storage.googleapis.com",
        "fonts.googleapis.com", "fonts.gstatic.com", "cdnjs.cloudflare.com",
        "cdn.jsdelivr.net", "unpkg.com", "vercel.app", "myblog.github.io", "mysite.netlify.app",
        "s3.amazonaws.com"
    );

    private static void fail(String msg) {
        System.err.println("FAIL: " + msg);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Testing Adversarial Feed Poisoning & Category Controls ===");
        testAdversarialAndCategoryIsolation();

        System.out.println("\n=== 2. Testing Live Multi-Feed Auto-Update (AdGuard + AdAway + CNAME) ===");
        DynamicFeedParser.ParsedThreatBundle liveBundle = DynamicFeedParser.fetchAllLiveFeeds();
        System.out.printf("Live Multi-Feed Summary: feeds=%d | ads=%d | trackers=%d | fingerprints=%d | exceptions=%d | total=%d\n",
            liveBundle.feedsSucceeded, liveBundle.ads.size(), liveBundle.trackers.size(),
            liveBundle.fingerprints.size(), liveBundle.exceptions.size(), liveBundle.totalBlockCount());

        if (liveBundle.feedsSucceeded < 2) fail("Expected >= 2 live feeds to succeed, got " + liveBundle.feedsSucceeded);
        if (liveBundle.ads.size() < 40000) fail("Expected > 40k dynamic ad rules, got " + liveBundle.ads.size());
        if (liveBundle.trackers.size() < 40000) fail("Expected > 40k dynamic tracker rules, got " + liveBundle.trackers.size());
        if (liveBundle.fingerprints.size() < 30) fail("Expected > 30 dynamic fingerprint rules, got " + liveBundle.fingerprints.size());
        if (liveBundle.exceptions.size() < 50) fail("Expected > 50 dynamic @@|| exceptions, got " + liveBundle.exceptions.size());

        FilterEngine.updateDynamicRuleSnapshots(
            liveBundle.ads, liveBundle.trackers, liveBundle.fingerprints, liveBundle.exceptions
        );

        System.out.println("\n=== 3. Zero-Breakage Audit on 155+ Real Websites (Live Feeds Loaded) ===");
        int broken = 0;
        for (String site : REAL_WORLD_PROTECTED_WEBSITES) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(site);
            if (res.isBlocked()) {
                System.err.println("  BROKEN WEBSITE: " + site + " -> " + res.category + " (" + res.rule + ")");
                broken++;
            }
        }
        System.out.printf("Real-world websites checked: %d | Broken: %d\n", REAL_WORLD_PROTECTED_WEBSITES.size(), broken);
        if (broken > 0) fail(broken + " real websites were blocked by live feeds!");

        // Sample check that live dynamic rules actually block sample entries from each category
        String sampleAd = liveBundle.ads.iterator().next();
        String sampleTrk = liveBundle.trackers.iterator().next();
        String sampleFp = liveBundle.fingerprints.iterator().next();
        FilterEngine.MatchResult rAd = FilterEngine.evaluate("sub." + sampleAd);
        FilterEngine.MatchResult rTrk = FilterEngine.evaluate("sub." + sampleTrk);
        FilterEngine.MatchResult rFp = FilterEngine.evaluate("sub." + sampleFp);
        System.out.println("Live sample ad check (" + sampleAd + "): " + rAd.decision + " [" + rAd.rule + "]");
        System.out.println("Live sample tracker check (" + sampleTrk + "): " + rTrk.decision + " [" + rTrk.rule + "]");
        System.out.println("Live sample fingerprint check (" + sampleFp + "): " + rFp.decision + " [" + rFp.rule + "]");
        if (!rAd.isBlocked() || !rTrk.isBlocked() || !rFp.isBlocked()) {
            fail("Sample live rules failed blocking check!");
        }

        System.out.println("\n=== 3.5 Re-Verifying Morphe/YouTube & Benchmark Suites WITH 183k Live Rules Active ===");
        TestYoutubeMorpheDomains.main(new String[0]);
        TestTurtleCute.main(new String[0]);
        TestD3Host.main(new String[0]);
        TestCanYouBlockIt.main(new String[0]);
        TestAdBlockTesterDomains.main(new String[0]);

        System.out.println("\n=== 4. Testing HaGeZi Multi LIGHT Failover Feed ===");
        DynamicFeedParser.ParsedThreatBundle failoverBundle = new DynamicFeedParser.ParsedThreatBundle();
        int failoverCount = DynamicFeedParser.downloadAndParseFeed(
            DynamicFeedParser.FEED_HAGEZI_LIGHT_FAILOVER,
            DynamicFeedParser.FeedMode.GENERAL_ZERO_BREAKAGE,
            failoverBundle
        );
        failoverBundle.finalizeCategories();
        System.out.printf("HaGeZi Light Failover: parsed=%d | ads=%d | trackers=%d | fingerprints=%d\n",
            failoverCount, failoverBundle.ads.size(), failoverBundle.trackers.size(), failoverBundle.fingerprints.size());
        if (failoverBundle.totalBlockCount() < 80000) {
            fail("HaGeZi Light failover rule count unexpectedly low: " + failoverBundle.totalBlockCount());
        }
        FilterEngine.updateDynamicRuleSnapshots(
            failoverBundle.ads, failoverBundle.trackers, failoverBundle.fingerprints, failoverBundle.exceptions
        );
        int failoverBroken = 0;
        for (String site : REAL_WORLD_PROTECTED_WEBSITES) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(site);
            if (res.isBlocked()) {
                System.err.println("  FAILOVER BROKEN: " + site + " -> " + res.rule);
                failoverBroken++;
            }
        }
        if (failoverBroken > 0) fail(failoverBroken + " websites broken under HaGeZi Light failover!");
        System.out.println("HaGeZi Light Failover zero-breakage check: 0/" + REAL_WORLD_PROTECTED_WEBSITES.size() + " broken");

        FilterEngine.clearDynamicRuleSnapshots();
        System.out.println("\nALL DYNAMIC AUTO-UPDATE & ZERO-BREAKAGE CHECKS PASSED 100%!");
    }

    private static void testAdversarialAndCategoryIsolation() throws Exception {
        String syntheticFeed = String.join("\n",
            "! Title: Adversarial + Multi-Section Test",
            "! Source name: AdGuard Mobile Ads filter",
            "||google.com^",
            "||www.hdfcbank.com^",
            "||api.amazon.in^",
            "||cdn.flipkart.com^",
            "||auth.mycollege.edu^",
            "||portal.city.gov^",
            "||pay.newstore.com^",
            "||track.newcourier.in^",
            "||login.startup.io^",
            "||ad.nl^",
            "||github.io^",
            "||cloudfront.net^",
            "||example.com/ads^", // URL path rule must NOT block whole domain
            "||brandnew-adserver-2027.com^",
            "||unbreak-me-dynamic.com^",
            "! Source name: EasyPrivacy Tracking Protection",
            "||evil-tracker-2027.io^",
            "! Source name: Fingerprinting & NoCoin Filter",
            "||device-probe-2027.net^",
            "@@||unbreak-me-dynamic.com^",
            "@@||www.some-site.com/ads^" // Path exception must not leak whole domain
        );

        DynamicFeedParser.ParsedThreatBundle bundle = new DynamicFeedParser.ParsedThreatBundle();
        DynamicFeedParser.parseFeedStream(
            new ByteArrayInputStream(syntheticFeed.getBytes(StandardCharsets.UTF_8)),
            DynamicFeedParser.FeedMode.ADGUARD_MULTI,
            bundle
        );
        bundle.finalizeCategories();

        String[] mustReject = {
            "google.com", "www.hdfcbank.com", "api.amazon.in", "cdn.flipkart.com",
            "auth.mycollege.edu", "portal.city.gov", "pay.newstore.com", "track.newcourier.in",
            "login.startup.io", "ad.nl", "github.io", "cloudfront.net", "example.com", "unbreak-me-dynamic.com"
        };
        for (String r : mustReject) {
            if (bundle.ads.contains(r) || bundle.trackers.contains(r) || bundle.fingerprints.contains(r)) {
                fail("Adversarial / protected domain leaked into block bundle: " + r);
            }
        }

        FilterEngine.updateDynamicRuleSnapshots(
            bundle.ads, bundle.trackers, bundle.fingerprints, bundle.exceptions
        );

        for (String r : mustReject) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(r);
            if (res.isBlocked()) {
                fail("Protected/exception domain was blocked: " + r + " -> " + res.rule);
            }
        }

        FilterEngine.MatchResult adRes = FilterEngine.evaluate("v1.brandnew-adserver-2027.com");
        if (adRes.decision != FilterEngine.Decision.BLOCK_AD) {
            fail("Expected BLOCK_AD for dynamic ad, got " + adRes.decision);
        }

        FilterEngine.MatchResult trkRes = FilterEngine.evaluate("v1.evil-tracker-2027.io");
        if (trkRes.decision != FilterEngine.Decision.BLOCK_TRACKER) {
            fail("Expected BLOCK_TRACKER for dynamic tracker, got " + trkRes.decision);
        }

        FilterEngine.MatchResult fpRes = FilterEngine.evaluate("sdk.device-probe-2027.net");
        if (fpRes.decision != FilterEngine.Decision.BLOCK_FINGERPRINT) {
            fail("Expected BLOCK_FINGERPRINT for dynamic fingerprint, got " + fpRes.decision);
        }

        // Toggle respect check
        FilterEngine.blockFingerprint = false;
        if (FilterEngine.evaluate("sdk.device-probe-2027.net").isBlocked()) {
            fail("Fingerprint toggle OFF did not allow dynamic fingerprint domain");
        }
        FilterEngine.blockFingerprint = true;

        FilterEngine.blockTrackers = false;
        if (FilterEngine.evaluate("v1.evil-tracker-2027.io").isBlocked()) {
            fail("Trackers toggle OFF did not allow dynamic tracker domain");
        }
        FilterEngine.blockTrackers = true;

        FilterEngine.clearDynamicRuleSnapshots();
        System.out.println("Adversarial + category isolation checks passed cleanly.");
    }
}
