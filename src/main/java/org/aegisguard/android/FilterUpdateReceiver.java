package org.aegisguard.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 5-Hour Autonomous Filter Auto-Update Receiver.
 * Wakes up periodically via AlarmManager to pull fresh zero-day ad, tracker,
 * and fingerprinting signatures into AegisGuard's in-memory FilterEngine.
 */
public class FilterUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "FilterUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        Log.i(TAG, "5-Hour Periodic Alarm Triggered: Checking for global threat intelligence updates...");

        if (DynamicFilterManager.isAutoUpdateEnabled(context)) {
            DynamicFilterManager.updateFiltersAsync(context.getApplicationContext(), null);
            // Guarantee reschedule for next 5 hours
            DynamicFilterManager.schedulePeriodicUpdate(context.getApplicationContext());
        }
    }
}
