package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ListView;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadAppAsyncTask extends AsyncTask<Void, Void, List<AppInfo>> {

    private WeakReference<Context> contextReference;
    private String text;
    private AppFlag appFlag;
    private boolean reload;

    public LoadAppAsyncTask(String text, AppFlag appFlag, Context context) {
        this(text, appFlag, false, context);
    }

    LoadAppAsyncTask(String text, AppFlag appFlag, boolean reload, Context context) {
        this.text = text;
        this.appFlag = appFlag;
        this.reload = reload;
        this.contextReference = new WeakReference<>(context);
    }

    @Override
    protected List<AppInfo> doInBackground(Void... voids) {
        return getListFromDb();
    }

    @Override
    protected void onPostExecute(List<AppInfo> packageList) {
        Context context = contextReference.get();
        if (context != null) {
            ListView listView = ((Activity)context).findViewById(appFlag.getLoadLayout());
            if (listView != null) {
                AppInfoAdapter adapter = new AppInfoAdapter(packageList, appFlag.getType(), reload, context);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private List<AppInfo> getListFromDb() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String filterText = '%' + text + '%';
        switch (appFlag.getType()) {
            case DISABLER:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInDisabledOrder(filterText);
            case MOBILE_RESTRICTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder(filterText);
            case WIFI_RESTRICTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder(filterText);
            case WHITELISTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInWhitelistedOrder(filterText);
            case COMPONENT:
                boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                if (text.length() == 0) {
                    return showSystemApps ?
                            appDatabase.applicationInfoDao().getEnabledAppsAlphabetically() :
                            appDatabase.applicationInfoDao().getUserApps();
                }
                return showSystemApps ?
                        appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText) :
                        appDatabase.applicationInfoDao().getUserApps(filterText);
            case DNS:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInDnsOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInDnsOrder(filterText);
        }
        return null;
    }
}
