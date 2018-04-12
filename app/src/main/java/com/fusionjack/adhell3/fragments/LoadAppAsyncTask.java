package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ListView;

import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadAppAsyncTask extends AsyncTask<Void, Void, List<AppInfo>> {

    static final int SORTED_DISABLED_ALPHABETICALLY = 0;
    static final int SORTED_DISABLED_INSTALL_TIME = 1;
    static final int SORTED_DISABLED = 2;
    static final int SORTED_RESTRICTED_ALPHABETICALLY = 3;
    static final int SORTED_RESTRICTED_INSTALL_TIME = 4;
    static final int SORTED_RESTRICTED = 5;
    static final int SORTED_WHITELISTED = 6;

    private WeakReference<Context> contextReference;
    private String text;
    private AppFlag appFlag;
    private boolean reload;

    LoadAppAsyncTask(String text, AppFlag appFlag, Context context) {
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
            AppInfoAdapter adapter = new AppInfoAdapter(packageList, appFlag, reload, context);
            ListView listView = ((Activity)context).findViewById(appFlag.getLoadLayout());
            if (listView != null) {
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private List<AppInfo> getListFromDb() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String filterText = '%' + text + '%';
        switch (appFlag.getSortState()) {
            case SORTED_DISABLED_ALPHABETICALLY:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsAlphabetically();
                }
                return appDatabase.applicationInfoDao().getAppsAlphabetically(filterText);
            case SORTED_DISABLED_INSTALL_TIME:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInTimeOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInTimeOrder(filterText);
            case SORTED_DISABLED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInDisabledOrder(filterText);
            case SORTED_RESTRICTED_ALPHABETICALLY:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getEnabledAppsAlphabetically();
                }
                return appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText);
            case SORTED_RESTRICTED_INSTALL_TIME:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getEnabledAppsInTimeOrder();
                }
                return appDatabase.applicationInfoDao().getEnabledAppsInTimeOrder(filterText);
            case SORTED_RESTRICTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder(filterText);
            case SORTED_WHITELISTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                }
                return appDatabase.applicationInfoDao().getAppsInWhitelistedOrder(filterText);
        }
        return null;
    }
}
