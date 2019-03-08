package com.fusionjack.adhell3.db.repository;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class AppRepository {

    public enum Type {
        DISABLER,
        MOBILE_RESTRICTED,
        WIFI_RESTRICTED,
        WHITELISTED,
        COMPONENT,
        DNS
    }

    public Single<List<AppInfo>> loadAppList(String text, Type type) {
        return Single.create(emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            String filterText = '%' + text + '%';
            List<AppInfo> list = new ArrayList<>();
            switch (type) {
                case DISABLER:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInDisabledOrder(filterText);
                    break;
                case MOBILE_RESTRICTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder(filterText);
                    break;
                case WIFI_RESTRICTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder(filterText);
                    break;
                case WHITELISTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder(filterText);
                    break;
                case COMPONENT:
                    boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                    if (text.length() == 0) {
                        list = showSystemApps ?
                                appDatabase.applicationInfoDao().getEnabledAppsAlphabetically() :
                                appDatabase.applicationInfoDao().getUserApps();
                    }
                    list = showSystemApps ?
                            appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText) :
                            appDatabase.applicationInfoDao().getUserApps(filterText);
                    break;
                case DNS:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInDnsOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInDnsOrder(filterText);
                    break;
            }
            emitter.onSuccess(list);
        });
    }
}
