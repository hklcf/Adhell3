package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.enterprise.ApplicationPolicy;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppsListDBInitializer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RefreshAppAsyncTask extends AsyncTask<Void, Void, Void> {
    private WeakReference<Context> contextReference;
    private AppFlag appFlag;

    RefreshAppAsyncTask(AppFlag appFlag, Context context) {
        this.appFlag = appFlag;
        this.contextReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Get first disabled, restricted and whitelisted apps before they get deleted
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppInfo> disabledApps = appDatabase.applicationInfoDao().getDisabledApps();
        List<AppInfo> mobileRestrictedApps = appDatabase.applicationInfoDao().getMobileRestrictedApps();
        List<AppInfo> wifiRestrictedApps = appDatabase.applicationInfoDao().getWifiRestrictedApps();
        List<FirewallWhitelistedPackage> whitelistedApps = appDatabase.firewallWhitelistedPackageDao().getAll();

        // Delete all apps info
        appDatabase.applicationInfoDao().deleteAll();
        AppsListDBInitializer.getInstance().fillPackageDb(AdhellFactory.getInstance().getPackageManager());

        // Disable apps
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        appDatabase.disabledPackageDao().deleteAll();
        List<DisabledPackage> disabledPackages = new ArrayList<>();
        for (AppInfo oldAppInfo : disabledApps) {
            appPolicy.setEnableApplication(oldAppInfo.packageName);
            AppInfo newAppInfo = appDatabase.applicationInfoDao().getAppByPackageName(oldAppInfo.packageName);
            if (newAppInfo != null) {
                newAppInfo.disabled = true;
                appDatabase.applicationInfoDao().insert(newAppInfo);
                appPolicy.setDisableApplication(newAppInfo.packageName);

                DisabledPackage disabledPackage = new DisabledPackage();
                disabledPackage.packageName = newAppInfo.packageName;
                disabledPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                disabledPackages.add(disabledPackage);
            }
        }
        appDatabase.disabledPackageDao().insertAll(disabledPackages);

        // Restricted apps
        appDatabase.restrictedPackageDao().deleteAll();
        List<RestrictedPackage> restrictedPackages = new ArrayList<>();
        for (AppInfo oldAppInfo : mobileRestrictedApps) {
            AppInfo newAppInfo = appDatabase.applicationInfoDao().getAppByPackageName(oldAppInfo.packageName);
            if (newAppInfo != null) {
                newAppInfo.mobileRestricted = true;
                appDatabase.applicationInfoDao().insert(newAppInfo);

                RestrictedPackage restrictedPackage = new RestrictedPackage();
                restrictedPackage.packageName = newAppInfo.packageName;
                restrictedPackage.type = DatabaseFactory.MOBILE_RESTRICTED_TYPE;
                restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                restrictedPackages.add(restrictedPackage);
            }
        }
        for (AppInfo oldAppInfo : wifiRestrictedApps) {
            AppInfo newAppInfo = appDatabase.applicationInfoDao().getAppByPackageName(oldAppInfo.packageName);
            if (newAppInfo != null) {
                newAppInfo.wifiRestricted = true;
                appDatabase.applicationInfoDao().insert(newAppInfo);

                RestrictedPackage restrictedPackage = new RestrictedPackage();
                restrictedPackage.packageName = newAppInfo.packageName;
                restrictedPackage.type = DatabaseFactory.WIFI_RESTRICTED_TYPE;
                restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                restrictedPackages.add(restrictedPackage);
            }
        }
        appDatabase.restrictedPackageDao().insertAll(restrictedPackages);

        // Whitelisted apps
        appDatabase.firewallWhitelistedPackageDao().deleteAll();
        List<FirewallWhitelistedPackage> whitelistedPackages = new ArrayList<>();
        for (FirewallWhitelistedPackage oldAppInfo : whitelistedApps) {
            AppInfo newAppInfo = appDatabase.applicationInfoDao().getAppByPackageName(oldAppInfo.packageName);
            if (newAppInfo != null) {
                newAppInfo.adhellWhitelisted = true;
                appDatabase.applicationInfoDao().insert(newAppInfo);

                FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                whitelistedPackage.packageName = newAppInfo.packageName;
                whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                whitelistedPackages.add(whitelistedPackage);
            }
        }
        appDatabase.firewallWhitelistedPackageDao().insertAll(whitelistedPackages);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Context context = contextReference.get();
        if (context != null) {
            SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(appFlag.getRefreshLayout());
            if (swipeContainer != null) {
                swipeContainer.setRefreshing(false);
            }

            new LoadAppAsyncTask("", appFlag, true, context).execute();
        }
    }
}
