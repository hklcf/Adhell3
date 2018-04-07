package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AdhellPermissionInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdhellPermissionInfoFragment extends Fragment {
    private AppCompatActivity parentActivity;

    public AdhellPermissionInfoFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (AppCompatActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().setTitle("App Permissions");
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
        }

        View view = inflater.inflate(R.layout.fragment_adhell_permission_info, container, false);
        List<AdhellPermissionInfo> permissionInfos = AdhellPermissionInfo.getPermissionList();
        if (permissionInfos == null) {
            new CreatePermissionsAsyncTask(getContext(), getActivity()).execute();
        } else {
            ListView listView = view.findViewById(R.id.permissionInfoRecyclerView);
            AdhellPermissionInfoAdapter adapter = new AdhellPermissionInfoAdapter(getContext(), permissionInfos);
            listView.setAdapter(adapter);

            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            SharedAppPermissionViewModel viewModel = ViewModelProviders.of(getActivity()).get(SharedAppPermissionViewModel.class);
            listView.setOnItemClickListener(
                    (AdapterView<?> adView, View view2, int position, long id) -> {
                        AdhellPermissionInfo permissionInfo = permissionInfos.get(position);
                        viewModel.select(permissionInfo);

                        Bundle bundle = new Bundle();
                        bundle.putString("permissionName", permissionInfo.name);
                        AdhellPermissionInAppsFragment fragment = new AdhellPermissionInAppsFragment();
                        fragment.setArguments(bundle);

                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                        fragmentTransaction.addToBackStack("permissionsInfo_permissionsInApp");
                        fragmentTransaction.commit();
                    }
            );
        }

        SwipeRefreshLayout swipeContainer = view.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(() ->
                new CreatePermissionsAsyncTask(getContext(), getActivity()).execute()
        );

        return view;
    }

    private static class CreatePermissionsAsyncTask extends AsyncTask<Void, Void, List<AdhellPermissionInfo>> {
        private ProgressDialog dialog;
        private PackageManager packageManager;
        private AppDatabase appDatabase;
        private List<AdhellPermissionInfo> permissionList;
        private WeakReference<Context> contextReference;
        private WeakReference<FragmentActivity> activityReference;

        CreatePermissionsAsyncTask(Context context, FragmentActivity activity) {
            this.contextReference = new WeakReference<>(context);
            this.activityReference = new WeakReference<>(activity);
            this.packageManager = AdhellFactory.getInstance().getPackageManager();
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.dialog = new ProgressDialog(context);
            this.dialog.setCancelable(false);
            this.permissionList = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Creating permissions...");
            dialog.show();
        }

        @Override
        protected List<AdhellPermissionInfo> doInBackground(Void... voids) {
            Set<String> permissionNameList = new HashSet<>();

            List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
            for (AppInfo userApp : userApps) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(userApp.packageName, PackageManager.GET_PERMISSIONS);
                    if (packageInfo != null) {
                        String[] permissions = packageInfo.requestedPermissions;
                        if (permissions != null) {
                            for (String permissionName : permissions) {
                                if (permissionName.startsWith("android.permission.") ||
                                        permissionName.startsWith("com.android.")) {
                                    permissionNameList.add(permissionName);
                                }
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            List<String> sortedPermissionNameList = new ArrayList<>(permissionNameList);
            Collections.sort(sortedPermissionNameList);
            for (String permissionName : sortedPermissionNameList) {
                try {
                    PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
                    CharSequence description = info.loadDescription(packageManager);
                    permissionList.add(new AdhellPermissionInfo(permissionName, description == null ? "No description" : description.toString()));
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            AdhellPermissionInfo.cachePermissionList(permissionList);
            return permissionList;
        }

        @Override
        protected void onPostExecute(List<AdhellPermissionInfo> permissionInfos) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            Context context = contextReference.get();
            if (context != null) {
                ListView listView = ((Activity)context).findViewById(R.id.permissionInfoRecyclerView);
                AdhellPermissionInfoAdapter adapter = new AdhellPermissionInfoAdapter(context, permissionInfos);
                listView.setAdapter(adapter);

                FragmentActivity activity = activityReference.get();
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                SharedAppPermissionViewModel viewModel = ViewModelProviders.of(activity).get(SharedAppPermissionViewModel.class);
                listView.setOnItemClickListener(
                        (AdapterView<?> adView, View view2, int position, long id) -> {
                            AdhellPermissionInfo permissionInfo = permissionInfos.get(position);
                            viewModel.select(permissionInfo);

                            Bundle bundle = new Bundle();
                            bundle.putString("permissionName", permissionInfo.name);
                            AdhellPermissionInAppsFragment fragment = new AdhellPermissionInAppsFragment();
                            fragment.setArguments(bundle);

                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                            fragmentTransaction.addToBackStack("permissionsInfo_permissionsInApp");
                            fragmentTransaction.commit();
                        }
                );

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
                swipeContainer.setRefreshing(false);
            }
        }
    }
}
