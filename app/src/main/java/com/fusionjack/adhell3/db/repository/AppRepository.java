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
