package org.aegisguard.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dynamic Threat Intelligence Engine (v3.1.11).
 *
 * 1. Auto-Enabled by Default: Autonomous 5-hour background sync + boot & network-recovery sync.
 * 2. Multi-Feed Categorized Ingestion: Pulls and merges fresh Ads, Trackers, Fingerprinting,
 *    and upstream @@|| unbreak exceptions via DynamicFeedParser with HaGeZi Light failover.
 * 3. Atomic Lock-Free Hot-Swap & Temp-File Persistence: Never blocks VPN packet forwarding
 *    and flushes stale DNS cache entries right after updating rules.
 * 4. Multi-Layer Zero-Breakage Safety: Sanitizes every rule at ingestion and evaluation time.
 */
public class DynamicFilterManager {

    private static final String TAG = "DynamicFilterManager";
    public static final String PREFS_NAME = "aegis_settings_prefs";
    public static final String KEY_AUTO_UPDATE = "auto_update_threat_filters";
    public static final String KEY_LAST_UPDATE = "last_threat_update_time";
    public static final String KEY_DYNAMIC_COUNT = "dynamic_rules_count";
    private static final String KEY_V311_INITIALIZED = "auto_update_v311_initialized";
    public static final boolean DEFAULT_AUTO_UPDATE = true;

    // 5 Hours = 5 * 60 * 60 * 1000 milliseconds
    public static final long UPDATE_INTERVAL_MS = 5 * 60 * 60 * 1000L;
    private static final long RETRY_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final String CACHE_FILE_NAME = "aegis_dynamic_threat_cache_v311.txt";

    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean sIsUpdating = new AtomicBoolean(false);
    private static volatile long sLastSyncAttemptMs = 0L;

    public interface UpdateCallback {
        void onUpdateStarted();
        void onUpdateCompleted(boolean success, int newRulesCount, String message);
    }

    public static boolean isGlobalProtectedRoot(String domain) {
        return FilterEngine.isGlobalProtectedRoot(domain);
    }

    public static boolean isSafeDynamicDomain(String domain) {
        return DynamicFeedParser.isSafeDynamicDomain(domain);
    }

    public static boolean isAutoUpdateEnabled(Context context) {
        if (context == null) return DEFAULT_AUTO_UPDATE;
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureV311DefaultMigration(sp);
        return sp.getBoolean(KEY_AUTO_UPDATE, DEFAULT_AUTO_UPDATE);
    }

    private static void ensureV311DefaultMigration(SharedPreferences sp) {
        if (!sp.getBoolean(KEY_V311_INITIALIZED, false)) {
            sp.edit()
                .putBoolean(KEY_AUTO_UPDATE, DEFAULT_AUTO_UPDATE)
                .putBoolean(KEY_V311_INITIALIZED, true)
                .apply();
        }
    }

    /**
     * Initializes the dynamic filter subsystem on app / boot / VPN startup.
     */
    public static void init(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (!isAutoUpdateEnabled(appContext)) {
            initDefaultBootstrapRules();
            return;
        }

        // Immediate bootstrap signatures while cached live feeds load asynchronously
        if (FilterEngine.getDynamicRulesCount() == 0) {
            initDefaultBootstrapRules();
        }

        sExecutor.execute(new Runnable() {
            @Override
            public void run() {
                loadCachedRules(appContext);
                syncIfStale(appContext);
            }
        });

        schedulePeriodicUpdate(appContext);
    }

