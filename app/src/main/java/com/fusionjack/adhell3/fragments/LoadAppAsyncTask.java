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

    private WeakReference<Context> contextReference;
    private String text;
    private int sortState;
    private int layout;
    private AppFlag appFlag;

    LoadAppAsyncTask(String text, int sortState, int layout, AppFlag appFlag, Context context) {
        this.text = text;
        this.sortState = sortState;
        this.layout = layout;
        this.appFlag = appFlag;
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
            AppInfoAdapter adapter = new AppInfoAdapter(packageList, appFlag, context);
            ListView listView = ((Activity)context).findViewById(layout);
            listView.setAdapter(adapter);
            listView.invalidateViews();
        }
    }

    private List<AppInfo> getListFromDb() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String filterText = '%' + text + '%';
        switch (sortState) {
            case SORTED_DISABLED_ALPHABETICALLY:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAll();
                }
                return appDatabase.applicationInfoDao().getAllAppsWithStrInName(filterText);
            case SORTED_DISABLED_INSTALL_TIME:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAllRecentSort();
                }
                return appDatabase.applicationInfoDao().getAllAppsWithStrInNameTimeOrder(filterText);
            case SORTED_DISABLED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getAllSortedByDisabled();
                }
                return appDatabase.applicationInfoDao().getAllAppsWithStrInNameDisabledOrder(filterText);
            case SORTED_RESTRICTED_ALPHABETICALLY:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getEnabledApps();
                }
                return appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText);
            case SORTED_RESTRICTED_INSTALL_TIME:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getEnabledAppsInTimeOrder();
                }
                return appDatabase.applicationInfoDao().getEnabledAppsInTimeOrder(filterText);
            case SORTED_RESTRICTED:
                if (text.length() == 0) {
                    return appDatabase.applicationInfoDao().getEnableAppsByMobileRestricted();
                }
                return appDatabase.applicationInfoDao().getEnableAppsByMobileRestricted(filterText);
        }
        return null;
    }
}
