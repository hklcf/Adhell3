package com.fusionjack.adhell3.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static com.fusionjack.adhell3.db.DatabaseFactory.MOBILE_RESTRICTED_TYPE;
import static com.fusionjack.adhell3.db.DatabaseFactory.WIFI_RESTRICTED_TYPE;

public class AppCache {
    private static AppCache instance;
    private Map<String, Drawable> appsIcons;
    private Map<String, String> appsNames;
    private Map<String, String> versionNames;

    private AppCache(Context context, Handler handler) {
        this.appsIcons = new HashMap<>();
        this.appsNames = new HashMap<>();
        this.versionNames = new HashMap<>();
        loadApps(context, handler);
    }

    private void loadApps(Context context, Handler handler) {
        new AppCacheAsyncTask(context, handler, appsIcons, appsNames, versionNames).execute();
    }

    public static synchronized AppCache getInstance(Context context, Handler handler) {
        if (instance == null) {
            instance = new AppCache(context, handler);
        }
        return instance;
    }

    public static synchronized AppCache reload(Context context, Handler handler) {
        instance = null;
        instance = new AppCache(context, handler);
        return instance;
    }

    public Map<String, Drawable> getIcons() {
        return appsIcons;
    }

    public Map<String, String> getNames() {
        return appsNames;
    }

    public Map<String, String> getVersionNames() {
        return versionNames;
    }

    private static class AppCacheAsyncTask extends AsyncTask<Void, Void, Throwable> {
        private ProgressDialog dialog;
        private Map<String, Drawable> appsIcons;
        private Map<String, String> appsNames;
        private Map<String, String> versionNames;
        private Handler handler;
        private WeakReference<Context> contextWeakReference;

        AppCacheAsyncTask(Context context, Handler handler, Map<String, Drawable> appsIcons,
                          Map<String, String> appsNames, Map<String, String> versionNames) {
            this.appsIcons = appsIcons;
            this.appsNames = appsNames;
            this.versionNames = versionNames;
            this.handler = handler;
            this.contextWeakReference = new WeakReference<>(context);

            if (context != null) {
                dialog = new ProgressDialog(context);
                dialog.setCancelable(false);
            }
        }

        @Override
        protected void onPreExecute() {
            if (dialog != null) {
                dialog.setMessage("Caching apps, please wait...");
                dialog.show();
            }
        }

        @Override
        protected Throwable doInBackground(Void... args) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<AppInfo> modifiedApps = null;

            try {
                PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
                List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                int appCount = apps.size();
                int cpuCount = Runtime.getRuntime().availableProcessors() / 2;
                ExecutorService executorService = Executors.newFixedThreadPool(cpuCount);
                List<FutureTask<AppInfoResult>> tasks = new ArrayList<>();

                modifiedApps = appDatabase.applicationInfoDao().getModifiedApps();
                appDatabase.applicationInfoDao().deleteAll();

                int distributedAppCount = (int) Math.ceil(appCount / (double) cpuCount);
                List<List<ApplicationInfo>> chunks = Lists.partition(apps, distributedAppCount);
                for (List<ApplicationInfo> chunk : chunks) {
                    long id = distributedAppCount * tasks.size();
                    AppExecutor appExecutor = new AppExecutor(chunk, id);
                    FutureTask<AppInfoResult> task = new FutureTask<>(appExecutor);
                    tasks.add(task);
                    executorService.execute(task);
                }

                for (FutureTask<AppInfoResult> task : tasks) {
                    AppInfoResult result = task.get();
                    appsIcons.putAll(result.getAppsIcons());
                    appsNames.putAll(result.getAppsNames());
                    versionNames.putAll(result.getVersionNames());
                }

                executorService.shutdown();
            } catch (Throwable th) {
                // Something went wrong, put the modified apps back to db
                // The app list needs to be refreshed manually to trigger the caching again
                th.printStackTrace();
                if (modifiedApps != null) {
                    appDatabase.applicationInfoDao().insertAll(modifiedApps);
                }
                return th;
            }

