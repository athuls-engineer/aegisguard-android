package org.aegisguard.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * 24/7 Immortal Defense & Self-Healing Watchdog Engine.
 * 
 * Guarantees AegisGuard runs perpetually without interruption:
 * 1. Resurrects the VPN service instantly if killed by OS low-memory killer (LMK),
 *    aggressive OEM battery managers (Samsung, Xiaomi, OnePlus, Oppo, Vivo, Huawei),
 *    or when the app is swiped away from Recent Apps.
 * 2. Runs an autonomous periodic heartbeat alarm (every 15 minutes) to verify service health.
 * 3. Restores protection immediately across device reboots, package updates, and network handoffs.
 * 4. Strictly honors user intent: the service turns off ONLY when the user explicitly taps "Turn Off".
 */
public class AegisWatchdogReceiver extends BroadcastReceiver {

    private static final String TAG = "AegisWatchdog";

    public static final String ACTION_WATCHDOG_HEARTBEAT = "org.aegisguard.android.ACTION_WATCHDOG_HEARTBEAT";
    public static final String ACTION_RESURRECT = "org.aegisguard.android.ACTION_RESURRECT";

    private static final int WATCHDOG_REQUEST_CODE = 8881;
    private static final int RESURRECT_REQUEST_CODE = 8882;
    private static final long HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L; // 15 minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        Log.i(TAG, "Watchdog triggered with action: " + action);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            try {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AegisGuard:WatchdogWakeLock");
                wakeLock.acquire(10000L); // 10 seconds max safety timeout
            } catch (Throwable ignored) {}
        }

        try {
            SharedPreferences statsPrefs = context.getSharedPreferences("aegis_stats_prefs", Context.MODE_PRIVATE);
            boolean shouldBeRunning = statsPrefs.getBoolean("shield_enabled", true);

            if (shouldBeRunning) {
                boolean running = AegisVpnService.isRunning.get();
                if (!running) {
                    Log.w(TAG, "AegisGuard was killed or stopped unexpectedly! Resurrecting 24/7 service...");
                    if (VpnService.prepare(context) == null) {
                        Intent startIntent = new Intent(context, AegisVpnService.class);
                        startIntent.setAction(AegisVpnService.ACTION_START);
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(startIntent);
                            } else {
                                context.startService(startIntent);
                            }
                            Log.i(TAG, "AegisGuard 24/7 service resurrection intent dispatched successfully");
                        } catch (Throwable t) {
                            Log.e(TAG, "Failed to resurrect AegisVpnService", t);
                        }
                    } else {
                        Log.w(TAG, "VPN permission requires user confirmation before restart");
                    }
                } else {
                    Log.d(TAG, "Watchdog health check passed: AegisGuard is actively running");
                }

                // Keep the watchdog heartbeat chain alive
                scheduleWatchdog(context);
            } else {
                Log.i(TAG, "Shield was explicitly disabled by user. Watchdog in standby.");
            }
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Schedules the next 15-minute autonomous watchdog heartbeat alarm.
     */
    public static void scheduleWatchdog(Context context) {
        if (context == null) return;
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, AegisWatchdogReceiver.class);
            intent.setAction(ACTION_WATCHDOG_HEARTBEAT);
            PendingIntent pi = PendingIntent.getBroadcast(
                context, WATCHDOG_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL_MS;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
            Log.d(TAG, "Scheduled next 24/7 watchdog heartbeat in 15 minutes");
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule watchdog heartbeat: " + t.getMessage());
        }
    }

    /**
     * Triggers an immediate sub-second resurrection alarm to restart the VPN if killed.
     */
    public static void triggerImmediateResurrection(Context context) {
        if (context == null) return;
        try {
            SharedPreferences statsPrefs = context.getSharedPreferences("aegis_stats_prefs", Context.MODE_PRIVATE);
            boolean shouldBeRunning = statsPrefs.getBoolean("shield_enabled", true);
            if (!shouldBeRunning) return; // Honor explicit user stop

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, AegisWatchdogReceiver.class);
            intent.setAction(ACTION_RESURRECT);
            PendingIntent pi = PendingIntent.getBroadcast(
                context, RESURRECT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = SystemClock.elapsedRealtime() + 300L; // 300ms immediate resurrection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
            Log.i(TAG, "Dispatched immediate 300ms resurrection alarm");
        } catch (Throwable t) {
            Log.w(TAG, "Could not trigger immediate resurrection: " + t.getMessage());
        }
    }

    /**
     * Cancels all watchdog alarms when the user explicitly turns off the shield.
     */
    public static void cancelWatchdog(Context context) {
        if (context == null) return;
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, AegisWatchdogReceiver.class);
            intent.setAction(ACTION_WATCHDOG_HEARTBEAT);
            PendingIntent pi = PendingIntent.getBroadcast(
                context, WATCHDOG_REQUEST_CODE, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }

            Intent resurrectIntent = new Intent(context, AegisWatchdogReceiver.class);
            resurrectIntent.setAction(ACTION_RESURRECT);
            PendingIntent resPi = PendingIntent.getBroadcast(
                context, RESURRECT_REQUEST_CODE, resurrectIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (resPi != null) {
                am.cancel(resPi);
                resPi.cancel();
            }
            Log.i(TAG, "Cancelled watchdog alarms following explicit user disengage");
        } catch (Throwable ignored) {}
    }
}
