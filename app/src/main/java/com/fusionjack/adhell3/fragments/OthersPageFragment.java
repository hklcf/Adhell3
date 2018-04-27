package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.app.enterprise.ApplicationPolicy;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AdhellPermissionInfoAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.blocker.ContentBlocker57;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.model.AdhellPermissionInfo;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OthersPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private Context context;

    public static final int PERMISSIONS_PAGE = 0;
    public static final int DNS_PAGE = 1;
    public static final int SETTINGS_PAGE = 2;

    public static OthersPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        OthersPageFragment fragment = new OthersPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.page = getArguments().getInt(ARG_PAGE);
        this.context = getContext();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (page == PERMISSIONS_PAGE) {
            inflater.inflate(R.menu.app_permission_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllPermissions();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPermissions() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_enable_permissions, (ViewGroup) getView(), false);
        new AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                AsyncTask.execute(() -> {
                   ApplicationPermissionControlPolicy policy = AdhellFactory.getInstance().getAppControlPolicy();
                   if (policy != null) {
                       policy.clearPackagesFromPermissionBlackList();
                       AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                       appDatabase.appPermissionDao().deleteAll();
                   }
                });
            })
            .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = null;
        ContentBlocker contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();
        switch (page) {
            case PERMISSIONS_PAGE:
                view = inflater.inflate(R.layout.fragment_adhell_permission_info, container, false);
                List<AdhellPermissionInfo> permissionInfos = AdhellPermissionInfo.getPermissionList();
                if (permissionInfos == null) {
                    new CreatePermissionsAsyncTask(context, getActivity()).execute();
                } else {
                    ListView listView = view.findViewById(R.id.permissionInfoListView);
                    AdhellPermissionInfoAdapter adapter = new AdhellPermissionInfoAdapter(context, permissionInfos);
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
                        new CreatePermissionsAsyncTask(context, getActivity()).execute()
                );
                break;

            case DNS_PAGE:
                final SharedPreferences sharedPreferences = context.getSharedPreferences("dnsAddresses", Context.MODE_PRIVATE);
                if (contentBlocker instanceof ContentBlocker56 || contentBlocker instanceof ContentBlocker57) {
                    view = inflater.inflate(R.layout.fragment_dns, container, false);

                    EditText dns1EditText = view.findViewById(R.id.dns_address_1);
                    EditText dns2EditText = view.findViewById(R.id.dns_address_2);
                    if (sharedPreferences.contains("dns1") && sharedPreferences.contains("dns2")) {
                        dns1EditText.setText(sharedPreferences.getString("dns1", "0.0.0.0"));
                        dns2EditText.setText(sharedPreferences.getString("dns2", "0.0.0.0"));
                    }
                    dns1EditText.requestFocus();

                    Button setDnsButton = view.findViewById(R.id.changeDnsOkButton);
                    setDnsButton.setOnClickListener(v -> {
                        String dns1 = dns1EditText.getText().toString();
                        String dns2 = dns2EditText.getText().toString();
                        if (!Patterns.IP_ADDRESS.matcher(dns1).matches() || !Patterns.IP_ADDRESS.matcher(dns2).matches()) {
                            Toast.makeText(context, getString(R.string.check_input_dns), Toast.LENGTH_LONG).show();
                            return;
                        }

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("dns1", dns1);
                        editor.putString("dns2", dns2);
                        editor.apply();
                        Toast.makeText(context, getString(R.string.changed_dns), Toast.LENGTH_LONG).show();
                    });

                    Button restoreDefaultDnsButton = view.findViewById(R.id.restoreDefaultDnsButton);
                    restoreDefaultDnsButton.setOnClickListener(v -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("dns1");
                        editor.remove("dns2");
                        editor.apply();
                        dns1EditText.setText("0.0.0.0");
                        dns2EditText.setText("0.0.0.0");
                        Toast.makeText(context, getString(R.string.restored_dns), Toast.LENGTH_LONG).show();
                    });
                }
                break;

            case SETTINGS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_settings, container, false);

                Button deleteAppButton = view.findViewById(R.id.deleteApp);
                deleteAppButton.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle(getString(R.string.delete_app_dialog_title))
                    .setMessage(getString(R.string.delete_app_dialog_text))
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        contentBlocker.disableDomainRules();
                        contentBlocker.disableFirewallRules();
                        ComponentName devAdminReceiver = new ComponentName(context, CustomDeviceAdminReceiver.class);
                        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.removeActiveAdmin(devAdminReceiver);
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        String packageName = "package:" + BuildConfig.APPLICATION_ID;
                        intent.setData(Uri.parse(packageName));
                        startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.no, null).show());

                Button backupDatabaseButton = view.findViewById(R.id.backup_database);
                backupDatabaseButton.setOnClickListener(view1 ->
                    new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.backup_database_dialog_title))
                        .setMessage(getString(R.string.backup_database_dialog_text))
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new BackupDatabaseAsyncTask(getActivity()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null).show()
                );

                Button restoreDatabaseButton = view.findViewById(R.id.restore_database);
                restoreDatabaseButton.setOnClickListener(view2 ->
                    new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.restore_database_dialog_title))
                        .setMessage(getString(R.string.restore_database_dialog_text))
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new RestoreDatabaseAsyncTask(getActivity()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null).show()
                );
                break;
        }

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
                    permissionList.add(new AdhellPermissionInfo(permissionName,
                            description == null ? "No description" : description.toString(),
                            info.protectionLevel));
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
                ListView listView = ((Activity)context).findViewById(R.id.permissionInfoListView);
                if (listView != null) {
                    AdhellPermissionInfoAdapter adapter = new AdhellPermissionInfoAdapter(context, permissionInfos);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(
                        (AdapterView<?> adView, View view2, int position, long id) -> {
                            AdhellPermissionInfo permissionInfo = permissionInfos.get(position);

                            FragmentActivity activity = activityReference.get();
                            FragmentManager fragmentManager = activity.getSupportFragmentManager();
                            SharedAppPermissionViewModel viewModel = ViewModelProviders.of(activity).get(SharedAppPermissionViewModel.class);
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

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
            }
        }
    }

    private static class BackupDatabaseAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;

        BackupDatabaseAsyncTask(Activity activity) {
            dialog = new ProgressDialog(activity);
            builder = new AlertDialog.Builder(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Backup database is running...");
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            try {
                DatabaseFactory.getInstance().backupDatabase();
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String message) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (message == null) {
                builder.setMessage("Backup database is finished");
                builder.setTitle("Info");
            } else {
                builder.setMessage(message);
                builder.setTitle("Error");
            }
            builder.create().show();
        }
    }

    private static class RestoreDatabaseAsyncTask extends AsyncTask<Void, String, String> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;
        private AppDatabase appDatabase;
        private ApplicationPolicy appPolicy;
        private ApplicationPermissionControlPolicy appControlPolicy;

        RestoreDatabaseAsyncTask(Activity activity) {
            this.builder = new AlertDialog.Builder(activity);
            this.dialog = new ProgressDialog(activity);
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
            this.appControlPolicy = AdhellFactory.getInstance().getAppControlPolicy();
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Restore database is running...");
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            try {
                DatabaseFactory.getInstance().restoreDatabase();

                publishProgress("Updating all providers...");

                List<BlockUrlProvider> providers = appDatabase.blockUrlProviderDao().getAll2();
                appDatabase.blockUrlDao().deleteAll();
                for (BlockUrlProvider provider : providers) {
                    try {
                        List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                        provider.count = blockUrls.size();
                        provider.lastUpdated = new Date();
                        appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                        appDatabase.blockUrlDao().insertAll(blockUrls);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                publishProgress("Disabling apps...");
                List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
                for (DisabledPackage disabledPackage : disabledPackages) {
                    appPolicy.setDisableApplication(disabledPackage.packageName);
                }

                publishProgress("Disabling app's permissions...");
                List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
                for (AppPermission appPermission : appPermissions) {
                    List<String> packageList = new ArrayList<>();
                    packageList.add(appPermission.packageName);
                    appControlPolicy.addPackagesToPermissionBlackList(appPermission.permissionName, packageList);
                }

                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(String message) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (message == null) {
                builder.setMessage("Restore database is finished. Turn on Adhell.");
                builder.setTitle("Info");
            } else {
                builder.setMessage(message);
                builder.setTitle("Error");
            }
            builder.create().show();
        }
    }
}
