package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;

public final class AppPreferences {
    private static AppPreferences instance;
    private SharedPreferences sharedPreferences;

    private static final String BLOCKED_DOMAINS_COUNT = "blockedDomainsCount";
    private static final String DISABLER_TOGGLE = "disablerToggle";
    private final static String DNS_ALL_APPS_ENABLED = "dnsAllAppsEnabled";
    private static final String DNS1 = "dns1";
    private static final String DNS2 = "dns2";

    private AppPreferences() {
        sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
    }

    public static AppPreferences getInstance() {
        if (instance == null) {
            instance = new AppPreferences();
        }
        return instance;
    }

    public boolean isAppDisablerEnabled() {
        return sharedPreferences.getBoolean(DISABLER_TOGGLE, true);
    }

    public void setAppDisabler(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DISABLER_TOGGLE, enabled);
        editor.apply();
    }

    public boolean isDnsAllAppsEnabled() {
        return sharedPreferences.getBoolean(DNS_ALL_APPS_ENABLED, false);
    }

    public void setDnsAllApps(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DNS_ALL_APPS_ENABLED, enabled);
        editor.apply();
    }

    public String getDns1() {
        return sharedPreferences.getString(DNS1, "0.0.0.0");
    }

    public String getDns2() {
        return sharedPreferences.getString(DNS2, "0.0.0.0");
    }

    public boolean isDnsNotEmpty() {
        return sharedPreferences.contains(DNS1) && sharedPreferences.contains(DNS2);
    }

    public void setDns(String dns1, String dns2) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DNS1, dns1);
        editor.putString(DNS2, dns2);
        editor.apply();
    }

    public void removeDns() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DNS1);
        editor.remove(DNS2);
        editor.apply();
    }

    public void setBlockedDomainsCount(int count) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BLOCKED_DOMAINS_COUNT, count);
        editor.apply();
    }

    public int getBlockedDomainsCount() {
        return sharedPreferences.getInt(BLOCKED_DOMAINS_COUNT, 0);
    }

    public void resetBlockedDomainsCount() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(BLOCKED_DOMAINS_COUNT);
        editor.apply();
    }
}
