package com.fusionjack.adhell3.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;

import com.fusionjack.adhell3.App;
import com.google.common.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class AppCache {
    private static AppCache instance;
    private Map<String, Drawable> appsIcons;
    private Map<String, String> appsNames;

    private AppCache(Context context, Handler handler) {
        this.appsIcons = new HashMap<>();
        this.appsNames = new HashMap<>();
        loadApps(context, handler);
    }

    private void loadApps(Context context, Handler handler) {
        new AppCacheAsyncTask(context, handler, appsIcons, appsNames).execute();
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

    private static class AppCacheAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        private WeakReference<Context> contextReference;
        private Map<String, Drawable> appsIcons;
        private Map<String, String> appsNames;
        private Handler handler;

        AppCacheAsyncTask(Context context, Handler handler, Map<String, Drawable> appsIcons, Map<String, String> appsNames) {
            this.contextReference = new WeakReference<>(context);
            this.appsIcons = appsIcons;
            this.appsNames = appsNames;
            this.handler = handler;
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Caching apps, please wait...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... args) {
            Context context = contextReference.get();
            if (context != null) {
                PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
                List<ApplicationInfo> appInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                int appCount = appInfos.size();
                int cpuCount = Runtime.getRuntime().availableProcessors() / 2;
                ExecutorService executorService = Executors.newFixedThreadPool(cpuCount);
                List<FutureTask<AppInfoResult>> tasks = new ArrayList<>();

                int distributedAppCount = (int) Math.ceil(appCount / (double) cpuCount);
                List<List<ApplicationInfo>> chunks = Lists.partition(appInfos, distributedAppCount);
                for (List<ApplicationInfo> chunk : chunks) {
                    FutureTask<AppInfoResult> task = new FutureTask<>(new AppExecutor(chunk));
                    tasks.add(task);
                    executorService.execute(task);
                }

                for (FutureTask<AppInfoResult> task : tasks) {
                    try {
                        AppInfoResult result = task.get();
                        appsIcons.putAll(result.getAppsIcons());
                        appsNames.putAll(result.getAppsNames());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                executorService.shutdown();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (handler != null) {
                handler.obtainMessage().sendToTarget();
            }
        }
    }

    private static class AppExecutor implements Callable<AppInfoResult> {
        private List<ApplicationInfo> appInfos;

        AppExecutor(List<ApplicationInfo> appInfos) {
            this.appInfos = appInfos;
        }

        @Override
        public AppInfoResult call() throws Exception {
            String pckg = App.get().getApplicationContext().getPackageName();
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            AppInfoResult appInfoResult = new AppInfoResult();

            for (ApplicationInfo applicationInfo : appInfos) {
                if (applicationInfo.packageName.equals(pckg)) {
                    continue;
                }

                Drawable icon;
                try {
                    icon = packageManager.getApplicationIcon(applicationInfo.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
                appInfoResult.putAppIcon(applicationInfo.packageName, icon);

                String appName = packageManager.getApplicationLabel(applicationInfo).toString();
                appInfoResult.putAppName(applicationInfo.packageName, appName);
            }

            return appInfoResult;
        }
    }

    private static class AppInfoResult {
        private Map<String, Drawable> appsIcons;
        private Map<String, String> appsNames;

        AppInfoResult() {
            this.appsIcons = new HashMap<>();
            this.appsNames = new HashMap<>();
        }

        public void putAppIcon(String packageName, Drawable icon) {
            appsIcons.put(packageName, icon);
        }

        public void putAppName(String packageName, String appName) {
            appsNames.put(packageName, appName);
        }

        public Map<String, Drawable> getAppsIcons() {
            return appsIcons;
        }

        public Map<String, String> getAppsNames() {
            return appsNames;
        }
    }
}
