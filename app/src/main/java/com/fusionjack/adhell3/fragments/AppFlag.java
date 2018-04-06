package com.fusionjack.adhell3.fragments;

import android.app.enterprise.ApplicationPolicy;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;

import javax.inject.Inject;

public class AppFlag {

    @Nullable
    @Inject
    ApplicationPolicy appPolicy;

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    public enum Flag {
        DISABLER_FLAG,
        RESTRICTED_FLAG
    }

    private Flag flag;

    private AppFlag(Flag flag) {
        this.flag = flag;
        App.get().getAppComponent().inject(this);
    }

    public static AppFlag createDisablerFlag() {
        return new AppFlag(Flag.DISABLER_FLAG);
    }

    public static AppFlag createRestrictedFlag() {
        return new AppFlag(Flag.RESTRICTED_FLAG);
    }

    @Nullable
    public ApplicationPolicy getAppPolicy() {
        return appPolicy;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public Flag getFlag() {
        return flag;
    }
}
