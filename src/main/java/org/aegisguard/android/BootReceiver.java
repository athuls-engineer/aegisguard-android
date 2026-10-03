package org.aegisguard.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

/**
 * Ensures systemwide ad blocking resumes silently on phone restart, direct boot,
 * OEM quick-power-on, and APK package updates.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "AegisBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            SharedPreferences statsPrefs = context.getSharedPreferences("aegis_stats_prefs", Context.MODE_PRIVATE);
            SharedPreferences settingsPrefs = context.getSharedPreferences("aegis_settings_prefs", Context.MODE_PRIVATE);

            boolean autoStart = settingsPrefs.getBoolean("auto_start_on_boot", true);
            boolean wasShieldActive = statsPrefs.getBoolean("shield_enabled", true);

            if (autoStart && wasShieldActive) {
                // Verify VPN preparation is already approved
                if (VpnService.prepare(context) == null) {
                    Intent startIntent = new Intent(context, AegisVpnService.class);
                    startIntent.setAction(AegisVpnService.ACTION_START);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(startIntent);
                        } else {
                            context.startService(startIntent);
                        }
                        Log.i(TAG, "AegisGuard Shield auto-activated on boot/update successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Could not auto-start AegisGuard", e);
                    }
                }
            }

            // Ensure 5-Hour Dynamic Filter Auto-Update is active on device startup
            DynamicFilterManager.schedulePeriodicUpdate(context);
        }
    }
}
