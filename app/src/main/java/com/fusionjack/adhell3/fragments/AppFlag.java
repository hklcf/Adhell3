package com.fusionjack.adhell3.fragments;

import com.fusionjack.adhell3.R;

import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_DISABLED;
import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_RESTRICTED;
import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_WHITELISTED;

public class AppFlag {

    public enum Flag {
        DISABLER_FLAG,
        RESTRICTED_FLAG,
        WHITELISTED_FLAG
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

    public static AppFlag createRestrictedFlag() {
        int loadLayout = R.id.enabled_apps_list;
        int refreshLayout = R.id.restrictedSwipeContainer;
        return new AppFlag(Flag.RESTRICTED_FLAG, SORTED_RESTRICTED, loadLayout, refreshLayout);
    }

    public static AppFlag createWhitelistedFlag() {
        int loadLayout = R.id.whitelisted_apps_list;
        int refreshLayout = R.id.whitelistedSwipeContainer;
        return new AppFlag(Flag.WHITELISTED_FLAG, SORTED_WHITELISTED, loadLayout, refreshLayout);
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
