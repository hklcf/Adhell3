package com.fusionjack.adhell3.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.ComponentTabPageFragment;

public class ComponentPagerAdapter extends FragmentPagerAdapter {
    private static final int PAGE_COUNT = 3;
    private String tabTitles[];
    private String packageName;

    public ComponentPagerAdapter(FragmentManager fm, Context context, String packageName) {
        super(fm);
        tabTitles = new String[] {
                context.getString(R.string.permission_fragment_title),
                context.getString(R.string.service_fragment_title),
                context.getString(R.string.receiver_fragment_title)};

        this.packageName = packageName;
    }

    @Override
    public Fragment getItem(int position) {
        return ComponentTabPageFragment.newInstance(position, packageName);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
