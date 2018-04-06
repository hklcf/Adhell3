package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppsListDBInitializer {
    private static AppsListDBInitializer instance;
    private AppDatabase appDatabase;

    private AppsListDBInitializer() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static AppsListDBInitializer getInstance() {
        if (instance == null) {
            instance = new AppsListDBInitializer();
        }
        return instance;
    }

    public void fillPackageDb(PackageManager packageManager) {
        List<AppInfo> appsInfo = new ArrayList<>();
        long id = 0;
        String pckg = App.get().getApplicationContext().getPackageName();
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

        List<ApplicationInfo> applicationsInfo = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : applicationsInfo) {
            if (applicationInfo.packageName.equals(pckg)) {
                continue;
            }

            AppInfo appInfo = new AppInfo();
            appInfo.id = id++;
            appInfo.appName = packageManager.getApplicationLabel(applicationInfo).toString();
            appInfo.packageName = applicationInfo.packageName;
            appInfo.system = (applicationInfo.flags & mask) != 0;
            try {
                appInfo.installTime = packageManager.getPackageInfo(applicationInfo.packageName, 0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                appInfo.installTime = 0;
            }
            appsInfo.add(appInfo);
        }
        appDatabase.applicationInfoDao().insertAll(appsInfo);
    }

    public AppInfo generateAppInfo(PackageManager packageManager, String packageName) {
        AppInfo appInfo = new AppInfo();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            appInfo.id = appDatabase.applicationInfoDao().getMaxId() + 1;
            appInfo.packageName = packageName;
            appInfo.appName = packageManager.getApplicationLabel(applicationInfo).toString();
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            appInfo.system = (applicationInfo.flags & mask) != 0;
            appInfo.installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appInfo;
    }
}
