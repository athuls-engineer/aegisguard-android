package org.aegisguard.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    public static final String KEY_BLOCK_MALWARE = "block_malware";
    public static final String KEY_BLOCK_CRYPTOMINERS = "block_cryptominers";
    public static final String KEY_AUTO_START = "auto_start_on_boot";
    public static final String KEY_DISALLOWED_APPS = "disallowed_apps_set";

    private FrameLayout btnBack;
    private TextView btnResetStats;
    private TextView btnFlushCache;
    private LinearLayout cardDnsProvider;
    private TextView tvCurrentDnsProvider;
    private LinearLayout cardDnsBenchmark;
    private TextView tvDnsBenchmarkStatus;

    private Switch swBlockAds;
    private Switch swBlockPopups;
    private Switch swBlockTrackers;
    private Switch swBlockFingerprint;
    private Switch swBlockOem;
    private Switch swBlockMalware;
    private Switch swBlockCryptominers;

    private LinearLayout cardCustomRules;
    private TextView tvCustomRulesCount;

    private LinearLayout cardSnoozeDefense;
    private TextView tvSnoozeStatus;

    private LinearLayout cardBypassApps;
    private TextView tvBypassAppsCount;

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
        "AdGuard DNS (94.140.14.14 & 94.140.15.15) [Filtering]",
        "Custom Upstream DNS Server (Enter IPv4)"
    };

    private final String[] dnsProviderKeys = {
        "racing",
        "cloudflare",
        "google",
        "quad9",
        "adguard",
        "custom"
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
        cardDnsBenchmark = findViewById(R.id.cardDnsBenchmark);
        tvDnsBenchmarkStatus = findViewById(R.id.tvDnsBenchmarkStatus);

        swBlockAds = findViewById(R.id.swBlockAds);
        swBlockPopups = findViewById(R.id.swBlockPopups);
        swBlockTrackers = findViewById(R.id.swBlockTrackers);
        swBlockFingerprint = findViewById(R.id.swBlockFingerprint);
        swBlockOem = findViewById(R.id.swBlockOem);
        swBlockMalware = findViewById(R.id.swBlockMalware);
        swBlockCryptominers = findViewById(R.id.swBlockCryptominers);

        cardCustomRules = findViewById(R.id.cardCustomRules);
        tvCustomRulesCount = findViewById(R.id.tvCustomRulesCount);

        cardSnoozeDefense = findViewById(R.id.cardSnoozeDefense);
        tvSnoozeStatus = findViewById(R.id.tvSnoozeStatus);

        cardBypassApps = findViewById(R.id.cardBypassApps);
        tvBypassAppsCount = findViewById(R.id.tvBypassAppsCount);

        swAutoStartOnBoot = findViewById(R.id.swAutoStartOnBoot);
        cardBatteryOptimization = findViewById(R.id.cardBatteryOptimization);
        tvBatteryOptStatus = findViewById(R.id.tvBatteryOptStatus);
        cardAlwaysOnVpn = findViewById(R.id.cardAlwaysOnVpn);

        swAutoUpdateFilters = findViewById(R.id.swAutoUpdateFilters);
        tvThreatRulesCount = findViewById(R.id.tvThreatRulesCount);
        tvThreatLastSync = findViewById(R.id.tvThreatLastSync);
        btnUpdateFiltersNow = findViewById(R.id.btnUpdateFiltersNow);

        TextView tvSettingsSubtitle = findViewById(R.id.tvSettingsSubtitle);
        TextView tvAboutVersion = findViewById(R.id.tvAboutVersion);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String vName = pInfo.versionName;
            if (tvSettingsSubtitle != null) {
                tvSettingsSubtitle.setText("Engine Configuration • v" + vName + " Ultra");
            }
            if (tvAboutVersion != null) {
                tvAboutVersion.setText("AegisGuard v" + vName + " Ultra (Line-Rate Defense & Streaming Bypass)");
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryOptBadge();
        updateThreatIntelligenceDisplay();
        updateCustomRulesDisplay();
        updateSnoozeDisplay();
        updateBypassAppsDisplay();
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

    private void updateCustomRulesDisplay() {
        if (tvCustomRulesCount != null) {
            int whiteCount = FilterEngine.CUSTOM_WHITELIST.size();
            int blackCount = FilterEngine.CUSTOM_BLACKLIST.size();
            tvCustomRulesCount.setText(String.format(Locale.US, "%d Whitelisted • %d Custom Sinkholed", whiteCount, blackCount));
        }
    }

    private void updateBypassAppsDisplay() {
        if (tvBypassAppsCount != null && prefs != null) {
            Set<String> set = prefs.getStringSet(KEY_DISALLOWED_APPS, null);
            int count = (set != null) ? set.size() : 0;
            if (count == 0) {
                tvBypassAppsCount.setText("0 Apps Bypassed • Tap to configure");
            } else {
                tvBypassAppsCount.setText(count + " Apps Bypassed • Tap to configure");
            }
        }
    }

    private void updateSnoozeDisplay() {
        if (tvSnoozeStatus != null) {
            if (AegisVpnService.isPaused()) {
                long rem = Math.max(1, (AegisVpnService.pauseUntilTimestamp - System.currentTimeMillis()) / 60000);
                tvSnoozeStatus.setText("Protection Paused (" + rem + "m left) • Tap to Resume");
                tvSnoozeStatus.setTextColor(getResources().getColor(R.color.accent_amber));
            } else {
                tvSnoozeStatus.setText("Protection Active • Tap to snooze (5m, 15m, 1h)");
                tvSnoozeStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            }
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
        swBlockMalware.setChecked(prefs.getBoolean(KEY_BLOCK_MALWARE, true));
        swBlockCryptominers.setChecked(prefs.getBoolean(KEY_BLOCK_CRYPTOMINERS, true));
        swAutoStartOnBoot.setChecked(prefs.getBoolean(KEY_AUTO_START, true));

        if (swAutoUpdateFilters != null) {
            swAutoUpdateFilters.setChecked(prefs.getBoolean(DynamicFilterManager.KEY_AUTO_UPDATE, false));
        }
        updateThreatIntelligenceDisplay();
        updateCustomRulesDisplay();
        updateSnoozeDisplay();
        updateBypassAppsDisplay();
    }

    private String getProviderDisplayName(String key) {
        if (key != null && key.startsWith("custom:")) {
            return "Custom: " + key.substring("custom:".length());
        }
        for (int i = 0; i < dnsProviderKeys.length; i++) {
            if (dnsProviderKeys[i].equals(key)) {
                return dnsProviderNames[i];
            }
        }
        return dnsProviderNames[0];
    }

    private int getProviderIndex(String key) {
        if (key != null && key.startsWith("custom:")) {
            return 5;
        }
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

        swBlockMalware.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_MALWARE, isChecked).apply();
                FilterEngine.blockMalware = isChecked;
            }
        });

        swBlockCryptominers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_BLOCK_CRYPTOMINERS, isChecked).apply();
                FilterEngine.blockCryptominers = isChecked;
            }
        });

        if (cardCustomRules != null) {
            cardCustomRules.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCustomRulesManagerDialog();
                }
            });
        }

        if (cardSnoozeDefense != null) {
            cardSnoozeDefense.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSnoozeDialog();
                }
            });
        }

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
                        DynamicFilterManager.clearDynamicRules(SettingsActivity.this);
                        updateThreatIntelligenceDisplay();
                        Toast.makeText(SettingsActivity.this, "5-Hour Threat Auto-Sync Paused (Dynamic Rules Cleared)", Toast.LENGTH_SHORT).show();
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

        if (cardDnsBenchmark != null) {
            cardDnsBenchmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    runDnsBenchmark();
                }
            });
        }

        if (cardBypassApps != null) {
            cardBypassApps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBypassAppsDialog();
                }
            });
        }
    }

    private void showCustomRulesManagerDialog() {
        int whiteCount = FilterEngine.CUSTOM_WHITELIST.size();
        int blackCount = FilterEngine.CUSTOM_BLACKLIST.size();
        String[] options = {
            "⭐ Add New Domain to Whitelist (Always Allow)",
            "⛔ Add New Domain to Blacklist (Always Sinkhole)",
            "📋 View Whitelist Rules (" + whiteCount + ")",
            "📋 View Blacklist Rules (" + blackCount + ")",
            "🗑️ Clear All Custom Rules"
        };

        new AlertDialog.Builder(this)
            .setTitle("User Custom Rules Manager")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        showAddRuleInput(true);
                    } else if (which == 1) {
                        showAddRuleInput(false);
                    } else if (which == 2) {
                        showRulesListDialog(true);
                    } else if (which == 3) {
                        showRulesListDialog(false);
                    } else if (which == 4) {
                        FilterEngine.CUSTOM_WHITELIST.clear();
                        FilterEngine.CUSTOM_BLACKLIST.clear();
                        FilterEngine.saveCustomRules(SettingsActivity.this);
                        DnsForwarder.clearCacheGlobal();
                        updateCustomRulesDisplay();
                        Toast.makeText(SettingsActivity.this, "All custom rules cleared", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showAddRuleInput(final boolean isWhitelist) {
        final EditText et = new EditText(this);
        et.setHint(isWhitelist ? "e.g., mysite.com (no https://)" : "e.g., badsite.com (no https://)");
        et.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
            .setTitle(isWhitelist ? "Add Whitelist Domain" : "Add Blacklist Domain")
            .setMessage(isWhitelist ? "This domain and all subdomains will ALWAYS bypass adblocking." : "This domain and all subdomains will ALWAYS be blocked.")
            .setView(et)
            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String domain = et.getText().toString().trim().toLowerCase();
                    if (domain.startsWith("http://")) domain = domain.substring(7);
                    if (domain.startsWith("https://")) domain = domain.substring(8);
                    if (domain.contains("/")) domain = domain.substring(0, domain.indexOf('/'));
                    if (domain.isEmpty()) return;

                    if (isWhitelist) {
                        FilterEngine.addCustomWhitelist(SettingsActivity.this, domain);
                        DnsForwarder.clearCacheGlobal();
                        Toast.makeText(SettingsActivity.this, "Added to Whitelist: " + domain + " (Cache Cleared)", Toast.LENGTH_LONG).show();
                    } else {
                        FilterEngine.addCustomBlacklist(SettingsActivity.this, domain);
                        DnsForwarder.clearCacheGlobal();
                        Toast.makeText(SettingsActivity.this, "Added to Blacklist: " + domain + " (Cache Cleared)", Toast.LENGTH_LONG).show();
                    }
                    updateCustomRulesDisplay();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showRulesListDialog(final boolean isWhitelist) {
        final List<String> rulesList = new ArrayList<>(isWhitelist ? FilterEngine.CUSTOM_WHITELIST : FilterEngine.CUSTOM_BLACKLIST);
        if (rulesList.isEmpty()) {
            Toast.makeText(this, "No " + (isWhitelist ? "whitelist" : "blacklist") + " rules added yet", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] items = rulesList.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle((isWhitelist ? "Custom Whitelist" : "Custom Blacklist") + " (Tap to Remove)")
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String domain = items[which];
                    FilterEngine.removeCustomRule(SettingsActivity.this, domain);
                    DnsForwarder.clearCacheGlobal();
                    updateCustomRulesDisplay();
                    Toast.makeText(SettingsActivity.this, "Removed: " + domain + " (Cache Cleared)", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void showSnoozeDialog() {
        if (AegisVpnService.isPaused()) {
            new AlertDialog.Builder(this)
                .setTitle("Protection Snoozed")
                .setMessage("Defense is currently paused. Resume now?")
                .setPositiveButton("Resume Protection", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AegisVpnService.resumeProtection();
                        Intent resumeIntent = new Intent(SettingsActivity.this, AegisVpnService.class);
                        resumeIntent.setAction(AegisVpnService.ACTION_RESUME);
                        startService(resumeIntent);
                        updateSnoozeDisplay();
                        Toast.makeText(SettingsActivity.this, "AegisGuard Shield Resumed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Keep Snoozed", null)
                .show();
            return;
        }

        String[] options = {
            "⏸️ Snooze for 5 Minutes",
            "⏸️ Snooze for 15 Minutes",
            "⏸️ Snooze for 1 Hour"
        };
        new AlertDialog.Builder(this)
            .setTitle("Snooze Protection")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    long duration = (which == 0 ? 5 : (which == 1 ? 15 : 60)) * 60 * 1000L;
                    Intent pauseIntent = new Intent(SettingsActivity.this, AegisVpnService.class);
                    pauseIntent.setAction(AegisVpnService.ACTION_PAUSE);
                    pauseIntent.putExtra(AegisVpnService.EXTRA_PAUSE_DURATION_MS, duration);
                    startService(pauseIntent);
                    AegisVpnService.pauseProtection(duration);
                    updateSnoozeDisplay();
                    Toast.makeText(SettingsActivity.this, "Protection snoozed for " + (duration / 60000) + " min", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
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
                    if ("custom".equals(selectedKey)) {
                        dialog.dismiss();
                        showCustomDnsPromptDialog();
                    } else {
                        prefs.edit().putString(KEY_DNS_PROVIDER, selectedKey).apply();
                        tvCurrentDnsProvider.setText(dnsProviderNames[which]);
                        DnsForwarder.setUpstreamProviderGlobal(selectedKey);
                        Toast.makeText(SettingsActivity.this, "Upstream DNS set to: " + dnsProviderKeys[which].toUpperCase(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void showCustomDnsPromptDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g., 45.90.28.0 or 192.168.1.1");
        input.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
            .setTitle("Custom Upstream DNS Server")
            .setMessage("Enter the IPv4 address of your custom DNS resolver (NextDNS, Pi-hole, AdGuard Home, or ISP gateway):")
            .setView(input)
            .setPositiveButton("Set Resolver", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String ip = input.getText().toString().trim();
                    if (ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")) {
                        String customKey = "custom:" + ip;
                        prefs.edit().putString(KEY_DNS_PROVIDER, customKey).apply();
                        tvCurrentDnsProvider.setText("Custom: " + ip);
                        DnsForwarder.setUpstreamProviderGlobal(customKey);
                        Toast.makeText(SettingsActivity.this, "Upstream DNS set to custom IP: " + ip, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Invalid IPv4 address format", Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void runDnsBenchmark() {
        if (tvDnsBenchmarkStatus != null) {
            tvDnsBenchmarkStatus.setText("Pinging Cloudflare, Google, Quad9 & Gateway...");
            tvDnsBenchmarkStatus.setTextColor(getResources().getColor(R.color.accent_amber));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> serverNames = new ArrayList<>();
                final List<String> serverIps = new ArrayList<>();

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Network active = cm.getActiveNetwork();
                        if (active != null) {
                            LinkProperties lp = cm.getLinkProperties(active);
                            if (lp != null && lp.getDnsServers() != null && !lp.getDnsServers().isEmpty()) {
                                InetAddress gw = lp.getDnsServers().get(0);
                                if (gw != null && !gw.isLoopbackAddress()) {
                                    serverNames.add("Physical Gateway (" + gw.getHostAddress() + ")");
                                    serverIps.add(gw.getHostAddress());
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                serverNames.add("Cloudflare Anycast (1.1.1.1)");
                serverIps.add("1.1.1.1");
                serverNames.add("Google Public DNS (8.8.8.8)");
                serverIps.add("8.8.8.8");
                serverNames.add("Quad9 Swiss Anycast (9.9.9.9)");
                serverIps.add("9.9.9.9");

                final StringBuilder results = new StringBuilder();
                String fastestName = "Cloudflare";
                long lowestPing = Long.MAX_VALUE;

                // Standard RFC 1035 A-record DNS query for google.com
                byte[] query = new byte[] {
                    (byte)0x12, (byte)0x34, // txId
                    0x01, 0x00,             // standard query, RD=1
                    0x00, 0x01,             // QDCOUNT=1
                    0x00, 0x00, 0x00, 0x00, // AN, NS, AR = 0
                    0x06, 'g', 'o', 'o', 'g', 'l', 'e',
                    0x03, 'c', 'o', 'm', 0x00,
                    0x00, 0x01,             // TYPE A
                    0x00, 0x01              // CLASS IN
                };

                for (int i = 0; i < serverIps.size(); i++) {
                    String name = serverNames.get(i);
                    String ip = serverIps.get(i);
                    long latency = -1;
                    DatagramSocket s = null;
                    try {
                        s = new DatagramSocket();
                        s.setSoTimeout(1200);
                        InetAddress addr = InetAddress.getByName(ip);
                        DatagramPacket pack = new DatagramPacket(query, query.length, addr, 53);
                        long t0 = System.currentTimeMillis();
                        s.send(pack);
                        byte[] recv = new byte[512];
                        DatagramPacket resp = new DatagramPacket(recv, recv.length);
                        s.receive(resp);
                        latency = System.currentTimeMillis() - t0;
                    } catch (Throwable t) {
                        latency = -1;
                    } finally {
                        if (s != null) {
                            try { s.close(); } catch (Exception ignored) {}
                        }
                    }

                    if (latency >= 0) {
                        String tier = (latency < 25) ? "🟢 Ultra-Fast" : (latency < 60) ? "🟡 Good" : "🟠 High Latency";
                        results.append("• ").append(name).append(": ").append(latency).append(" ms (").append(tier).append(")\n");
                        if (latency < lowestPing) {
                            lowestPing = latency;
                            fastestName = name;
                        }
                    } else {
                        results.append("• ").append(name).append(": Timed out / Blocked by carrier\n");
                    }
                }

                final String finalFastest = fastestName;
                final long finalLowest = lowestPing;
                final String finalMsg = results.toString();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tvDnsBenchmarkStatus != null) {
                            if (finalLowest < Long.MAX_VALUE) {
                                tvDnsBenchmarkStatus.setText("Fastest: " + finalFastest + " (" + finalLowest + "ms)");
                                tvDnsBenchmarkStatus.setTextColor(getResources().getColor(R.color.accent_emerald));
                            } else {
                                tvDnsBenchmarkStatus.setText("Benchmark complete • Tap to re-test");
                                tvDnsBenchmarkStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                            }
                        }

                        new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("DNS Latency Benchmark")
                            .setMessage("Live response times to upstream resolvers:\n\n" + finalMsg + 
                                "\nEngine Strategy:\n" +
                                (finalLowest < Long.MAX_VALUE ? 
                                    "Your fastest upstream resolver is " + finalFastest + " (" + finalLowest + "ms).\n\nAegis Turbo Racing races your local gateway and Anycast resolvers in parallel for instant 0ms responses." :
                                    "No response received. Check internet connection or carrier firewall."))
                            .setPositiveButton("OK", null)
                            .show();
                    }
                });
            }
        }).start();
    }

    private static class AppInfoItem {
        final String packageName;
        final String label;
        boolean selected;
        AppInfoItem(String pkg, String label, boolean selected) {
            this.packageName = pkg;
            this.label = label;
            this.selected = selected;
        }
    }

    private void showBypassAppsDialog() {
        Toast.makeText(this, "Scanning installed applications...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final PackageManager pm = getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

                final List<AppInfoItem> appList = new ArrayList<>();
                Set<String> currentDisallowed = prefs.getStringSet(KEY_DISALLOWED_APPS, null);
                if (currentDisallowed == null) currentDisallowed = new HashSet<>();

                String myPkg = getPackageName();
                Set<String> seen = new HashSet<>();
                for (ResolveInfo ri : resolveInfos) {
                    if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                        String pkg = ri.activityInfo.packageName;
                        if (pkg.equals(myPkg) || seen.contains(pkg)) continue;
                        seen.add(pkg);
                        String label = ri.loadLabel(pm).toString();
                        appList.add(new AppInfoItem(pkg, label, currentDisallowed.contains(pkg)));
                    }
                }

                Collections.sort(appList, new Comparator<AppInfoItem>() {
                    @Override
                    public int compare(AppInfoItem o1, AppInfoItem o2) {
                        return o1.label.compareToIgnoreCase(o2.label);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayBypassAppChooser(appList);
                    }
                });
            }
        }).start();
    }

    private void displayBypassAppChooser(final List<AppInfoItem> appList) {
        if (appList.isEmpty()) {
            Toast.makeText(this, "No launchable applications found", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] labels = new String[appList.size()];
        final boolean[] checked = new boolean[appList.size()];
        for (int i = 0; i < appList.size(); i++) {
            AppInfoItem item = appList.get(i);
            labels[i] = item.label + "\n" + item.packageName;
            checked[i] = item.selected;
        }

        new AlertDialog.Builder(SettingsActivity.this)
            .setTitle("App Bypass / Split Tunneling")
            .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checked[which] = isChecked;
                }
            })
            .setPositiveButton("Save & Apply", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Set<String> newDisallowed = new HashSet<>();
                    for (int i = 0; i < appList.size(); i++) {
                        if (checked[i]) {
                            newDisallowed.add(appList.get(i).packageName);
                        }
                    }
                    prefs.edit().putStringSet(KEY_DISALLOWED_APPS, newDisallowed).apply();
                    updateBypassAppsDisplay();

                    if (AegisVpnService.isRunning.get()) {
                        new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("Reconnect Required")
                            .setMessage("Bypass list updated (" + newDisallowed.size() + " apps). Reconnect AegisGuard now to apply split tunneling immediately?")
                            .setPositiveButton("Reconnect Now", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    Intent reconnectIntent = new Intent(SettingsActivity.this, AegisVpnService.class);
                                    reconnectIntent.setAction(AegisVpnService.ACTION_RECONNECT);
                                    startService(reconnectIntent);
                                    Toast.makeText(SettingsActivity.this, "AegisGuard Reconnected with updated Split Tunneling", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Later", null)
                            .show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Bypass list updated (" + newDisallowed.size() + " apps)", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
