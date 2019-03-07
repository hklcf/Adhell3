package com.fusionjack.adhell3.model;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.repository.AppRepository;

public class AppFlag {

    private AppRepository.Type type;
    private int loadLayout;
    private int refreshLayout;

    private AppFlag(AppRepository.Type type, int loadLayout, int refreshLayout) {
        this.type = type;
        this.loadLayout = loadLayout;
        this.refreshLayout = refreshLayout;
    }

    public static AppFlag createDisablerFlag() {
        int loadLayout = R.id.installed_apps_list;
        int refreshLayout = R.id.disablerSwipeContainer;
        return new AppFlag(AppRepository.Type.DISABLER, loadLayout, refreshLayout);
    }

    public static AppFlag createMobileRestrictedFlag() {
        int loadLayout = R.id.mobile_apps_list;
        int refreshLayout = R.id.mobileSwipeContainer;
        return new AppFlag(AppRepository.Type.MOBILE_RESTRICTED, loadLayout, refreshLayout);
    }

    public static AppFlag createWifiRestrictedFlag() {
        int loadLayout = R.id.wifi_apps_list;
        int refreshLayout = R.id.wifiSwipeContainer;
        return new AppFlag(AppRepository.Type.WIFI_RESTRICTED, loadLayout, refreshLayout);
    }

    public static AppFlag createWhitelistedFlag() {
        int loadLayout = R.id.whitelisted_apps_list;
        int refreshLayout = R.id.whitelistedSwipeContainer;
        return new AppFlag(AppRepository.Type.WHITELISTED, loadLayout, refreshLayout);
    }

    public static AppFlag createComponentFlag() {
        int loadLayout = R.id.component_apps_list;
        int refreshLayout = R.id.componentSwipeContainer;
        return new AppFlag(AppRepository.Type.COMPONENT, loadLayout, refreshLayout);
    }

    public static AppFlag createDnsFlag() {
        int loadLayout = R.id.dns_apps_list;
        int refreshLayout = R.id.dnsSwipeContainer;
        return new AppFlag(AppRepository.Type.DNS, loadLayout, refreshLayout);
    }

    public AppRepository.Type getType() {
        return type;
    }

    public int getLoadLayout() {
        return loadLayout;
    }

    public int getRefreshLayout() {
        return refreshLayout;
    }
}
