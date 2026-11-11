package org.aegisguard.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Pure-Java Threat Feed Downloader, Categorizer & Zero-Breakage Sanitizer.
 * Separated from Android framework wrappers so both the app runtime and JVM
 * regression suites execute identical parsing, classification, and safety guards.
 */
public final class DynamicFeedParser {

    public static final int MAX_DYNAMIC_RULES = 250000;
    public static final int MAX_EXCEPTIONS = 15000;

    // Live verified endpoints (checked 2026-11)
    public static final String FEED_ADGUARD_SDNS =
        "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt";
    public static final String FEED_ADAWAY_PRIMARY =
        "https://raw.githubusercontent.com/AdAway/adaway.github.io/master/hosts.txt";
    public static final String FEED_ADAWAY_MIRROR =
        "https://adaway.org/hosts.txt";
    public static final String FEED_ADGUARD_CNAME_ADS =
        "https://raw.githubusercontent.com/AdguardTeam/cname-trackers/master/data/combined_disguised_ads_justdomains.txt";
    public static final String FEED_HAGEZI_LIGHT_FAILOVER =
        "https://gitlab.com/hagezi/mirror/-/raw/main/dns-blocklists/domains/light.txt";

    public enum FeedMode {
        ADGUARD_MULTI,         // Uses "! Source name:" section hints + @@|| exceptions
        MOBILE_MIXED,          // Curated mobile ads & trackers (AdAway)
        AD_ONLY,               // Dedicated ad rules (CNAME disguised ads)
        GENERAL_ZERO_BREAKAGE  // HaGeZi Multi LIGHT failover
    }

    private enum RuleCategory {
        AD,
        TRACKER,
        FINGERPRINT
    }

    public static final class ParsedThreatBundle {
        public final Set<String> ads = new HashSet<>(8192);
        public final Set<String> trackers = new HashSet<>(8192);
        public final Set<String> fingerprints = new HashSet<>(512);
        public final Set<String> exceptions = new HashSet<>(512);
        public int feedsSucceeded = 0;
        public String lastPrimarySummary = "";

        public int totalBlockCount() {
            return ads.size() + trackers.size() + fingerprints.size();
        }

        public void finalizeCategories() {
            // Unbreak exceptions strictly win over any dynamic block entry
            ads.removeAll(exceptions);
            trackers.removeAll(exceptions);
            fingerprints.removeAll(exceptions);

            // Enforce disjoint buckets with category priority: fingerprint > tracker > ad
            trackers.removeAll(fingerprints);
            ads.removeAll(fingerprints);
            ads.removeAll(trackers);
        }
    }

    private static final Set<String> FUNCTIONAL_SUBDOMAIN_PREFIXES = new HashSet<>(Arrays.asList(
        "www", "api", "apis", "cdn", "cdns", "asset", "assets", "static", "img", "imgs", "image", "images",
        "media", "content", "auth", "login", "sso", "signin", "signup", "checkout", "pay", "payment",
        "payments", "shop", "store", "app", "mobile", "portal", "cloud", "edge", "download", "downloads",
        "secure", "mail", "email", "help", "support", "docs", "gateway", "connect", "status", "service",
        "services", "my", "account", "accounts", "user", "users", "dashboard", "client", "clients",
        "node", "srv", "server", "prod", "main", "stream", "video", "audio", "voice", "rtc", "web",
        "track", "tracking", "tracker", "stats", "pixel", "order", "orders", "delivery"
    ));

    private static final Set<String> PROTECTED_EXACT_ROOT_LABELS = new HashSet<>(Arrays.asList(
        "bank", "banking", "payments", "upi", "govt", "government", "university", "college",
        "hospital", "webmail", "intranet", "localhost", "broadcasthost", "local"
    ));

    private DynamicFeedParser() {}

