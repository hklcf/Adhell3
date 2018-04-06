package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AdhellPermissionInAppsAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import java.util.List;

import javax.inject.Inject;

public class AdhellPermissionInAppsFragment extends Fragment {
    private RecyclerView permissionInAppsRecyclerView;
    private AppCompatActivity parentActivity;
    private AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get().getAppComponent().inject(this);
        appDatabase = AppDatabase.getAppDatabase(getContext());
        parentActivity = (AppCompatActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(true);
        }
        SharedAppPermissionViewModel sharedAppPermissionViewModel = ViewModelProviders.of(getActivity()).get(SharedAppPermissionViewModel.class);
        View view = inflater.inflate(R.layout.fragment_permission_in_apps, container, false);
        permissionInAppsRecyclerView = view.findViewById(R.id.permissionInAppsRecyclerView);
        permissionInAppsRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL);
        permissionInAppsRecyclerView.addItemDecoration(itemDecoration);

        sharedAppPermissionViewModel.getSelected().observe(this, permissionInfo -> {
            if (permissionInfo != null) {
                getActivity().setTitle(permissionInfo.name);
                List<AppInfo> appInfos = AdhellPermissionInfo.getAppsByPermission(permissionInfo.name, appDatabase, packageManager);
                AdhellPermissionInAppsAdapter adhellPermissionInAppsAdapter = new AdhellPermissionInAppsAdapter(appInfos, appDatabase);
                adhellPermissionInAppsAdapter.currentPermissionName = permissionInfo.name;
                adhellPermissionInAppsAdapter.updatePermissionBlacklistedPackages();
                permissionInAppsRecyclerView.setAdapter(adhellPermissionInAppsAdapter);
                adhellPermissionInAppsAdapter.notifyDataSetChanged();
            }
        });
        return view;
    }
}
