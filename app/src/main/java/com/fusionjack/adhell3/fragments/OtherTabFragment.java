package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.OtherPagerAdapter;

import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.APP_COMPONENT_PAGE;
import static com.fusionjack.adhell3.fragments.OtherTabPageFragment.SETTINGS_PAGE;

public class OtherTabFragment extends Fragment {

    private int[] imageResId = {
            R.drawable.ic_security_black_24dp,
            R.drawable.ic_dns_black_24dp,
            R.drawable.ic_settings_black_24dp
    };
    private String viewpagerPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null){
            viewpagerPosition = getArguments().getString("viewpager_position");
        }
        getActivity().setTitle("Others");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_others, container, false);

        TabLayout tabLayout = view.findViewById(R.id.others_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.others_viewpager);
        viewPager.setAdapter(new OtherPagerAdapter(getChildFragmentManager(), getContext()));
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorText);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                        int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
                        tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
                    }
                }
        );

        int imageIndex = BuildConfig.APP_COMPONENT ? 0 : 1;
        int tabCount = viewPager.getAdapter().getCount();
        for (int i = 0; i < tabCount; i++, imageIndex++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setIcon(imageResId[i]);
                int tabIconColor = ContextCompat.getColor(getContext(), R.color.colorBottomNavUnselected);
                tab.getIcon().setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN);
            }
        }

        if(viewpagerPosition == "Settings"){
            TabLayout.Tab tab = tabLayout.getTabAt(SETTINGS_PAGE);
            if (tab != null) {
                tab.select();
            }
        }
        else {
            TabLayout.Tab tab = tabLayout.getTabAt(APP_COMPONENT_PAGE);
            if (tab != null) {
                tab.select();
            }
        }

        return view;
    }
}
