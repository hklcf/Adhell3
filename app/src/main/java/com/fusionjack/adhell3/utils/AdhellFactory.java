package com.fusionjack.adhell3.utils;

import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.FirewallPolicy;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.sec.enterprise.firewall.Firewall;

import javax.inject.Inject;

public final class AdhellFactory {
    private static AdhellFactory instance;

    @Nullable
    @Inject
    ApplicationPolicy appPolicy;

    @Nullable
    @Inject
    ApplicationPermissionControlPolicy appControlPolicy;

    @Nullable
    @Inject
    Firewall firewall;

    @Nullable
    @Inject
    FirewallPolicy firewallPolicy;

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    @Inject
    SharedPreferences sharedPreferences;

    private AdhellFactory() {
        App.get().getAppComponent().inject(this);
    }

    public static AdhellFactory getInstance() {
        if (instance == null) {
            instance = new AdhellFactory();
        }
        return instance;
    }

    @Nullable
    public ApplicationPolicy getAppPolicy() {
        return appPolicy;
    }

    @Nullable
    public ApplicationPermissionControlPolicy getAppControlPolicy() {
        return appControlPolicy;
    }

    @Nullable
    public Firewall getFirewall() {
        return firewall;
    }

    @Nullable
    public FirewallPolicy getFirewallPolicy() {
        return firewallPolicy;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}
