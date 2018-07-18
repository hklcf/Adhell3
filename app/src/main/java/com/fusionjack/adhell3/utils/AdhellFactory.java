package com.fusionjack.adhell3.utils;

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
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;

import static com.samsung.android.knox.application.ApplicationPolicy.ERROR_UNKNOWN;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DEFAULT;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_GRANT;

public final class AdhellFactory {
    private static AdhellFactory instance;

    @Nullable
    @Inject
    ApplicationPolicy appPolicy;

    @Nullable
    @Inject
    Firewall firewall;

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    @Inject
    SharedPreferences sharedPreferences;

    @Nullable
    @Inject
    KnoxEnterpriseLicenseManager knoxEnterpriseLicenseManager;

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
    public Firewall getFirewall() {
        return firewall;
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

    public void createNotSupportedDialog(Context context) {
        String knoxIsSupported = "Knox Enterprise License Manager is " + (knoxEnterpriseLicenseManager == null ? "not available" : "available");
        String knoxApiLevel = "Knox API Level: " + EnterpriseDeviceManager.getAPILevel();
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.not_supported_dialog_title))
                .setMessage(knoxIsSupported + "\n" + knoxApiLevel)
                .show();
    }

    public void createNoInternetConnectionDialog(Context context) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_error_black_24dp)
                .setTitle(context.getString(R.string.no_internet_connection_dialog_title))
                .setMessage(context.getString(R.string.no_internet_connection))
                .show();
    }

    public void setAppComponentToggle(boolean enabled) {
        // If the toggle is enabled, then we need to disable the app component
        AppPreferences.getInstance().setAppComponentToggle(enabled);
        boolean state = !enabled;
        setAppComponentState(state);
    }

    public void setAppComponentState(boolean state) {
        if (appPolicy == null) {
            return;
        }

        List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
        for (AppPermission appPermission : appPermissions) {
            String packageName = appPermission.packageName;
            String permissionName = appPermission.permissionName;
            switch (appPermission.permissionStatus) {
                case AppPermission.STATUS_PERMISSION:
                    List<String> permissions = new ArrayList<>();
                    permissions.add(permissionName);
                    setAppPermission(packageName, permissions, state);
                    break;
                case AppPermission.STATUS_SERVICE:
                    ComponentName componentName = new ComponentName(packageName, permissionName);
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
                case AppPermission.STATUS_RECEIVER:
                    StringTokenizer tokenizer = new StringTokenizer(permissionName, "|");
                    componentName = new ComponentName(packageName, tokenizer.nextToken());
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
            }
        }
    }

    public int setAppPermission(String packageName, List<String> permissions, boolean state) {
        if (appPolicy == null) {
            return ERROR_UNKNOWN;
        }

        if (state) {
            int errorCode = appPolicy.applyRuntimePermissions(new AppIdentity(packageName, null), permissions, PERMISSION_POLICY_STATE_GRANT);
            if (errorCode == ApplicationPolicy.ERROR_NONE) {
                return appPolicy.applyRuntimePermissions(new AppIdentity(packageName, null), permissions, PERMISSION_POLICY_STATE_DEFAULT);
            }
        }
        return appPolicy.applyRuntimePermissions(new AppIdentity(packageName, null), permissions, PERMISSION_POLICY_STATE_DENY);
    }

    public void setDns(String primaryDns, String secondaryDns, Handler handler) {
        if (primaryDns.isEmpty() && secondaryDns.isEmpty()) {
            AppPreferences.getInstance().removeDns();
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
            AppPreferences.getInstance().setDns(primaryDns, secondaryDns);
            if (handler != null) {
                Message message = handler.obtainMessage(0, R.string.changed_dns);
                message.sendToTarget();
            }
        }
    }

    public void applyDns(Handler handler) {
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            String dns1 = AppPreferences.getInstance().getDns1();
            String dns2 = AppPreferences.getInstance().getDns2();
            if (Patterns.IP_ADDRESS.matcher(dns1).matches() && Patterns.IP_ADDRESS.matcher(dns2).matches()) {
                LogUtils.getInstance().writeInfo("\nProcessing DNS...", handler);

                LogUtils.getInstance().writeInfo("DNS 1: " + dns1, handler);
                LogUtils.getInstance().writeInfo("DNS 2: " + dns2, handler);
                List<AppInfo> dnsPackages = appDatabase.applicationInfoDao().getDnsApps();
                if (dnsPackages.size() == 0) {
                    LogUtils.getInstance().writeInfo("No app is selected", handler);
                } else {
                    LogUtils.getInstance().writeInfo("Size: " + dnsPackages.size(), handler);
                    List<DomainFilterRule> rules = new ArrayList<>();
                    for (AppInfo app : dnsPackages) {
                        DomainFilterRule rule = new DomainFilterRule(new AppIdentity(app.packageName, null));
                        rule.setDns1(dns1);
                        rule.setDns2(dns2);
                        rules.add(rule);
                    }

                    try {
                        FirewallUtils.getInstance().addDomainFilterRules(rules, handler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setAppDisablerToggle(boolean state) {
        if (appPolicy == null) {
            return;
        }

        AppPreferences.getInstance().setAppDisablerToggle(state);
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        for (DisabledPackage disabledPackage : disabledPackages) {
            if (state) {
                appPolicy.setDisableApplication(disabledPackage.packageName);
            } else {
                appPolicy.setEnableApplication(disabledPackage.packageName);
            }
        }
    }

    public void updateAllProviders() {
        // Fetch URL providers
        List<BlockUrlProvider> providers = appDatabase.blockUrlProviderDao().getAll2();
        // Remove all blockurls
        appDatabase.blockUrlDao().deleteAll();

        /* Initial blockurl fetch */

        // For each blockurlprovider
        for (BlockUrlProvider provider : providers) {
            try {
                // Fetch the block urls
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                // Add to blockurls DB
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* Process Filters and update provider stats */

        // Fetch Adhell filters
        List<BlockUrl> adhellFilters = appDatabase.blockUrlDao().getAdhellFilters();
        // If there are filters, try to process them
        if (!adhellFilters.isEmpty()) {
            try {
                BlockUrlUtils.processAdhellFilters(adhellFilters, appDatabase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            // For each blockurlprovider
            for (BlockUrlProvider provider : providers) {
                // Set unique count
                provider.count = appDatabase.blockUrlProviderDao().getProviderUniqueUrlsCount(provider.id);
                // Set update time
                provider.lastUpdated = new Date();
                // Apply updates to DB
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
