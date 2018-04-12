package com.fusionjack.adhell3.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AppsListDBInitializer;

import java.util.List;

public class ApplicationsListChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AsyncTask.execute(() ->
        {
            AppDatabase appDatabase = AppDatabase.getAppDatabase(App.get().getApplicationContext());
            List<AppInfo> packageList = appDatabase.applicationInfoDao().getAppsAlphabetically();
            if (packageList.size() == 0) {
                return;
            }

            Uri data = intent.getData();
            String packageName = "";
            if (data != null) {
                packageName = data.getEncodedSchemeSpecificPart();
            }

            String action = intent.getAction();
            if (action != null && !packageName.isEmpty()) {
                if (action.equalsIgnoreCase("android.intent.action.PACKAGE_ADDED")) {
                    appDatabase.applicationInfoDao().deleteByPackageName(packageName);
                    appDatabase.applicationInfoDao().insert(AppsListDBInitializer.getInstance()
                            .generateAppInfo(context.getPackageManager(), packageName));
                } else if (action.equalsIgnoreCase("android.intent.action.PACKAGE_REMOVED")) {
                    appDatabase.applicationInfoDao().deleteByPackageName(packageName);
                }
            }
        });
    }
}
