package com.fusionjack.adhell3.db.repository;

import android.arch.lifecycle.LiveData;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

public class AppRepository {

    private String text;
    private AppFlag appFlag;

    private static final int SORTED_COMPONENT = 4;

    public LiveData<List<AppInfo>> getAppList(String text, AppFlag appFlag) {
        this.appFlag = appFlag;
        this.text = text;
        return getListFromDb();
    }

    private LiveData<List<AppInfo>> getListFromDb() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String filterText = '%' + text + '%';
        switch (appFlag.getSortState()) {
            case SORTED_COMPONENT:
                boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                if (text.length() == 0) {
                    return showSystemApps ?
                            appDatabase.applicationInfoDao().getLiveEnabledAppsAlphabetically() :
                            appDatabase.applicationInfoDao().getLiveUserApps();
                }
                return showSystemApps ?
                        appDatabase.applicationInfoDao().getLiveEnabledAppsAlphabetically(filterText) :
                        appDatabase.applicationInfoDao().getLiveUserApps(filterText);
        }
        return null;
    }
}
