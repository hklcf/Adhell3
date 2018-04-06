package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AdhellPermissionInfoAdapter;
import com.fusionjack.adhell3.adapter.ItemClickSupport;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import java.util.List;

import javax.inject.Inject;

public class AdhellPermissionInfoFragment extends Fragment {
    private static final String TAG = AdhellPermissionInfoFragment.class.getCanonicalName();
    private List<AdhellPermissionInfo> adhellPermissionInfos;
    private AppCompatActivity parentActivity;
    private SharedAppPermissionViewModel sharedAppPermissionViewModel;
    private FragmentManager fragmentManager;

    public AdhellPermissionInfoFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentActivity = (AppCompatActivity) getActivity();
        boolean isPermissionGranted = (this.getContext()
                .checkCallingOrSelfPermission("android.permission.sec.MDM_APP_PERMISSION_MGMT")
                == PackageManager.PERMISSION_GRANTED);
        if (isPermissionGranted) {
            Log.i(TAG, "Permission granted");
        } else {
            Log.w(TAG, "Permission for application permission policy is not granted");
            Toast.makeText(this.getContext(), "You need to re-enable admin to make this work", Toast.LENGTH_LONG).show();
            // TODO: if not show re-enable dialog
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO: Check if premium
        getActivity().setTitle("App Permissions");
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
        }
        sharedAppPermissionViewModel = ViewModelProviders.of(getActivity()).get(SharedAppPermissionViewModel.class);
        fragmentManager = getActivity().getSupportFragmentManager();
        adhellPermissionInfos = AdhellPermissionInfo.createPermissions();
        View view = inflater.inflate(R.layout.fragment_adhell_permission_info, container, false);
        RecyclerView permissionInfoRecyclerView = view.findViewById(R.id.permissionInfoRecyclerView);
        AdhellPermissionInfoAdapter adhellPermissionInfoAdapter = new AdhellPermissionInfoAdapter(this.getContext(), adhellPermissionInfos);
        permissionInfoRecyclerView.setAdapter(adhellPermissionInfoAdapter);
        permissionInfoRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL);

        permissionInfoRecyclerView.addItemDecoration(itemDecoration);
        ItemClickSupport.addTo(permissionInfoRecyclerView).setOnItemClickListener(
                (recyclerView, position, v) -> {
                    sharedAppPermissionViewModel.select(adhellPermissionInfos.get(position));
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainer, new AdhellPermissionInAppsFragment());
                    fragmentTransaction.addToBackStack("permissionsInfo_permissionsInApp");
                    fragmentTransaction.commit();
                }
        );
        return view;
    }
}
