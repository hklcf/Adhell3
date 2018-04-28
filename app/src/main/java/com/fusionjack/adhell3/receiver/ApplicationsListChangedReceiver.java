package com.fusionjack.adhell3.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppsListDBInitializer;

public class ApplicationsListChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AsyncTask.execute(() ->
        {
            Uri data = intent.getData();
            String action = intent.getAction();
            if (data == null || action == null) {
                return;
            }

            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            String packageName = data.getEncodedSchemeSpecificPart();
            if (!packageName.isEmpty()) {
                AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                if (action.equalsIgnoreCase("android.intent.action.PACKAGE_ADDED") && !isReplacing) {
                    appDatabase.applicationInfoDao().deleteByPackageName(packageName);
                    AppInfo appInfo = AppsListDBInitializer.getInstance().generateAppInfo(packageName);
                    appDatabase.applicationInfoDao().insert(appInfo);
                } else if (action.equalsIgnoreCase("android.intent.action.PACKAGE_REMOVED") && !isReplacing) {
                    appDatabase.applicationInfoDao().deleteByPackageName(packageName);
                } else if (action.equalsIgnoreCase("android.intent.action.PACKAGE_REPLACED")) {
                    AppInfo oldAppInfo = appDatabase.applicationInfoDao().getAppByPackageName(packageName);
                    AppInfo appInfo = AppsListDBInitializer.getInstance().generateAppInfo(packageName);
                    if (oldAppInfo != null && oldAppInfo.packageName.equals(packageName)) {
                        appInfo.disabled = oldAppInfo.disabled;
                        appInfo.mobileRestricted = oldAppInfo.mobileRestricted;
                        appInfo.wifiRestricted = oldAppInfo.wifiRestricted;
                        appInfo.adhellWhitelisted = oldAppInfo.adhellWhitelisted;
                    }
                    appDatabase.applicationInfoDao().deleteByPackageName(packageName);
                    appDatabase.applicationInfoDao().insert(appInfo);
                }
            }
        });
    }
}