    /**
     * Triggers an asynchronous update if cached threat feeds are missing or older than 5 hours,
     * throttled by a 5-minute retry cooldown to handle offline/flaky transitions gracefully.
     */
    public static void syncIfStale(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        if (!isAutoUpdateEnabled(appContext) || sIsUpdating.get()) {
            return;
        }

        SharedPreferences sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdate = sp.getLong(KEY_LAST_UPDATE, 0L);
        long now = System.currentTimeMillis();
        File cacheFile = new File(appContext.getFilesDir(), CACHE_FILE_NAME);
        boolean cacheMissing = !cacheFile.exists() || cacheFile.length() == 0;
        boolean expired = (lastUpdate <= 0L) || (now - lastUpdate >= UPDATE_INTERVAL_MS);

        if ((cacheMissing || expired) && (now - sLastSyncAttemptMs >= RETRY_COOLDOWN_MS)) {
            sLastSyncAttemptMs = now;
            updateFiltersAsync(appContext, null);
        }
    }

    /**
     * Registers the recurring 5-hour alarm with Android AlarmManager.
     */
    public static void schedulePeriodicUpdate(Context context) {
        if (context == null) return;
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, FilterUpdateReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 8881, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long firstTrigger = System.currentTimeMillis() + UPDATE_INTERVAL_MS;
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, firstTrigger, UPDATE_INTERVAL_MS, pendingIntent
            );
            Log.i(TAG, "5-Hour Autonomous Filter Auto-Update Scheduled successfully.");
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule AlarmManager update: " + t.getMessage());
        }
    }

    /**
     * Cancels the recurring 5-hour alarm when auto-update is disabled.
     */
    public static void cancelPeriodicUpdate(Context context) {
        if (context == null) return;
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, FilterUpdateReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 8881, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
            Log.i(TAG, "5-Hour Autonomous Filter Auto-Update Cancelled.");
        } catch (Throwable t) {
            Log.w(TAG, "Could not cancel AlarmManager update: " + t.getMessage());
        }
    }

    /**
     * Clears dynamic live feeds when auto-update is switched off, resetting to safe bootstrap rules.
     */
    public static void clearDynamicRules(Context context) {
        FilterEngine.clearDynamicRuleSnapshots();
        FilterEngine.DYNAMIC_DOMAINS.clear();
        initDefaultBootstrapRules();
        if (context != null) {
            try {
                File file = new File(context.getFilesDir(), CACHE_FILE_NAME);
                if (file.exists()) {
                    file.delete();
                }
                SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                sp.edit()
                    .remove(KEY_LAST_UPDATE)
                    .putInt(KEY_DYNAMIC_COUNT, FilterEngine.getDynamicRulesCount())
                    .apply();
            } catch (Throwable t) {
                Log.w(TAG, "Error clearing dynamic rules file: " + t.getMessage());
            }
        }
        DnsForwarder.clearCacheGlobal();
        Log.i(TAG, "Dynamic threat rules cleared and DNS cache flushed.");
    }

    /**
     * Executes asynchronous multi-feed threat sync (Ads, Trackers, Fingerprinting & Exceptions).
     */
    public static void updateFiltersAsync(final Context context, final UpdateCallback callback) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (!sIsUpdating.compareAndSet(false, true)) {
            if (callback != null) {
                sMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onUpdateCompleted(false, FilterEngine.getDynamicRulesCount(), "Update already in progress");
                    }
                });
            }
            return;
        }

        sLastSyncAttemptMs = System.currentTimeMillis();
        if (callback != null) {
            sMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onUpdateStarted();
                }
            });
        }

        sExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                String resultMessage;

                try {
                    DynamicFeedParser.ParsedThreatBundle bundle = DynamicFeedParser.fetchAllLiveFeeds();
                    if (bundle.feedsSucceeded > 0 && bundle.totalBlockCount() > 0) {
                        // Lock-free atomic snapshot swap in FilterEngine
                        FilterEngine.DYNAMIC_DOMAINS.clear();
                        FilterEngine.updateDynamicRuleSnapshots(
                            bundle.ads, bundle.trackers, bundle.fingerprints, bundle.exceptions
                        );

                        saveToLocalStorage(appContext, bundle);
                        DnsForwarder.clearCacheGlobal();

                        SharedPreferences sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        sp.edit()
                            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                            .putInt(KEY_DYNAMIC_COUNT, FilterEngine.getDynamicRulesCount())
                            .apply();

                        success = true;
                        resultMessage = String.format(
                            Locale.US,
                            "Synced %,d ads, %,d trackers, %,d fingerprinting (%d unbreak rules)",
                            bundle.ads.size(), bundle.trackers.size(),
                            bundle.fingerprints.size(), bundle.exceptions.size()
                        );
                        Log.i(TAG, "Hot-reload complete: " + resultMessage);
                    } else {
                        int cachedCount = loadCachedRules(appContext);
                        resultMessage = "Offline. Active threat signatures: " + cachedCount;
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Multi-feed update failed: " + t.getMessage());
                    int cachedCount = loadCachedRules(appContext);
                    resultMessage = "Sync retry queued. Cached threat rules: " + cachedCount;
                } finally {
                    sIsUpdating.set(false);
                }

                final boolean finalSuccess = success;
                final int finalCount = FilterEngine.getDynamicRulesCount();
                final String finalMsg = resultMessage;

                if (callback != null) {
                    sMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onUpdateCompleted(finalSuccess, finalCount, finalMsg);
                        }
                    });
                }
            }
        });
    }

    /**
     * Atomically persists categorized dynamic signatures and exceptions to disk via temp file + sync.
     */
    private static void saveToLocalStorage(Context context, DynamicFeedParser.ParsedThreatBundle bundle) {
        File finalFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
        File tmpFile = new File(context.getFilesDir(), CACHE_FILE_NAME + ".tmp");
        try {
            FileOutputStream fos = new FileOutputStream(tmpFile);
            StringBuilder sb = new StringBuilder(16384);

            appendTaggedDomains(fos, sb, "E|", bundle.exceptions);
            appendTaggedDomains(fos, sb, "F|", bundle.fingerprints);
            appendTaggedDomains(fos, sb, "T|", bundle.trackers);
            appendTaggedDomains(fos, sb, "A|", bundle.ads);

            if (sb.length() > 0) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                sb.setLength(0);
            }
            fos.flush();
            try { fos.getFD().sync(); } catch (Throwable ignored) {}
            fos.close();

            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tmpFile.renameTo(finalFile)) {
                Log.w(TAG, "Could not rename temp threat cache file.");
            } else {
                Log.i(TAG, "Atomically cached " + bundle.totalBlockCount() + " dynamic rules to " + finalFile.getName());
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed saving local threat cache: " + t.getMessage());
            if (tmpFile.exists()) {
                try { tmpFile.delete(); } catch (Throwable ignored) {}
            }
        }
    }

    private static void appendTaggedDomains(FileOutputStream fos, StringBuilder sb, String prefix, Set<String> domains) throws Exception {
        for (String d : domains) {
            sb.append(prefix).append(d).append('\n');
            if (sb.length() >= 16384) {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                sb.setLength(0);
            }
        }
    }

    /**
     * Loads and re-sanitizes categorized dynamic threat signatures from local cache.
     */
    private static int loadCachedRules(Context context) {
        try {
            deleteLegacyCaches(context);

            File file = new File(context.getFilesDir(), CACHE_FILE_NAME);
            if (!file.exists()) {
                if (FilterEngine.getDynamicRulesCount() == 0) {
                    initDefaultBootstrapRules();
                }
                return FilterEngine.getDynamicRulesCount();
            }

            DynamicFeedParser.ParsedThreatBundle bundle = new DynamicFeedParser.ParsedThreatBundle();
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8), 16384);
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("E|")) {
                    String dom = line.substring(2);
                    if (DynamicFeedParser.isValidDomain(dom)) {
                        bundle.exceptions.add(dom);
                    }
                } else if (line.startsWith("F|")) {
                    String dom = line.substring(2);
                    if (DynamicFeedParser.isSafeDynamicDomain(dom)) {
                        bundle.fingerprints.add(dom);
                    }
                } else if (line.startsWith("T|")) {
                    String dom = line.substring(2);
                    if (DynamicFeedParser.isSafeDynamicDomain(dom)) {
                        bundle.trackers.add(dom);
                    }
                } else if (line.startsWith("A|")) {
                    String dom = line.substring(2);
                    if (DynamicFeedParser.isSafeDynamicDomain(dom)) {
                        bundle.ads.add(dom);
                    }
                } else if (DynamicFeedParser.isSafeDynamicDomain(line)) {
                    bundle.ads.add(line);
                }
            }
            reader.close();
            fis.close();

            bundle.finalizeCategories();
            if (bundle.totalBlockCount() > 0) {
                FilterEngine.DYNAMIC_DOMAINS.clear();
                FilterEngine.updateDynamicRuleSnapshots(
                    bundle.ads, bundle.trackers, bundle.fingerprints, bundle.exceptions
                );
                Log.i(TAG, "Loaded and sanitized " + bundle.totalBlockCount() + " cached dynamic rules");
                return FilterEngine.getDynamicRulesCount();
            }

            initDefaultBootstrapRules();
            return FilterEngine.getDynamicRulesCount();
        } catch (Throwable t) {
            Log.w(TAG, "Could not load local cache: " + t.getMessage());
            initDefaultBootstrapRules();
            return FilterEngine.getDynamicRulesCount();
        }
    }

    private static void deleteLegacyCaches(Context context) {
        String[] oldFiles = new String[] {
            "aegis_dynamic_threat_cache.txt",
            "aegis_dynamic_threat_cache_v303.txt",
            "aegis_dynamic_threat_cache_v306.txt",
            "aegis_dynamic_threat_cache_v313.txt"
        };
        for (String name : oldFiles) {
            try {
                File old = new File(context.getFilesDir(), name);
                if (old.exists()) old.delete();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Pre-populates verified zero-day mobile ad & telemetry signatures immediately on cold start.
     */
    private static void initDefaultBootstrapRules() {
        String[] bootstrap = new String[] {
            "trc.taboola.com", "sync.outbrain.com", "static.criteo.net",
            "fastlane.rubiconproject.com", "ib.adnxs.com", "secure.adnxs.com",
            "hbopenbid.pubmatic.com", "dsp.adkernel.com", "engine.adzerk.net",
            "rtb.adxprts.com", "delivery.adxadserv.com", "s.pubmine.com",
            "track.adform.net", "ad.yieldmo.com", "match.adsrvr.org",
            "pixel.advertising.com", "dpm.demdex.net", "match.sharethrough.com",
            "ad.turn.com", "ups.analytics.yahoo.com", "fls.doubleclick.net",
            "app-measurement.com",
            "api.branch.io", "bnc.lt", "sdk.iad-03.braze.com"
        };
        for (String b : bootstrap) {
            FilterEngine.DYNAMIC_DOMAINS.add(b);
        }
    }

    /**
     * Formats human-friendly last-updated display text.
     */
    public static String getLastUpdateFormatted(Context context) {
        if (context == null) return "Every 5 Hours (Active)";
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdate = sp.getLong(KEY_LAST_UPDATE, 0);
        if (lastUpdate == 0) {
            return isAutoUpdateEnabled(context) ? "Syncing automatically every 5h" : "Paused (Tap CHECK NOW)";
        }

        long diff = System.currentTimeMillis() - lastUpdate;
        if (diff < 60_000) {
            return "Just now (Auto-updated)";
        } else if (diff < 3600_000) {
            long mins = diff / 60_000;
            return mins + " min ago (Auto-syncs every 5h)";
        } else if (diff < UPDATE_INTERVAL_MS) {
            long hours = diff / 3600_000;
            return hours + "h ago (Next in " + (5 - hours) + "h)";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(lastUpdate));
        }
    }
}