    /**
     * Fetches and merges live curated Feeds covering Ads, Trackers, Fingerprinting,
     * and upstream @@|| unbreak exceptions, falling back cleanly if a mirror is unreachable.
     */
    public static ParsedThreatBundle fetchAllLiveFeeds() {
        ParsedThreatBundle bundle = new ParsedThreatBundle();

        // 1. Primary multi-category feed: AdGuard SDNS Filter (Ads, Mobile, Trackers, Privacy, Fingerprinting/Miners + @@|| unbreaks)
        boolean primaryMultiOk = false;
        try {
            int added = downloadAndParseFeed(FEED_ADGUARD_SDNS, FeedMode.ADGUARD_MULTI, bundle);
            if (added > 0) {
                primaryMultiOk = true;
                bundle.feedsSucceeded++;
                bundle.lastPrimarySummary = "AdGuardDNS";
            }
        } catch (Throwable ignored) {}

        // Failover for primary feed: HaGeZi Multi LIGHT (zero-breakage mirror on GitLab)
        if (!primaryMultiOk) {
            try {
                int added = downloadAndParseFeed(FEED_HAGEZI_LIGHT_FAILOVER, FeedMode.GENERAL_ZERO_BREAKAGE, bundle);
                if (added > 0) {
                    bundle.feedsSucceeded++;
                    bundle.lastPrimarySummary = "HaGeZiLight";
                }
            } catch (Throwable ignored) {}
        }

        // 2. Android Mobile Ad/Tracker specialization: AdAway (GitHub Raw -> adaway.org fallback)
        boolean adawayOk = false;
        try {
            int added = downloadAndParseFeed(FEED_ADAWAY_PRIMARY, FeedMode.MOBILE_MIXED, bundle);
            if (added > 0) {
                adawayOk = true;
                bundle.feedsSucceeded++;
            }
        } catch (Throwable ignored) {}

        if (!adawayOk) {
            try {
                int added = downloadAndParseFeed(FEED_ADAWAY_MIRROR, FeedMode.MOBILE_MIXED, bundle);
                if (added > 0) {
                    bundle.feedsSucceeded++;
                }
            } catch (Throwable ignored) {}
        }

        // 3. First-Party CNAME Disguised Ads (catches cloaked third-party ad endpoints)
        try {
            int added = downloadAndParseFeed(FEED_ADGUARD_CNAME_ADS, FeedMode.AD_ONLY, bundle);
            if (added > 0) {
                bundle.feedsSucceeded++;
            }
        } catch (Throwable ignored) {}

        bundle.finalizeCategories();
        return bundle;
    }

