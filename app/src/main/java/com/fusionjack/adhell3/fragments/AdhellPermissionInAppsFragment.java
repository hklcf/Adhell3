package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AdhellPermissionInAppsAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AdhellPermissionInAppsFragment extends Fragment {
    private AppCompatActivity parentActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (AppCompatActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(true);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }

        AppCache.getInstance(getContext(), null);

        Bundle bundle = getArguments();
        if (bundle != null) {
            new GetAppsByPermissionAsyncTask(bundle.getString("permissionName"), this,
                    getContext(), getActivity()).execute();
        }

        return inflater.inflate(R.layout.fragment_permission_in_apps, container, false);
    }

    private static class GetAppsByPermissionAsyncTask extends AsyncTask<Void, Void, List<AppInfo>> {
        private AppDatabase appDatabase;
        private PackageManager packageManager;
        private List<AppInfo> permissionsApps = new ArrayList<>();
        private String permissionName;
        private AdhellPermissionInAppsFragment fragment;
        private WeakReference<Context> contextReference;
        private WeakReference<FragmentActivity> activityReference;

        GetAppsByPermissionAsyncTask(String permissionName, AdhellPermissionInAppsFragment fragment,
                                     Context context, FragmentActivity activity) {
            this.contextReference = new WeakReference<>(context);
            this.activityReference = new WeakReference<>(activity);
            this.fragment = fragment;
            this.permissionName = permissionName;
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.packageManager = AdhellFactory.getInstance().getPackageManager();
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
            for (AppInfo userApp : userApps) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(userApp.packageName, PackageManager.GET_PERMISSIONS);
                    if (packageInfo != null) {
                        String[] permissions = packageInfo.requestedPermissions;
                        if (permissions != null) {
                            for (String permissionName : permissions) {
                                if (permissionName.equalsIgnoreCase(this.permissionName)) {
                                    permissionsApps.add(userApp);
                                    break;
                                }
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            return permissionsApps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            Context context = contextReference.get();
            if (context != null) {
                RecyclerView recyclerView = ((Activity) context).findViewById(R.id.permissionInAppsRecyclerView);
                if (recyclerView != null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
                    itemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider));
                    recyclerView.addItemDecoration(itemDecoration);

                    FragmentActivity activity = activityReference.get();
                    SharedAppPermissionViewModel viewModel = ViewModelProviders.of(activity).get(SharedAppPermissionViewModel.class);
                    viewModel.getSelected().observe(fragment, permissionInfo -> {
                        if (permissionInfo != null) {
                            activity.setTitle(permissionInfo.name);
                            AdhellPermissionInAppsAdapter adapter = new AdhellPermissionInAppsAdapter(permissionInfo.name, appInfos, context);
                            recyclerView.setAdapter(adapter);
                        }
                    });
                }
            }
        }
    }
}
