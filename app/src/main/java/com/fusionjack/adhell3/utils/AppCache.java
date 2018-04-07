package com.fusionjack.adhell3.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;

import com.fusionjack.adhell3.App;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                String pckg = App.get().getApplicationContext().getPackageName();
                PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
                List<ApplicationInfo> applicationsInfo = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo applicationInfo : applicationsInfo) {
                    if (applicationInfo.packageName.equals(pckg)) {
                        continue;
                    }

                    Drawable icon;
                    try {
                        icon = packageManager.getApplicationIcon(applicationInfo.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                    }
                    appsIcons.put(applicationInfo.packageName, icon);

                    String appName = packageManager.getApplicationLabel(applicationInfo).toString();
                    appsNames.put(applicationInfo.packageName, appName);
                }
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
}
