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
import com.fusionjack.adhell3.adapter.ComponentPagerAdapter;

public class ComponentTabFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String packageName = "";
        String appName = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            packageName = bundle.getString("packageName");
            appName = bundle.getString("appName");
        }

        getActivity().setTitle(appName.isEmpty() ? "App Component" : appName);
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(true);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }

        View view = inflater.inflate(R.layout.fragment_app_component_tabs, container, false);

        TabLayout tabLayout = view.findViewById(R.id.apps_sliding_tabs);
        ViewPager viewPager = view.findViewById(R.id.apps_viewpager);
        viewPager.setAdapter(new ComponentPagerAdapter(getChildFragmentManager(), getContext(), packageName));
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}
