package com.fusionjack.adhell3.utils;

import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.FirewallPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Patterns;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.blocker.ContentBlocker57;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.sec.enterprise.AppIdentity;
import com.sec.enterprise.firewall.DomainFilterRule;
import com.sec.enterprise.firewall.Firewall;
import com.sec.enterprise.firewall.FirewallResponse;
import com.sec.enterprise.firewall.FirewallRule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

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

    public void setAppComponentState(boolean state) {
        if (appControlPolicy == null && appPolicy == null) {
            return;
        }

        List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
        for (AppPermission appPermission : appPermissions) {
            List<String> packageList = new ArrayList<>();
            packageList.add(appPermission.packageName);
            switch (appPermission.permissionStatus) {
                case AppPermission.STATUS_PERMISSION:
                    if (state) {
                        appControlPolicy.removePackagesFromPermissionBlackList(appPermission.permissionName, packageList);
                    } else {
                        appControlPolicy.addPackagesToPermissionBlackList(appPermission.permissionName, packageList);
                    }
                    break;
                case AppPermission.STATUS_SERVICE:
                    ComponentName componentName = new ComponentName(appPermission.packageName, appPermission.permissionName);
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
                case AppPermission.STATUS_RECEIVER:
                    StringTokenizer tokenizer = new StringTokenizer(appPermission.permissionName, "|");
                    componentName = new ComponentName(appPermission.packageName, tokenizer.nextToken());
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
            }
        }

        if (state) {
            appDatabase.appPermissionDao().deleteAll();
        }
    }

    public boolean isDnsAllowed() {
        ContentBlocker contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();
        return contentBlocker instanceof ContentBlocker56 || contentBlocker instanceof ContentBlocker57;
    }

    public boolean isDnsNotEmpty() {
        return sharedPreferences.contains("dns1") && sharedPreferences.contains("dns2");
    }

    public void setDns(String primaryDns, String secondaryDns, Handler handler) {
        if (primaryDns.isEmpty() && secondaryDns.isEmpty()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("dns1");
            editor.remove("dns2");
            editor.apply();
            if (handler != null) {
                Message message = handler.obtainMessage(0, R.string.restored_dns);
                message.sendToTarget();
            }
        } else if (!Patterns.IP_ADDRESS.matcher(primaryDns).matches() || !Patterns.IP_ADDRESS.matcher(secondaryDns).matches()) {
            if (handler != null) {
                Message message = handler.obtainMessage(0, R.string.check_input_dns);
                message.sendToTarget();
            }
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("dns1", primaryDns);
            editor.putString("dns2", secondaryDns);
            editor.apply();
            if (handler != null) {
                Message message = handler.obtainMessage(0, R.string.changed_dns);
                message.sendToTarget();
            }
        }
    }

    public void applyDns(Handler handler) {
        if (isDnsNotEmpty()) {
            String dns1 = sharedPreferences.getString("dns1", "0.0.0.0");
            String dns2 = sharedPreferences.getString("dns2", "0.0.0.0");
            if (Patterns.IP_ADDRESS.matcher(dns1).matches() && Patterns.IP_ADDRESS.matcher(dns2).matches()) {
                LogUtils.getInstance().writeInfo("\nProcessing DNS...", handler);

                List<AppInfo> dnsPackages = AdhellFactory.getInstance().getAppDatabase().applicationInfoDao().getDnsApps();
                if (dnsPackages.size() == 0) {
                    LogUtils.getInstance().writeInfo("No app is selected", handler);
                } else {
                    LogUtils.getInstance().writeInfo("DNS app size: " + dnsPackages.size(), handler);
                    List<DomainFilterRule> rules = new ArrayList<>();
                    for (AppInfo app : dnsPackages) {
                        DomainFilterRule rule = new DomainFilterRule(new AppIdentity(app.packageName, null));
                        rule.setDns1(dns1);
                        rule.setDns2(dns2);
                        rules.add(rule);
                    }

                    try {
                        addDomainFilterRules(rules, handler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void updateAllProviders() {
        List<BlockUrlProvider> providers = appDatabase.blockUrlProviderDao().getAll2();
        appDatabase.blockUrlDao().deleteAll();
        for (BlockUrlProvider provider : providers) {
            try {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                provider.count = blockUrls.size();
                provider.lastUpdated = new Date();
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     public FirewallRule[] createFirewallRules(String packageName, Firewall.NetworkInterface networkInterface) {
         FirewallRule[] rules = new FirewallRule[2];

         rules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
         rules[0].setNetworkInterface(networkInterface);
         rules[0].setApplication(new AppIdentity(packageName, null));

         rules[1] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV6);
         rules[1].setNetworkInterface(networkInterface);
         rules[1].setApplication(new AppIdentity(packageName, null));

         return rules;
     }
}
