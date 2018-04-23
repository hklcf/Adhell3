package com.fusionjack.adhell3.model;

import com.fusionjack.adhell3.R;

import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_COMPONENT;
import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_DISABLED;
import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_MOBILE_RESTRICTED;
import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_WHITELISTED;
import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_WIFI_RESTRICTED;
import static com.fusionjack.adhell3.tasks.LoadAppAsyncTask.SORTED_DNS;

public class AppFlag {

    public enum Flag {
        DISABLER_FLAG,
        MOBILE_RESTRICTED_FLAG,
        WIFI_RESTRICTED_FLAG,
        WHITELISTED_FLAG,
        COMPONENT_FLAG,
        DNS_FLAG
    }

    private Flag flag;
    private int sortState;
    private int loadLayout;
    private int refreshLayout;

    private AppFlag(Flag flag, int sortState, int loadLayout, int refreshLayout) {
        this.flag = flag;
        this.sortState = sortState;
        this.loadLayout = loadLayout;
        this.refreshLayout = refreshLayout;
    }

    public static AppFlag createDisablerFlag() {
        int loadLayout = R.id.installed_apps_list;
        int refreshLayout = R.id.disablerSwipeContainer;
        return new AppFlag(Flag.DISABLER_FLAG, SORTED_DISABLED, loadLayout, refreshLayout);
    }

    public static AppFlag createMobileRestrictedFlag() {
        int loadLayout = R.id.mobile_apps_list;
        int refreshLayout = R.id.mobileSwipeContainer;
        return new AppFlag(Flag.MOBILE_RESTRICTED_FLAG, SORTED_MOBILE_RESTRICTED, loadLayout, refreshLayout);
    }

    public static AppFlag createWifiRestrictedFlag() {
        int loadLayout = R.id.wifi_apps_list;
        int refreshLayout = R.id.wifiSwipeContainer;
        return new AppFlag(Flag.WIFI_RESTRICTED_FLAG, SORTED_WIFI_RESTRICTED, loadLayout, refreshLayout);
    }

    public static AppFlag createWhitelistedFlag() {
        int loadLayout = R.id.whitelisted_apps_list;
        int refreshLayout = R.id.whitelistedSwipeContainer;
        return new AppFlag(Flag.WHITELISTED_FLAG, SORTED_WHITELISTED, loadLayout, refreshLayout);
    }

    public static AppFlag createComponentFlag() {
        int loadLayout = R.id.component_apps_list;
        int refreshLayout = R.id.componentSwipeContainer;
        return new AppFlag(Flag.COMPONENT_FLAG, SORTED_COMPONENT, loadLayout, refreshLayout);
    }

    public static AppFlag createDnsFlag() {
        int loadLayout = R.id.dns_apps_list;
        int refreshLayout = R.id.dnsSwipeContainer;
        return new AppFlag(Flag.DNS_FLAG, SORTED_DNS, loadLayout, refreshLayout);
    }

    public Flag getFlag() {
        return flag;
    }

    public int getSortState() {
        return sortState;
    }

    public int getLoadLayout() {
        return loadLayout;
    }

    public int getRefreshLayout() {
        return refreshLayout;
    }
}
