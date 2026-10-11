package org.aegisguard.android;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * MainActivity — Authentic Tesla Mobile App executive design.
 * Features:
 * - 196dp Big Clean Circular Tesla Hero Button with concentric illuminated rings
 * - Smooth tactile mechanical spring compression & recoil animation
 * - Icon morphing (lock/unlock) & typography cross-fade transitions
 * - Real-time bidirectional VPN state synchronization
 * - Pure AMOLED pitch black (#000000) backdrop
 */
public class MainActivity extends Activity implements AegisVpnService.TrafficListener, AegisVpnService.StateListener {

    private static final int VPN_REQUEST_CODE = 4001;
    private static final int PERMISSION_REQUEST_CODE = 4002;
    private static final String PREFS_NAME = "aegis_stats_prefs";
    private static final String PREFS_SETTINGS = "aegis_settings_prefs";

    private boolean isToggling = false;

    private TextView tvStatusDot;
    private TextView tvStatusText;
    private TextView tvBatteryStatus;
    private FrameLayout btnSettings;

    // Big Clean Circular Tesla Button
    private FrameLayout btnBigRoundTesla;
    private ImageView ivRoundBtnIcon;
    private TextView tvRoundBtnTitle;
    private TextView tvRoundBtnSubtitle;
    private TextView tvShieldState;

    // Quick Action Dock
    private LinearLayout btnQuickFlush;
    private LinearLayout btnQuickReset;
    private LinearLayout btnQuickModules;
    private LinearLayout btnQuickSettings;

    // Telemetry & Logs
    private TextView tvAdsBlocked;
    private TextView tvTrackersBlocked;
    private TextView tvDataSaved;
    private TextView tvLiveStatusIndicator;
    private TextView tvRecentLog;
    private TextView tvUpstreamStatus;

    private LinearLayout rowDnsStrategy;
    private LinearLayout rowDefenseModules;

    private long displayedAdsCount = 0;
    private long displayedTrackersCount = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final LinkedList<String> recentLogs = new LinkedList<>();
    private final DecimalFormat dataFormat = new DecimalFormat("#,##0.0");
    private SharedPreferences statsPrefs;
    private SharedPreferences settingsPrefs;

    public static class QueryLogEntry {
        public final String domain;
        public final String category;
        public final boolean blocked;
        public final String latency;
        public final long timestamp;

        public QueryLogEntry(String domain, String category, boolean blocked, String latency) {
            this.domain = domain;
            this.category = category;
            this.blocked = blocked;
            this.latency = latency;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static final LinkedList<QueryLogEntry> detailedLogs = new LinkedList<>();

    public static void clearLogs() {
        synchronized (recentLogs) {
            recentLogs.clear();
        }
        synchronized (detailedLogs) {
            detailedLogs.clear();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            statsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

            FilterEngine.loadPreferences(this);
            DynamicFilterManager.init(this);

            initViews();
            setupClickListeners();
            checkPermissions();
            updateUiState();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Error in onCreate", t);
            Toast.makeText(this, "Startup notice: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        tvStatusDot = findViewById(R.id.tvStatusDot);
        tvStatusText = findViewById(R.id.tvStatusText);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        btnSettings = findViewById(R.id.btnSettings);

        btnBigRoundTesla = findViewById(R.id.btnBigRoundTesla);
        ivRoundBtnIcon = findViewById(R.id.ivRoundBtnIcon);
        tvRoundBtnTitle = findViewById(R.id.tvRoundBtnTitle);
        tvRoundBtnSubtitle = findViewById(R.id.tvRoundBtnSubtitle);
        tvShieldState = findViewById(R.id.tvShieldState);

        btnQuickFlush = findViewById(R.id.btnQuickFlush);
        btnQuickReset = findViewById(R.id.btnQuickReset);
        btnQuickModules = findViewById(R.id.btnQuickModules);
        btnQuickSettings = findViewById(R.id.btnQuickSettings);

        tvAdsBlocked = findViewById(R.id.tvAdsBlocked);
        tvTrackersBlocked = findViewById(R.id.tvTrackersBlocked);
        tvDataSaved = findViewById(R.id.tvDataSaved);
        tvLiveStatusIndicator = findViewById(R.id.tvLiveStatusIndicator);
        tvRecentLog = findViewById(R.id.tvRecentLog);
        tvUpstreamStatus = findViewById(R.id.tvUpstreamStatus);

        rowDnsStrategy = findViewById(R.id.rowDnsStrategy);
        rowDefenseModules = findViewById(R.id.rowDefenseModules);
    }

    private void setupClickListeners() {
        btnBigRoundTesla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShield();
            }
        });

        btnBigRoundTesla.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (AegisVpnService.isRunning.get()) {
                    showSnoozeDialog();
                    return true;
                }
                return false;
            }
        });

        if (tvRecentLog != null) {
            tvRecentLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLiveInspectorDialog();
                }
            });
            if (tvRecentLog.getParent() instanceof View) {
                ((View) tvRecentLog.getParent()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLiveInspectorDialog();
                    }
                });
            }
        }

        btnQuickFlush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                DnsForwarder.clearCacheGlobal();
                Toast.makeText(MainActivity.this, "DNS Cache Purged (< 0.01ms)", Toast.LENGTH_SHORT).show();
            }
        });

        btnQuickReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                showResetDialog();
            }
        });

        btnQuickModules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnQuickSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        if (rowDnsStrategy != null) {
            rowDnsStrategy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
        }

        if (rowDefenseModules != null) {
            rowDefenseModules.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
        }
    }

    private void animateButtonPress(View v) {
        v.animate()
            .scaleX(0.90f).scaleY(0.90f)
            .setDuration(90)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                }
            }).start();
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.dialog_reset_title)
            .setMessage(R.string.dialog_reset_message)
            .setPositiveButton(R.string.dialog_confirm_reset, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AegisVpnService.adsBlockedCount.set(0);
                    AegisVpnService.trackersBlockedCount.set(0);
                    AegisVpnService.bytesSaved.set(0);

                    if (statsPrefs != null) {
                        statsPrefs.edit().clear().apply();
                    }
                    displayedAdsCount = 0;
                    displayedTrackersCount = 0;
                    updateUiState();
                    Toast.makeText(MainActivity.this, "Defense telemetry reset to 0", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void toggleShield() {
        if (isToggling) return;
        isToggling = true;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isToggling = false;
            }
        }, 400);

        if (AegisVpnService.isPaused()) {
            AegisVpnService.resumeProtection();
            Intent resumeIntent = new Intent(this, AegisVpnService.class);
            resumeIntent.setAction(AegisVpnService.ACTION_RESUME);
            try {
                startService(resumeIntent);
            } catch (Throwable ignored) {}
            Toast.makeText(this, "AegisGuard Shield Resumed", Toast.LENGTH_SHORT).show();
            updateUiState();
            return;
        }

        boolean running = AegisVpnService.isRunning.get() || 
            (tvRoundBtnTitle != null && "PROTECTED".equals(tvRoundBtnTitle.getText().toString()));

        if (running) {
            // Instantly update UI disengage & notify service
            AegisVpnService.isRunning.set(false);
            if (statsPrefs != null) {
                statsPrefs.edit().putBoolean("shield_enabled", false).apply();
            }

            animateShieldToggle(false);

            Intent stopIntent = new Intent(this, AegisVpnService.class);
            stopIntent.setAction(AegisVpnService.ACTION_STOP);
            try {
                startService(stopIntent);
            } catch (Throwable ignored) {}

            Toast.makeText(this, "AegisGuard Disengaged", Toast.LENGTH_SHORT).show();
        } else {
            // Engaging
            Intent prepareIntent = VpnService.prepare(this);
            if (prepareIntent != null) {
                startActivityForResult(prepareIntent, VPN_REQUEST_CODE);
            } else {
                startAegisService();
            }
        }
    }

    private void startAegisService() {
        animateShieldToggle(true);
        if (statsPrefs != null) {
            statsPrefs.edit().putBoolean("shield_enabled", true).apply();
        }

        Intent startIntent = new Intent(this, AegisVpnService.class);
        startIntent.setAction(AegisVpnService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent);
            } else {
                startService(startIntent);
            }
            Toast.makeText(this, "AegisGuard Shield Engaged", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Failed to start AegisVpnService", t);
            Toast.makeText(this, "Failed to start service: " + t.getMessage(), Toast.LENGTH_LONG).show();
            applyState(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startAegisService();
            } else {
                applyState(false);
                Toast.makeText(this, "VPN permission required for systemwide protection", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            AegisVpnService.setTrafficListener(this);
            AegisVpnService.addStateListener(this);
            applyState(AegisVpnService.isRunning.get());
            AegisVpnService.updateQuickSettingsTile(this);
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Error in onResume", t);
        }
    }

    @Override
    protected void onPause() {
        AegisVpnService.setTrafficListener(null);
        AegisVpnService.removeStateListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onStateChanged(final boolean isRunning) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                applyState(isRunning);
            }
        });
    }

    private void updateUiState() {
        applyState(AegisVpnService.isRunning.get());
    }

    private void animateShieldToggle(final boolean targetActive) {
        if (btnBigRoundTesla == null) return;

        // 1. Tactile physical haptic click
        try {
            btnBigRoundTesla.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        } catch (Exception ignored) {}

        // 2. Mechanical compression & overshoot spring rebound
        btnBigRoundTesla.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(90)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    btnBigRoundTesla.setBackgroundResource(targetActive 
                        ? R.drawable.bg_tesla_big_round_active 
                        : R.drawable.bg_tesla_big_round_standby);

                    btnBigRoundTesla.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(240)
                        .setInterpolator(new OvershootInterpolator(2.4f))
                        .start();
                }
            }).start();

        // 3. Center Icon Shrink-Morph-Expand Animation
        if (ivRoundBtnIcon != null) {
            ivRoundBtnIcon.animate()
                .scaleX(0.15f)
                .scaleY(0.15f)
                .alpha(0.0f)
                .setDuration(90)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (targetActive) {
                            ivRoundBtnIcon.setImageResource(R.drawable.ic_tesla_lock);
                            ivRoundBtnIcon.setColorFilter(getResources().getColor(R.color.text_primary));
                        } else {
                            ivRoundBtnIcon.setImageResource(R.drawable.ic_tesla_unlock);
                            ivRoundBtnIcon.setColorFilter(getResources().getColor(R.color.text_muted));
                        }
                        ivRoundBtnIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(180)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                    }
                }).start();
        }

        // 4. Primary Typography Cross-Fade Animation
        if (tvRoundBtnTitle != null) {
            tvRoundBtnTitle.animate()
                .alpha(0.0f)
                .setDuration(90)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (targetActive) {
                            tvRoundBtnTitle.setText("PROTECTED");
                            tvRoundBtnTitle.setTextColor(getResources().getColor(R.color.text_primary));
                        } else {
                            tvRoundBtnTitle.setText("STANDBY");
                            tvRoundBtnTitle.setTextColor(getResources().getColor(R.color.text_muted));
                        }
                        tvRoundBtnTitle.animate().alpha(1.0f).setDuration(140).start();
                    }
                }).start();
        }

        // 5. Telemetry Caption Cross-Fade Animation
        if (tvRoundBtnSubtitle != null) {
            tvRoundBtnSubtitle.animate()
                .alpha(0.0f)
                .setDuration(90)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (targetActive) {
                            tvRoundBtnSubtitle.setText("0% DRAIN");
                            tvRoundBtnSubtitle.setTextColor(getResources().getColor(R.color.tesla_blue));
                        } else {
                            tvRoundBtnSubtitle.setText("TAP TO ENGAGE");
                            tvRoundBtnSubtitle.setTextColor(getResources().getColor(R.color.text_muted));
                        }
                        tvRoundBtnSubtitle.animate().alpha(1.0f).setDuration(140).start();
                    }
                }).start();
        }

        // 6. Secondary Shield State Text Animation
        if (tvShieldState != null) {
            tvShieldState.animate()
                .alpha(0.0f)
                .setDuration(90)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (targetActive) {
                            tvShieldState.setText("SHIELD ENGAGED");
                            tvShieldState.setTextColor(getResources().getColor(R.color.text_secondary));
                        } else {
                            tvShieldState.setText("SHIELD STANDBY");
                            tvShieldState.setTextColor(getResources().getColor(R.color.text_muted));
                        }
                        tvShieldState.animate().alpha(1.0f).setDuration(140).start();
                    }
                }).start();
        }

        // 7. Header and Status Indicators
        if (targetActive) {
            tvStatusDot.setText("● ");
            tvStatusDot.setTextColor(getResources().getColor(R.color.tesla_green));
            tvStatusText.setText("Protected");
            tvStatusText.setTextColor(getResources().getColor(R.color.text_primary));
            tvBatteryStatus.setText("0.00% / hr • Line-Rate TUN");
            tvLiveStatusIndicator.setText("LIVE");
            tvLiveStatusIndicator.setTextColor(getResources().getColor(R.color.tesla_green));
        } else {
            tvStatusDot.setText("○ ");
            tvStatusDot.setTextColor(getResources().getColor(R.color.text_muted));
            tvStatusText.setText("Standby");
            tvStatusText.setTextColor(getResources().getColor(R.color.text_secondary));
            tvBatteryStatus.setText("Offline");
            tvLiveStatusIndicator.setText("OFFLINE");
            tvLiveStatusIndicator.setTextColor(getResources().getColor(R.color.text_muted));
        }

        renderRecentLogs();
    }

    private void applyState(boolean active) {
        if (btnBigRoundTesla == null || tvRoundBtnTitle == null) return;

        boolean paused = AegisVpnService.isPaused();
        if (active && paused) {
            tvStatusDot.setText("● ");
            tvStatusDot.setTextColor(getResources().getColor(R.color.accent_amber));
            tvStatusText.setText("Snoozed");
            tvStatusText.setTextColor(getResources().getColor(R.color.accent_amber));
            long remMin = Math.max(1, (AegisVpnService.pauseUntilTimestamp - System.currentTimeMillis()) / 60000);
            tvBatteryStatus.setText(remMin + "m remaining • Bypass active");

            btnBigRoundTesla.setBackgroundResource(R.drawable.bg_tesla_big_round_standby);
            ivRoundBtnIcon.setImageResource(R.drawable.ic_tesla_unlock);
            ivRoundBtnIcon.setColorFilter(getResources().getColor(R.color.accent_amber));

            tvRoundBtnTitle.setText("SNOOZED");
            tvRoundBtnTitle.setTextColor(getResources().getColor(R.color.accent_amber));
            tvRoundBtnSubtitle.setText("TAP TO RESUME");
            tvRoundBtnSubtitle.setTextColor(getResources().getColor(R.color.text_secondary));

            tvShieldState.setText("SHIELD SNOOZED");
            tvShieldState.setTextColor(getResources().getColor(R.color.accent_amber));

            tvLiveStatusIndicator.setText("PAUSED");
            tvLiveStatusIndicator.setTextColor(getResources().getColor(R.color.accent_amber));
        } else if (active) {
            tvStatusDot.setText("● ");
            tvStatusDot.setTextColor(getResources().getColor(R.color.tesla_green));
            tvStatusText.setText("Protected");
            tvStatusText.setTextColor(getResources().getColor(R.color.text_primary));
            tvBatteryStatus.setText("0.00% / hr • Line-Rate TUN");

            btnBigRoundTesla.setBackgroundResource(R.drawable.bg_tesla_big_round_active);
            ivRoundBtnIcon.setImageResource(R.drawable.ic_tesla_lock);
            ivRoundBtnIcon.setColorFilter(getResources().getColor(R.color.text_primary));

            tvRoundBtnTitle.setText("PROTECTED");
            tvRoundBtnTitle.setTextColor(getResources().getColor(R.color.text_primary));
            tvRoundBtnSubtitle.setText("0% DRAIN");
            tvRoundBtnSubtitle.setTextColor(getResources().getColor(R.color.tesla_blue));

            tvShieldState.setText("SHIELD ENGAGED");
            tvShieldState.setTextColor(getResources().getColor(R.color.text_secondary));

            tvLiveStatusIndicator.setText("LIVE");
            tvLiveStatusIndicator.setTextColor(getResources().getColor(R.color.tesla_green));
        } else {
            tvStatusDot.setText("○ ");
            tvStatusDot.setTextColor(getResources().getColor(R.color.text_muted));
            tvStatusText.setText("Standby");
            tvStatusText.setTextColor(getResources().getColor(R.color.text_secondary));
            tvBatteryStatus.setText("Offline");

            btnBigRoundTesla.setBackgroundResource(R.drawable.bg_tesla_big_round_standby);
            ivRoundBtnIcon.setImageResource(R.drawable.ic_tesla_unlock);
            ivRoundBtnIcon.setColorFilter(getResources().getColor(R.color.text_muted));

            tvRoundBtnTitle.setText("STANDBY");
            tvRoundBtnTitle.setTextColor(getResources().getColor(R.color.text_muted));
            tvRoundBtnSubtitle.setText("TAP TO ENGAGE");
            tvRoundBtnSubtitle.setTextColor(getResources().getColor(R.color.text_muted));

            tvShieldState.setText("SHIELD STANDBY");
            tvShieldState.setTextColor(getResources().getColor(R.color.text_muted));

            tvLiveStatusIndicator.setText("OFFLINE");
            tvLiveStatusIndicator.setTextColor(getResources().getColor(R.color.text_muted));
        }

        // Smooth Counter Interpolation
        long targetAds = AegisVpnService.adsBlockedCount.get();
        long targetTrackers = AegisVpnService.trackersBlockedCount.get();
        double mbSaved = AegisVpnService.bytesSaved.get() / 1000000.0;

        animateCounter(tvAdsBlocked, displayedAdsCount, targetAds);
        displayedAdsCount = targetAds;

        animateCounter(tvTrackersBlocked, displayedTrackersCount, targetTrackers);
        displayedTrackersCount = targetTrackers;

        tvDataSaved.setText(dataFormat.format(mbSaved) + " MB");

        if (tvUpstreamStatus != null) {
            String provider = settingsPrefs.getString(SettingsActivity.KEY_DNS_PROVIDER, "racing");
            if ("cloudflare".equals(provider)) {
                tvUpstreamStatus.setText("Cloudflare Anycast (1.1.1.1)");
            } else if ("google".equals(provider)) {
                tvUpstreamStatus.setText("Google Public DNS (8.8.8.8)");
            } else if ("quad9".equals(provider)) {
                tvUpstreamStatus.setText("Quad9 Swiss Secure (9.9.9.9)");
            } else if ("adguard".equals(provider)) {
                tvUpstreamStatus.setText("AdGuard Filtering DNS");
            } else if (provider != null && provider.startsWith("custom:")) {
                tvUpstreamStatus.setText("Custom: " + provider.substring("custom:".length()));
            } else {
                tvUpstreamStatus.setText("Racing (Cloudflare + Google Anycast)");
            }
        }

        renderRecentLogs();
    }

    private void showSnoozeDialog() {
        String[] options = {
            "⏸️ Snooze for 5 Minutes",
            "⏸️ Snooze for 15 Minutes",
            "⏸️ Snooze for 1 Hour",
            "🛑 Disengage Shield Completely"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Protection Snooze Timer")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        snoozeShield(5 * 60 * 1000L);
                    } else if (which == 1) {
                        snoozeShield(15 * 60 * 1000L);
                    } else if (which == 2) {
                        snoozeShield(60 * 60 * 1000L);
                    } else {
                        toggleShield();
                    }
                }
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void snoozeShield(long durationMs) {
        Intent pauseIntent = new Intent(this, AegisVpnService.class);
        pauseIntent.setAction(AegisVpnService.ACTION_PAUSE);
        pauseIntent.putExtra(AegisVpnService.EXTRA_PAUSE_DURATION_MS, durationMs);
        startService(pauseIntent);
        AegisVpnService.pauseProtection(durationMs);
        long min = durationMs / 60000;
        Toast.makeText(this, "AegisGuard Snoozed for " + min + " min", Toast.LENGTH_SHORT).show();
        updateUiState();
    }

    private void showLiveInspectorDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Live Traffic Inspector");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 16);

        // Search Bar
        final EditText etSearch = new EditText(this);
        etSearch.setHint("Search domain or service...");
        etSearch.setTextColor(getResources().getColor(R.color.text_primary));
        etSearch.setHintTextColor(getResources().getColor(R.color.text_muted));
        etSearch.setTextSize(13f);
        etSearch.setBackgroundColor(0x22FFFFFF);
        etSearch.setPadding(24, 20, 24, 20);
        root.addView(etSearch);

        // Filter Buttons Container
        final LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, 16, 0, 16);

        final String[] filters = {"ALL", "BLOCKED", "ALLOWED"};
        final int[] currentFilter = {0}; // 0=ALL, 1=BLOCKED, 2=ALLOWED

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800));
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);

        final Runnable populateList = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                String query = etSearch.getText().toString().trim().toLowerCase();
                List<QueryLogEntry> snapshot;
                synchronized (detailedLogs) {
                    snapshot = new ArrayList<>(detailedLogs);
                }

                if (snapshot.isEmpty()) {
                    TextView tvEmpty = new TextView(MainActivity.this);
                    tvEmpty.setText("No query traffic recorded yet. Connect to websites to view live stream.");
                    tvEmpty.setTextColor(getResources().getColor(R.color.text_muted));
                    tvEmpty.setPadding(16, 32, 16, 32);
                    listContainer.addView(tvEmpty);
                    return;
                }

                int count = 0;
                for (final QueryLogEntry entry : snapshot) {
                    if (currentFilter[0] == 1 && !entry.blocked) continue;
                    if (currentFilter[0] == 2 && entry.blocked) continue;
                    if (!query.isEmpty() && !entry.domain.toLowerCase().contains(query)) continue;

                    LinearLayout itemRow = new LinearLayout(MainActivity.this);
                    itemRow.setOrientation(LinearLayout.HORIZONTAL);
                    itemRow.setPadding(16, 16, 16, 16);
                    itemRow.setBackgroundColor(count % 2 == 0 ? 0x0AFFFFFF : 0x00000000);
                    itemRow.setClickable(true);
                    itemRow.setFocusable(true);

                    LinearLayout textCol = new LinearLayout(MainActivity.this);
                    textCol.setOrientation(LinearLayout.VERTICAL);
                    textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    TextView tvDomain = new TextView(MainActivity.this);
                    tvDomain.setText(entry.domain);
                    tvDomain.setTextColor(getResources().getColor(R.color.text_primary));
                    tvDomain.setTextSize(13f);
                    tvDomain.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView tvMeta = new TextView(MainActivity.this);
                    tvMeta.setText(entry.category + " • " + entry.latency);
                    tvMeta.setTextColor(getResources().getColor(R.color.text_secondary));
                    tvMeta.setTextSize(11f);

                    textCol.addView(tvDomain);
                    textCol.addView(tvMeta);

                    TextView tvBadge = new TextView(MainActivity.this);
                    if (entry.blocked) {
                        tvBadge.setText("SINKHOLED");
                        tvBadge.setTextColor(getResources().getColor(R.color.accent_red));
                    } else {
                        tvBadge.setText("PASSED");
                        tvBadge.setTextColor(getResources().getColor(R.color.accent_emerald));
                    }
                    tvBadge.setTextSize(10f);
                    tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);

                    itemRow.addView(textCol);
                    itemRow.addView(tvBadge);

                    itemRow.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showDomainActionDialog(entry);
                        }
                    });

                    listContainer.addView(itemRow);
                    count++;
                }
            }
        };

        for (int i = 0; i < filters.length; i++) {
            final int idx = i;
            final TextView tvChip = new TextView(this);
            tvChip.setText(filters[i]);
            tvChip.setPadding(24, 10, 24, 10);
            tvChip.setTextSize(11f);
            tvChip.setTextColor(getResources().getColor(i == 0 ? R.color.text_primary : R.color.text_muted));
            tvChip.setBackgroundColor(i == 0 ? 0x33FFFFFF : 0x11FFFFFF);
            tvChip.setClickable(true);
            tvChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentFilter[0] = idx;
                    for (int j = 0; j < filterRow.getChildCount(); j++) {
                        TextView c = (TextView) filterRow.getChildAt(j);
                        boolean sel = (j == idx);
                        c.setTextColor(getResources().getColor(sel ? R.color.text_primary : R.color.text_muted));
                        c.setBackgroundColor(sel ? 0x33FFFFFF : 0x11FFFFFF);
                    }
                    populateList.run();
                }
            });
            filterRow.addView(tvChip);
        }

        root.addView(filterRow);
        root.addView(scrollView);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { populateList.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        populateList.run();
        builder.setView(root);
        builder.setPositiveButton("Close", null);
        builder.setNeutralButton("Clear Log", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearLogs();
                renderRecentLogs();
                Toast.makeText(MainActivity.this, "Traffic logs cleared", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showDomainActionDialog(final QueryLogEntry entry) {
        final String domain = entry.domain;
        String[] actions = {
            "⭐ Add to Custom Whitelist (Always Allow)",
            "⛔ Add to Custom Blacklist (Always Block)",
            "📋 Copy Domain Name"
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(domain)
            .setMessage("Category: " + entry.category + "\nStatus: " + (entry.blocked ? "Sinkholed" : "Passed") + "\nLatency: " + entry.latency)
            .setItems(actions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        FilterEngine.addCustomWhitelist(MainActivity.this, domain);
                        Toast.makeText(MainActivity.this, "Added to Whitelist: " + domain, Toast.LENGTH_LONG).show();
                    } else if (which == 1) {
                        FilterEngine.addCustomBlacklist(MainActivity.this, domain);
                        Toast.makeText(MainActivity.this, "Added to Blacklist: " + domain, Toast.LENGTH_LONG).show();
                    } else if (which == 2) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("Domain", domain));
                            Toast.makeText(MainActivity.this, "Copied: " + domain, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void animateCounter(final TextView textView, long startVal, long endVal) {
        if (startVal == endVal) {
            textView.setText(String.format("%,d", endVal));
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(startVal, endVal);
        animator.setDuration(400);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                long current = Math.round((float) animation.getAnimatedValue());
                textView.setText(String.format("%,d", current));
            }
        });
        animator.start();
    }

    private void renderRecentLogs() {
        synchronized (recentLogs) {
            if (recentLogs.isEmpty()) {
                if (AegisVpnService.isRunning.get()) {
                    tvRecentLog.setText("Protection active. Tap here to open Live Traffic Inspector...");
                } else {
                    tvRecentLog.setText("Protection standby. Tap button above to engage defense.");
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (String entry : recentLogs) {
                    sb.append(entry).append("\n");
                }
                tvRecentLog.setText(sb.toString().trim());
            }
        }
    }

    @Override
    public void onTrafficEvent(final String domain, final String category, final boolean blocked, final String latency) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateUiState();
                String line = String.format("[%s] %s: %s", latency, (blocked ? "SINKHOLED" : "PASSED"), domain);
                synchronized (recentLogs) {
                    recentLogs.addFirst(line);
                    if (recentLogs.size() > 5) {
                        recentLogs.removeLast();
                    }
                }
                synchronized (detailedLogs) {
                    detailedLogs.addFirst(new QueryLogEntry(domain, category, blocked, latency));
                    if (detailedLogs.size() > 100) {
                        detailedLogs.removeLast();
                    }
                }
                renderRecentLogs();
            }
        });
    }
}
