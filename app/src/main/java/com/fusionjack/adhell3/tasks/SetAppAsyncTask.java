package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.app.enterprise.ApplicationPolicy;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ListView;

import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.lang.ref.WeakReference;

public class SetAppAsyncTask extends AsyncTask<Void, Void, Void> {
    private AppFlag appFlag;
    private AppInfo appInfo;
    private WeakReference<Context> contextWeakReference;

    public SetAppAsyncTask(AppInfo appInfo, AppFlag appFlag, Context context) {
        this.appInfo = appInfo;
        this.appFlag = appFlag;
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        if (appPolicy == null) {
            return null;
        }

        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String packageName = appInfo.packageName;
        switch (appFlag.getFlag()) {
            case DISABLER_FLAG:
                appInfo.disabled = !appInfo.disabled;
                if (appInfo.disabled) {
                    appPolicy.setDisableApplication(packageName);
                    DisabledPackage disabledPackage = new DisabledPackage();
                    disabledPackage.packageName = packageName;
                    disabledPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.disabledPackageDao().insert(disabledPackage);
                } else {
                    appPolicy.setEnableApplication(packageName);
                    appDatabase.disabledPackageDao().deleteByPackageName(packageName);
                }
                break;

            case MOBILE_RESTRICTED_FLAG:
                appInfo.mobileRestricted = !appInfo.mobileRestricted;
                if (appInfo.mobileRestricted) {
                    RestrictedPackage restrictedPackage = new RestrictedPackage();
                    restrictedPackage.packageName = packageName;
                    restrictedPackage.type = DatabaseFactory.MOBILE_RESTRICTED_TYPE;
                    restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.restrictedPackageDao().insert(restrictedPackage);
                } else {
                    appDatabase.restrictedPackageDao().deleteByPackageName(packageName, DatabaseFactory.MOBILE_RESTRICTED_TYPE);
                }
                break;

            case WIFI_RESTRICTED_FLAG:
                appInfo.wifiRestricted = !appInfo.wifiRestricted;
                if (appInfo.wifiRestricted) {
                    RestrictedPackage restrictedPackage = new RestrictedPackage();
                    restrictedPackage.packageName = packageName;
                    restrictedPackage.type = DatabaseFactory.WIFI_RESTRICTED_TYPE;
                    restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.restrictedPackageDao().insert(restrictedPackage);
                } else {
                    appDatabase.restrictedPackageDao().deleteByPackageName(packageName, DatabaseFactory.WIFI_RESTRICTED_TYPE);
                }
                break;

            case WHITELISTED_FLAG:
                appInfo.adhellWhitelisted = !appInfo.adhellWhitelisted;
                if (appInfo.adhellWhitelisted) {
                    FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                    whitelistedPackage.packageName = packageName;
                    whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.firewallWhitelistedPackageDao().insert(whitelistedPackage);
                } else {
                    appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(packageName);
                }
                break;

            case DNS_FLAG:
                appInfo.hasCustomDns = !appInfo.hasCustomDns;
                if (appInfo.hasCustomDns) {
                    DnsPackage dnsPackage = new DnsPackage();
                    dnsPackage.packageName = packageName;
                    dnsPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.dnsPackageDao().insert(dnsPackage);
                } else {
                    appDatabase.dnsPackageDao().deleteByPackageName(packageName);
                }
                break;
        }
        appDatabase.applicationInfoDao().update(appInfo);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Context context = contextWeakReference.get();
        if (context != null) {
            ListView listView = ((Activity) context).findViewById(appFlag.getLoadLayout());
            if (listView != null) {
                if (listView.getAdapter() instanceof AppInfoAdapter) {
                    ((AppInfoAdapter) listView.getAdapter()).notifyDataSetChanged();
                }
            }
        }
    }
}
