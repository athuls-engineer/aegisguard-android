package org.aegisguard.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Advanced Executive Settings & Defense Configuration Activity.
 * Allows granular module customization, DNS provider selection,
 * cache purging, and total statistics resetting.
 */
public class SettingsActivity extends Activity {

    public static final String PREFS_SETTINGS = "aegis_settings_prefs";
    public static final String KEY_DNS_PROVIDER = "dns_provider";
    public static final String KEY_BLOCK_ADS = "block_ads";
    public static final String KEY_BLOCK_POPUPS = "block_popups";
    public static final String KEY_BLOCK_TRACKERS = "block_trackers";
    public static final String KEY_BLOCK_FINGERPRINT = "block_fingerprint";
    public static final String KEY_BLOCK_OEM = "block_oem";
    public static final String KEY_AUTO_START = "auto_start_on_boot";

    private FrameLayout btnBack;
    private TextView btnResetStats;
    private TextView btnFlushCache;
    private LinearLayout cardDnsProvider;
    private TextView tvCurrentDnsProvider;

    private Switch swBlockAds;
    private Switch swBlockPopups;
    private Switch swBlockTrackers;
    private Switch swBlockFingerprint;
    private Switch swBlockOem;

    private Switch swAutoStartOnBoot;
    private LinearLayout cardBatteryOptimization;
    private TextView tvBatteryOptStatus;
    private LinearLayout cardAlwaysOnVpn;

    private Switch swAutoUpdateFilters;
    private TextView tvThreatRulesCount;
    private TextView tvThreatLastSync;
    private TextView btnUpdateFiltersNow;

    private SharedPreferences prefs;

    private final String[] dnsProviderNames = {
        "Aegis Turbo Racing (ISP + Cloudflare 1.1.1.1) [Fastest]",
        "Cloudflare Anycast (1.1.1.1 & 1.0.0.1) [Privacy]",
        "Google Public DNS (8.8.8.8 & 8.8.4.4) [Reliable]",
        "Quad9 Swiss DNS (9.9.9.9 & 149.112.112.112) [Secure]",
        "AdGuard DNS (94.140.14.14 & 94.140.15.15) [Filtering]"
    };