    public static int downloadAndParseFeed(String urlString, FeedMode mode, ParsedThreatBundle bundle) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL currentUrl = new URL(urlString);
            for (int hop = 0; hop < 3; hop++) {
                conn = (HttpURLConnection) currentUrl.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AegisGuard-ThreatEngine/3.1");
                conn.setRequestProperty("Accept-Encoding", "gzip");
                conn.connect();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    conn.disconnect();
                    if (location == null || location.isEmpty()) {
                        throw new IOException("Redirect without Location from " + urlString);
                    }
                    currentUrl = new URL(currentUrl, location);
                    continue;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + code + " for " + urlString);
                }

                String encoding = conn.getHeaderField("Content-Encoding");
                in = conn.getInputStream();
                if (encoding != null && encoding.toLowerCase(Locale.US).contains("gzip")) {
                    in = new GZIPInputStream(in);
                }
                return parseFeedStream(in, mode, bundle);
            }
            throw new IOException("Too many redirects for " + urlString);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static int parseFeedStream(InputStream in, FeedMode mode, ParsedThreatBundle bundle) throws IOException {
        if (in == null || bundle == null) return 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 16384);
        String line;
        int addedCount = 0;
        RuleCategory currentSectionCategory = defaultCategoryForMode(mode);

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("!")) {
                if (mode == FeedMode.ADGUARD_MULTI) {
                    String lower = line.toLowerCase(Locale.US);
                    if (lower.startsWith("! source name:") || lower.startsWith("! title:")) {
                        currentSectionCategory = classifySourceHeader(lower);
                    }
                }
                continue;
            }

            if (line.startsWith("#") || line.startsWith("[")) {
                continue;
            }

            // Capture explicit Adblock domain unbreak exceptions: @@||example.com^ or @@||example.com^$important
            if (line.startsWith("@@||")) {
                String exceptionDomain = extractExceptionDomain(line);
                if (exceptionDomain != null && bundle.exceptions.size() < MAX_EXCEPTIONS) {
                    bundle.exceptions.add(exceptionDomain);
                }
                continue;
            }

            if (bundle.totalBlockCount() >= MAX_DYNAMIC_RULES) {
                continue; // Still continue reading exceptions if any remain, though rules cap reached
            }

            if (line.startsWith("@") || line.contains("##") || line.contains("#@#") ||
                line.contains("#?#") || line.contains("$badfilter") || line.contains("$dnsrewrite") ||
                line.contains("$denyallow") || line.contains("$third-party") || line.contains("$sitekey") ||
                line.contains("$client=") || line.contains("$ctag=")) {
                continue;
            }

            String domain = extractDomain(line);
            if (domain == null || !isSafeDynamicDomain(domain)) {
                continue;
            }

            RuleCategory cat = classifyDomain(domain, mode, currentSectionCategory);
            boolean isNew = false;
            if (cat == RuleCategory.FINGERPRINT) {
                if (!bundle.fingerprints.contains(domain)) {
                    bundle.fingerprints.add(domain);
                    isNew = true;
                }
            } else if (cat == RuleCategory.TRACKER) {
                if (!bundle.trackers.contains(domain) && !bundle.fingerprints.contains(domain)) {
                    bundle.trackers.add(domain);
                    isNew = true;
                }
            } else {
                if (!bundle.ads.contains(domain) && !bundle.trackers.contains(domain) && !bundle.fingerprints.contains(domain)) {
                    bundle.ads.add(domain);
                    isNew = true;
                }
            }
            if (isNew) {
                addedCount++;
            }
        }
        return addedCount;
    }

    private static RuleCategory defaultCategoryForMode(FeedMode mode) {
        if (mode == FeedMode.GENERAL_ZERO_BREAKAGE) {
            return RuleCategory.AD;
        }
        return RuleCategory.AD;
    }

    private static RuleCategory classifySourceHeader(String lowerHeader) {
        if (lowerHeader.contains("fingerprint") || lowerHeader.contains("cryptomin") ||
            lowerHeader.contains("no-coin") || lowerHeader.contains("coin")) {
            return RuleCategory.FINGERPRINT;
        }
        if (lowerHeader.contains("track") || lowerHeader.contains("privacy") ||
            lowerHeader.contains("telemetry") || lowerHeader.contains("analytics") ||
            lowerHeader.contains("spy") || lowerHeader.contains("first-party") ||
            lowerHeader.contains("firstparty")) {
            return RuleCategory.TRACKER;
        }
        return RuleCategory.AD;
    }

    private static RuleCategory classifyDomain(String domain, FeedMode mode, RuleCategory sectionCategory) {
        if (hasFingerprintToken(domain)) {
            return RuleCategory.FINGERPRINT;
        }
        if (mode == FeedMode.ADGUARD_MULTI) {
            if (sectionCategory != RuleCategory.AD) {
                return sectionCategory;
            }
            if (hasStrongTrackerToken(domain)) {
                return RuleCategory.TRACKER;
            }
            return RuleCategory.AD;
        }
        if (mode == FeedMode.AD_ONLY) {
            return RuleCategory.AD;
        }
        if (hasStrongTrackerToken(domain)) {
            return RuleCategory.TRACKER;
        }
        return RuleCategory.AD;
    }

    private static boolean hasFingerprintToken(String d) {
        return d.contains("fingerprint") || d.contains("fpjs") || d.startsWith("fp.") ||
               d.contains(".fp.") || d.contains("coinhive") || d.contains("cryptoloot") ||
               d.contains("coinimp") || d.contains("webmine") || d.contains("minero") ||
               d.contains("devicecheck") || d.contains("device-info") || d.contains("deviceinfo") ||
               d.contains("botd.") || d.contains("threatmetrix");
    }

    private static boolean hasStrongTrackerToken(String d) {
        return d.contains("telemetry") || d.contains("analytics") || d.contains("metrics") ||
               d.contains("beacon") || d.startsWith("segment.") || d.contains(".segment.") ||
               d.contains("mixpanel") || d.contains("amplitude") || d.contains("appsflyer") ||
               d.startsWith("adjust.") || d.contains(".adjust.") || d.contains("branch.io") ||
               d.contains("braze") || d.contains("bugsnag") ||
               d.startsWith("sentry.") || d.contains(".sentry.") || d.contains("crashlytics") ||
               d.contains("hotjar") || d.startsWith("clarity.") || d.contains(".clarity.") ||
               d.contains("mouseflow") || d.contains("fullstory") || d.contains("heapanalytics") ||
               d.startsWith("collect.") || d.contains(".collect.") ||
               d.startsWith("log.") || d.contains(".log.") ||
               d.startsWith("logs.") || d.contains(".logs.") ||
               d.startsWith("trk.") || d.contains(".trk.");
    }

    public static String extractExceptionDomain(String line) {
        if (line == null || !line.startsWith("@@||")) return null;
        if (line.contains("/") || line.contains("*") || line.contains("$badfilter") ||
            line.contains("$denyallow") || line.contains("$ctag")) {
            return null;
        }
        int end = line.indexOf('^', 4);
        if (end <= 4) return null;
        String candidate = line.substring(4, end).trim().toLowerCase(Locale.US);
        return isValidDomain(candidate) ? candidate : null;
    }

    /**
     * Extracts bare domain from hosts (`0.0.0.0 d`), Adblock (`||d^`), or justdomains (`d`).
     * Never strips URL paths into whole-domain blocks.
     */
    public static String extractDomain(String line) {
        if (line == null || line.isEmpty()) return null;
        if (line.contains("/") || line.contains("*") || line.contains("?") || line.contains("=")) {
            return null;
        }

        if (line.startsWith("0.0.0.0") || line.startsWith("127.0.0.1") || line.startsWith("::1") || line.startsWith(":: ")) {
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                String host = parts[1].trim().toLowerCase(Locale.US);
                int hashIdx = host.indexOf('#');
                if (hashIdx > 0) host = host.substring(0, hashIdx);
                return isValidDomain(host) ? host : null;
            }
            return null;
        }

        if (line.startsWith("||")) {
            int end = line.indexOf('^', 2);
            if (end > 2) {
                String modifierPart = line.substring(end + 1);
                if (!modifierPart.isEmpty() && !modifierPart.startsWith("$important")) {
                    return null;
                }
                String host = line.substring(2, end).trim().toLowerCase(Locale.US);
                return isValidDomain(host) ? host : null;
            }
            return null;
        }

        if (!line.contains(" ") && !line.contains(":") && !line.contains("$") && !line.contains("#")) {
            String host = line.toLowerCase(Locale.US);
            return isValidDomain(host) ? host : null;
        }

        return null;
    }

    public static boolean isValidDomain(String d) {
        if (d == null || d.length() < 4 || d.length() > 253) return false;
        int firstDot = d.indexOf('.');
        if (firstDot <= 0 || firstDot == d.length() - 1) return false;
        if (d.startsWith(".") || d.endsWith(".") || d.startsWith("-") || d.endsWith("-")) return false;
        if (d.contains("..") || d.contains(".-") || d.contains("-.")) return false;
        if (d.equals("localhost") || d.equals("broadcasthost") || d.equals("local") ||
            d.equals("localhost.localdomain") || d.equals("ip6-localhost") || d.equals("ip6-loopback")) {
            return false;
        }
        boolean hasAlpha = false;
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c >= 'a' && c <= 'z') {
                hasAlpha = true;
            } else if (!((c >= '0' && c <= '9') || c == '.' || c == '-')) {
                return false;
            }
        }
        if (!hasAlpha) return false;

        int lastDot = d.lastIndexOf('.');
        if (d.length() - lastDot - 1 < 2) return false;
        return true;
    }

    /**
     * Multi-layer Zero-Breakage Ingestion Validator:
     * Ensures newly updated threat feeds never sinkhole real websites, essential APIs,
     * educational/government infrastructure, logistics tracking, or functional hosts.
     */
    public static boolean isSafeDynamicDomain(String domain) {
        if (!isValidDomain(domain)) return false;
        String d = domain.toLowerCase(Locale.US);

        if (FilterEngine.isEssentialService(d) ||
            FilterEngine.isProtectedWebsite(d) ||
            FilterEngine.isGlobalProtectedRoot(d) ||
            FilterEngine.isSharedHostingSuffix(d)) {
            return false;
        }

        // Never ingest parcel logistics or postal tracking subdomains
        if (d.contains(".track.") || d.startsWith("track.") || d.startsWith("tracking.") || d.startsWith("tracker.")) {
            return false;
        }

        // Never ingest Dutch/European news (ad.nl) or corporate Active Directory (.ad.)
        if (d.equals("ad.nl") || d.endsWith(".ad.nl") || d.equals("ad.si") || d.endsWith(".ad.si") || d.contains(".ad.")) {
            return false;
        }

        // Reject public, government, educational, military, health & banking TLDs
        if (isSensitivePublicDomain(d)) {
            return false;
        }

        // Reject functional website labels as the leftmost subdomain (www.*, api.*, cdn.*, auth.*, login.*, pay.*, etc.)
        int firstDot = d.indexOf('.');
        String firstLabel = d.substring(0, firstDot);
        if (FUNCTIONAL_SUBDOMAIN_PREFIXES.contains(firstLabel) || PROTECTED_EXACT_ROOT_LABELS.contains(firstLabel)) {
            return false;
        }

        return true;
    }

    private static boolean isSensitivePublicDomain(String d) {
        if (d.endsWith(".edu") || d.endsWith(".gov") || d.endsWith(".mil") ||
            d.endsWith(".bank") || d.endsWith(".int") || d.endsWith(".museum") ||
            d.endsWith(".gov.in") || d.endsWith(".nic.in") || d.endsWith(".ac.in") || d.endsWith(".edu.in") ||
            d.endsWith(".res.in") || d.endsWith(".ernet.in") ||
            d.endsWith(".gov.uk") || d.endsWith(".ac.uk") || d.endsWith(".nhs.uk") ||
            d.endsWith(".police.uk") || d.endsWith(".mod.uk") ||
            d.endsWith(".gov.au") || d.endsWith(".edu.au") ||
            d.endsWith(".gov.ca") || d.endsWith(".gc.ca") ||
            d.endsWith(".go.jp") || d.endsWith(".go.id") ||
            d.endsWith(".gov.sg") || d.endsWith(".gov.my") || d.endsWith(".gov.za") ||
            d.endsWith(".gov.br") || d.endsWith(".gob.mx")) {
            return true;
        }
        // Generic *.gov.xx / *.edu.xx / *.ac.xx country-code public sectors
        int lastDot = d.lastIndexOf('.');
        if (lastDot > 0 && d.length() - lastDot - 1 == 2) {
            int prevDot = d.lastIndexOf('.', lastDot - 1);
            if (prevDot >= 0) {
                String secondLevel = d.substring(prevDot + 1, lastDot);
                if ("gov".equals(secondLevel) || "edu".equals(secondLevel) || "ac".equals(secondLevel) || "mil".equals(secondLevel)) {
                    return true;
                }
            }
        }
        return false;
    }
}
