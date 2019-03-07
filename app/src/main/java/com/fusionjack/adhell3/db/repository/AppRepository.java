package com.fusionjack.adhell3.db.repository;

import android.arch.lifecycle.LiveData;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

public class AppRepository {

    public enum Type {
        DISABLER,
        MOBILE_RESTRICTED,
        WIFI_RESTRICTED,
        WHITELISTED,
        COMPONENT,
        DNS
    }

    public LiveData<List<AppInfo>> getAppList(String text, Type type) {
        return getListFromDb(text, type);
    }

    private LiveData<List<AppInfo>> getListFromDb(String text, Type type) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        String filterText = '%' + text + '%';
        switch (type) {
            case COMPONENT:
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
