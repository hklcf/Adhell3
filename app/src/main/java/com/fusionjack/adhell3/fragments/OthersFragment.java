package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.OthersFragmentPagerAdapter;

public class OthersFragment extends Fragment {

    private int[] imageResId = {
            R.drawable.ic_security_black_24dp,
            R.drawable.ic_dns_black_24dp,
            R.drawable.ic_settings_black_24dp
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        viewPager.setAdapter(new OthersFragmentPagerAdapter(getChildFragmentManager(), getContext()));
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < imageResId.length; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setIcon(imageResId[i]);
            }
        }

        return view;
    }
}