            if (modifiedApps != null && modifiedApps.size() > 0 && appDatabase.applicationInfoDao().getAppSize() > 0) {
                appDatabase.firewallWhitelistedPackageDao().deleteAll();
                appDatabase.disabledPackageDao().deleteAll();
                appDatabase.restrictedPackageDao().deleteAll();
                appDatabase.dnsPackageDao().deleteAll();

                for (AppInfo modifiedApp : modifiedApps) {
                    AppInfo appInfo = appDatabase.applicationInfoDao().getAppByPackageName(modifiedApp.packageName);
                    if (appInfo != null) {
                        if (modifiedApp.adhellWhitelisted) {
                            appInfo.adhellWhitelisted = true;
                            FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                            whitelistedPackage.packageName = modifiedApp.packageName;
                            whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.firewallWhitelistedPackageDao().insert(whitelistedPackage);
                        }
                        if (modifiedApp.disabled) {
                            appInfo.disabled = true;
                            DisabledPackage disabledPackage = new DisabledPackage();
                            disabledPackage.packageName = modifiedApp.packageName;
                            disabledPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.disabledPackageDao().insert(disabledPackage);
                        }
                        if (modifiedApp.mobileRestricted) {
                            appInfo.mobileRestricted = true;
                            RestrictedPackage restrictedPackage = new RestrictedPackage();
                            restrictedPackage.packageName = modifiedApp.packageName;
                            restrictedPackage.type = MOBILE_RESTRICTED_TYPE;
                            restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.restrictedPackageDao().insert(restrictedPackage);
                        }
                        if (modifiedApp.wifiRestricted) {
                            appInfo.wifiRestricted = true;
                            RestrictedPackage restrictedPackage = new RestrictedPackage();
                            restrictedPackage.packageName = modifiedApp.packageName;
                            restrictedPackage.type = WIFI_RESTRICTED_TYPE;
                            restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.restrictedPackageDao().insert(restrictedPackage);
                        }
                        if (modifiedApp.hasCustomDns) {
                            appInfo.hasCustomDns = true;
                            DnsPackage dnsPackage = new DnsPackage();
                            dnsPackage.packageName = modifiedApp.packageName;
                            dnsPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.dnsPackageDao().insert(dnsPackage);
                        }
                        appDatabase.applicationInfoDao().update(appInfo);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Throwable th) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (handler != null) {
                handler.obtainMessage().sendToTarget();
            }

            Context context = contextWeakReference.get();
            if (th != null && context != null) {
                new AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong when caching apps, please refresh the app list. Error: \n\n" + th.getMessage())
                        .show();
            }
        }
    }

    private static class AppExecutor implements Callable<AppInfoResult> {
        private List<ApplicationInfo> apps;
        private long appId;

        AppExecutor(List<ApplicationInfo> apps, long appId) {
            this.apps = apps;
            this.appId = appId;
        }

        @Override
        public AppInfoResult call() throws Exception {
            String ownPackageName = App.get().getApplicationContext().getPackageName();
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            AppInfoResult appInfoResult = new AppInfoResult();

            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<AppInfo> appsInfo = new ArrayList<>();
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

            for (ApplicationInfo app : apps) {
                if (app.packageName.equals(ownPackageName)) {
                    continue;
                }

                Drawable icon;
                try {
                    icon = packageManager.getApplicationIcon(app.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
                appInfoResult.putAppIcon(app.packageName, icon);

                String appName = packageManager.getApplicationLabel(app).toString();
                appInfoResult.putAppName(app.packageName, appName);

                AppInfo appInfo = new AppInfo();
                appInfo.id = appId++;
                appInfo.appName = appName;
                appInfo.packageName = app.packageName;
                appInfo.system = (app.flags & mask) != 0;
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(app.packageName, 0);
                    appInfo.installTime = packageInfo.firstInstallTime;
                    appInfoResult.putVersionName(app.packageName, packageInfo.versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    appInfo.installTime = 0;
                }
                appsInfo.add(appInfo);
            }
            appDatabase.applicationInfoDao().insertAll(appsInfo);

            return appInfoResult;
        }
    }

    private static class AppInfoResult {
        private Map<String, Drawable> appsIcons;
        private Map<String, String> appsNames;
        private Map<String, String> versionNames;

        AppInfoResult() {
            this.appsIcons = new HashMap<>();
            this.appsNames = new HashMap<>();
            this.versionNames = new HashMap<>();
        }

        public void putAppIcon(String packageName, Drawable icon) {
            appsIcons.put(packageName, icon);
        }

        public void putAppName(String packageName, String appName) {
            appsNames.put(packageName, appName);
        }

        public void putVersionName(String packageName, String versionName) {
            versionNames.put(packageName, versionName);
        }

        public Map<String, Drawable> getAppsIcons() {
            return appsIcons;
        }

        public Map<String, String> getAppsNames() {
            return appsNames;
        }

        public Map<String, String> getVersionNames() {
            return versionNames;
        }
    }
}
