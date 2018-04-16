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
import com.fusionjack.adhell3.adapter.AppsFragmentPagerAdapter;

public class AppsFragment extends Fragment {

    private int[] imageResId = {
            R.drawable.ic_visibility_off_black_24dp,
            R.drawable.ic_signal_cellular_off_black_24dp,
            R.drawable.ic_beenhere_black_24dp
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Apps Management");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
        }
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_apps, container, false);

        TabLayout tabLayout = view.findViewById(R.id.apps_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setAdapter(new AppsFragmentPagerAdapter(getChildFragmentManager(), getContext()));
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < imageResId.length; i++) {
            tabLayout.getTabAt(i).setIcon(imageResId[i]);
        }
        return view;
    }
}
