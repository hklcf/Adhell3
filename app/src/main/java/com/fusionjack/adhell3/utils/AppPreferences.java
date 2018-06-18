package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;

public final class AppPreferences {
    private static AppPreferences instance;
    private SharedPreferences sharedPreferences;

    private static final String DISABLER_TOGGLE = "disablerToggle";

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

    public void enableAppDisabler(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DISABLER_TOGGLE, enabled);
        editor.apply();
    }
}
