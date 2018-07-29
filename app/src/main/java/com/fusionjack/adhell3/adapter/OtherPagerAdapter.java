package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.OtherTabPageFragment;

public class OtherPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 3;
    private String tabTitles[];

    public OtherPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabTitles = new String[] {
                context.getString(R.string.app_component_fragment_title),
                context.getString(R.string.dns_fragment_title),
                context.getString(R.string.settings_fragment_title)
        };
    }

    @Override
    public Fragment getItem(int position) {
        return OtherTabPageFragment.newInstance(BuildConfig.APP_COMPONENT ? position : position + 1);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return BuildConfig.APP_COMPONENT ? PAGE_COUNT : PAGE_COUNT - 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[BuildConfig.APP_COMPONENT ? position : position + 1];
    }
}
