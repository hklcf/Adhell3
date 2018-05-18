package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class OtherTabPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";

    public static final int APP_COMPONENT_PAGE = 0;
    public static final int DNS_PAGE = 1;
    public static final int SETTINGS_PAGE = 2;

    public static Fragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);

        Fragment fragment;
        switch (page) {
            case APP_COMPONENT_PAGE:
                fragment = new AppComponentFragment();
                break;
            case DNS_PAGE:
                fragment = new DnsFragment();
                break;
            case SETTINGS_PAGE:
                default:
                fragment = new SettingsFragment();
                break;
        }
        fragment.setArguments(args);

        return fragment;
    }
}
