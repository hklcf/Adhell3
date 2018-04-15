package com.fusionjack.adhell3.utils;

import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.FirewallPolicy;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.sec.enterprise.firewall.DomainFilterRule;
import com.sec.enterprise.firewall.Firewall;
import com.sec.enterprise.firewall.FirewallResponse;
import com.sec.enterprise.firewall.FirewallRule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

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

    public AlertDialog createNotSupportedDialog(Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_error_black_24dp)
                .setTitle(context.getString(R.string.not_supported_dialog_title))
                .setMessage(context.getString(R.string.adhell_not_supported))
                .show();
    }

    public AlertDialog createNoInternetConnectionDialog(Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_error_black_24dp)
                .setTitle(context.getString(R.string.no_internet_connection_dialog_title))
                .setMessage(context.getString(R.string.no_internet_connection))
                .show();
    }

    public void addDomainFilterRules(List<DomainFilterRule> domainRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            LogUtils.getInstance().writeInfo("Adding rule(s) to Knox Firewall...", handler);
            FirewallResponse[] response = firewall.addDomainFilterRules(domainRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add domain filter rule to Knox Firewall", ex, handler);
        }
    }

    public void addFirewallRules(FirewallRule[] firewallRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            LogUtils.getInstance().writeInfo("Adding rule(s) to Knox Firewall...", handler);
            FirewallResponse[] response = firewall.addRules(firewallRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add firewall rules to Knox Firewall", ex, handler);
        }
    }

    private void handleResponse(FirewallResponse[] response, Handler handler) throws Exception {
        if (response == null) {
            Exception ex = new Exception("There was no response from Knox Firewall");
            LogUtils.getInstance().writeError("There was no response from Knox Firewall", ex, handler);
            throw ex;
        } else {
            LogUtils.getInstance().writeInfo("Result: " + response[0].getMessage(), handler);
            if (FirewallResponse.Result.SUCCESS != response[0].getResult()) {
                Exception ex = new Exception(response[0].getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                LogUtils.getInstance().writeError(sw.toString(), ex, handler);
                throw ex;
            }
        }
    }
}