    private final String[] dnsProviderKeys = {
        "racing",
        "cloudflare",
        "google",
        "quad9",
        "adguard"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

        initViews();
        loadPreferences();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnResetStats = findViewById(R.id.btnResetStats);
        btnFlushCache = findViewById(R.id.btnFlushCache);
        cardDnsProvider = findViewById(R.id.cardDnsProvider);
        tvCurrentDnsProvider = findViewById(R.id.tvCurrentDnsProvider);

        swBlockAds = findViewById(R.id.swBlockAds);
        swBlockPopups = findViewById(R.id.swBlockPopups);
        swBlockTrackers = findViewById(R.id.swBlockTrackers);
        swBlockFingerprint = findViewById(R.id.swBlockFingerprint);
        swBlockOem = findViewById(R.id.swBlockOem);

        swAutoStartOnBoot = findViewById(R.id.swAutoStartOnBoot);
        cardBatteryOptimization = findViewById(R.id.cardBatteryOptimization);
        tvBatteryOptStatus = findViewById(R.id.tvBatteryOptStatus);
        cardAlwaysOnVpn = findViewById(R.id.cardAlwaysOnVpn);

        swAutoUpdateFilters = findViewById(R.id.swAutoUpdateFilters);
        tvThreatRulesCount = findViewById(R.id.tvThreatRulesCount);
        tvThreatLastSync = findViewById(R.id.tvThreatLastSync);
        btnUpdateFiltersNow = findViewById(R.id.btnUpdateFiltersNow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryOptBadge();
        updateThreatIntelligenceDisplay();
    }

    private void updateThreatIntelligenceDisplay() {
        if (tvThreatRulesCount != null) {
            int dynamicCount = FilterEngine.DYNAMIC_DOMAINS.size();
            int totalRules = FilterEngine.getTotalRulesCount();
            tvThreatRulesCount.setText(String.format(Locale.US, "Threat Signatures: %,d Active (+%,d live)", totalRules, dynamicCount));
        }
        if (tvThreatLastSync != null) {
            tvThreatLastSync.setText("Last Synced: " + DynamicFilterManager.getLastUpdateFormatted(this));
        }
    }

    private void updateBatteryOptBadge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                tvBatteryOptStatus.setText("UNRESTRICTED");
                tvBatteryOptStatus.setTextColor(getResources().getColor(R.color.accent_emerald));
            } else {
                tvBatteryOptStatus.setText("GRANT WHITELIST");
                tvBatteryOptStatus.setTextColor(getResources().getColor(R.color.accent_cyan));
            }
        }
    }

    private void loadPreferences() {
        String currentProviderKey = prefs.getString(KEY_DNS_PROVIDER, "racing");
        tvCurrentDnsProvider.setText(getProviderDisplayName(currentProviderKey));

        swBlockAds.setChecked(prefs.getBoolean(KEY_BLOCK_ADS, true));
        swBlockPopups.setChecked(prefs.getBoolean(KEY_BLOCK_POPUPS, true));
        swBlockTrackers.setChecked(prefs.getBoolean(KEY_BLOCK_TRACKERS, true));
        swBlockFingerprint.setChecked(prefs.getBoolean(KEY_BLOCK_FINGERPRINT, true));
        swBlockOem.setChecked(prefs.getBoolean(KEY_BLOCK_OEM, true));
        swAutoStartOnBoot.setChecked(prefs.getBoolean(KEY_AUTO_START, true));

        if (swAutoUpdateFilters != null) {
            swAutoUpdateFilters.setChecked(prefs.getBoolean(DynamicFilterManager.KEY_AUTO_UPDATE, true));
        }
        updateThreatIntelligenceDisplay();
    }

    private String getProviderDisplayName(String key) {
        for (int i = 0; i < dnsProviderKeys.length; i++) {
            if (dnsProviderKeys[i].equals(key)) {
                return dnsProviderNames[i];
            }
        }
        return dnsProviderNames[0];
    }

    private int getProviderIndex(String key) {
        for (int i = 0; i < dnsProviderKeys.length; i++) {
            if (dnsProviderKeys[i].equals(key)) {
                return i;
            }
        }
        return 0;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnResetStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirmDialog();
            }
        });

        btnFlushCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DnsForwarder.clearCacheGlobal();
                Toast.makeText(SettingsActivity.this, "In-Memory DNS Cache Purged (0 ms)", Toast.LENGTH_SHORT).show();
            }
        });

        cardDnsProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDnsProviderDialog();
            }
        });

        swBlockAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_ADS, isChecked).apply();
                FilterEngine.blockAds = isChecked;
            }
        });

        swBlockPopups.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_POPUPS, isChecked).apply();
                FilterEngine.blockPopups = isChecked;
            }
        });

        swBlockTrackers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_TRACKERS, isChecked).apply();
                FilterEngine.blockTrackers = isChecked;
            }
        });

        swBlockFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_FINGERPRINT, isChecked).apply();
                FilterEngine.blockFingerprint = isChecked;
            }
        });

        swBlockOem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_OEM, isChecked).apply();
                FilterEngine.blockOem = isChecked;
            }
        });

        swAutoStartOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_AUTO_START, isChecked).apply();
            }
        });

        cardBatteryOptimization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(fallback);
                        } catch (Exception ignored) {
                            Toast.makeText(SettingsActivity.this, "Set AegisGuard battery to 'Unrestricted' in App Info", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });

        cardAlwaysOnVpn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.net.vpn.SETTINGS");
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent fallback = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(fallback);
                    } catch (Exception ignored) {
                        Toast.makeText(SettingsActivity.this, "Open Android Settings -> VPN -> Tap AegisGuard gear icon -> Enable 'Always-on VPN'", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        if (swAutoUpdateFilters != null) {
            swAutoUpdateFilters.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean(DynamicFilterManager.KEY_AUTO_UPDATE, isChecked).apply();
                    if (isChecked) {
                        DynamicFilterManager.schedulePeriodicUpdate(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, "5-Hour Threat Auto-Sync Activated", Toast.LENGTH_SHORT).show();
                    } else {
                        DynamicFilterManager.cancelPeriodicUpdate(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, "5-Hour Threat Auto-Sync Paused", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnUpdateFiltersNow != null) {
            btnUpdateFiltersNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnUpdateFiltersNow.setEnabled(false);
                    btnUpdateFiltersNow.setText("SYNCING...");
                    btnUpdateFiltersNow.setAlpha(0.6f);

                    DynamicFilterManager.updateFiltersAsync(SettingsActivity.this, new DynamicFilterManager.UpdateCallback() {
                        @Override
                        public void onUpdateStarted() {
                            btnUpdateFiltersNow.setText("SYNCING...");
                        }

                        @Override
                        public void onUpdateCompleted(boolean success, int newRulesCount, String message) {
                            btnUpdateFiltersNow.setEnabled(true);
                            btnUpdateFiltersNow.setText("CHECK NOW");
                            btnUpdateFiltersNow.setAlpha(1.0f);
                            updateThreatIntelligenceDisplay();
                            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    private void showResetConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_reset_title)
            .setMessage(R.string.dialog_reset_message)
            .setPositiveButton(R.string.dialog_confirm_reset, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AegisVpnService.resetStats(SettingsActivity.this);
                    MainActivity.clearLogs();
                    Toast.makeText(SettingsActivity.this, "Statistics reset to zero (0)", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void showDnsProviderDialog() {
        String currentKey = prefs.getString(KEY_DNS_PROVIDER, "racing");
        int selectedIndex = getProviderIndex(currentKey);

        new AlertDialog.Builder(this)
            .setTitle(R.string.upstream_dns_title)
            .setSingleChoiceItems(dnsProviderNames, selectedIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String selectedKey = dnsProviderKeys[which];
                    prefs.edit().putString(KEY_DNS_PROVIDER, selectedKey).apply();
                    tvCurrentDnsProvider.setText(dnsProviderNames[which]);
                    DnsForwarder.setUpstreamProviderGlobal(selectedKey);
                    Toast.makeText(SettingsActivity.this, "Upstream DNS set to: " + dnsProviderKeys[which].toUpperCase(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }
}
