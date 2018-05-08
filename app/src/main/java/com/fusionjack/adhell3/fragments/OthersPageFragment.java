package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.ApplicationPermissionControlPolicy;
import android.app.enterprise.ApplicationPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
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
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.blocker.ContentBlocker57;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;

import java.util.Date;
import java.util.List;

public class OthersPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private Context context;

    public static final int APP_COMPONENT_PAGE = 0;
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

        if (page == APP_COMPONENT_PAGE) {
            inflater.inflate(R.menu.app_component_menu, menu);

            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    if (searchView.isIconified()) {
                        return false;
                    }
                    AppFlag appFlag = AppFlag.createComponentFlag();
                    new LoadAppAsyncTask(text, appFlag, getContext()).execute();
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllAppComponents();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllAppComponents() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.dialog_enable_components_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_enable_components_info);
        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        AsyncTask.execute(() ->
                                AdhellFactory.getInstance().setAppComponentState(true)
                        )
                )
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = null;
        ContentBlocker contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();
        switch (page) {
            case APP_COMPONENT_PAGE:
                view = inflater.inflate(R.layout.fragment_app_component, container, false);

                AppFlag appFlag = AppFlag.createComponentFlag();
                ListView listView = view.findViewById(appFlag.getLoadLayout());
                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    Bundle bundle = new Bundle();
                    AppInfo appInfo = adapter.getItem(position);
                    bundle.putString("packageName", appInfo.packageName);
                    bundle.putString("appName", appInfo.appName);
                    AppComponentFragment fragment = new AppComponentFragment();
                    fragment.setArguments(bundle);

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                    fragmentTransaction.addToBackStack("appComponents");
                    fragmentTransaction.commit();
                });

                SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
                swipeContainer.setOnRefreshListener(() ->
                        new RefreshAppAsyncTask(appFlag, context).execute()
                );

                AppCache.getInstance(context, null);
                new LoadAppAsyncTask("", appFlag, context).execute();
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
                deleteAppButton.setOnClickListener(v -> {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                    TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
                    titlTextView.setText(R.string.delete_app_dialog_title);
                    TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                    questionTextView.setText(R.string.delete_app_dialog_text);

                    new AlertDialog.Builder(context)
                            .setView(dialogView)
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
                            .setNegativeButton(android.R.string.no, null).show();
                });

                Button backupDatabaseButton = view.findViewById(R.id.backup_database);
                backupDatabaseButton.setOnClickListener(view1 -> {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                    TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
                    titlTextView.setText(R.string.backup_database_dialog_title);
                    TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                    questionTextView.setText(R.string.backup_database_dialog_text);

                    new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                    new BackupDatabaseAsyncTask(getActivity()).execute()
                            )
                            .setNegativeButton(android.R.string.no, null).show();
                });

                Button restoreDatabaseButton = view.findViewById(R.id.restore_database);
                restoreDatabaseButton.setOnClickListener(view2 -> {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                    TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
                    titlTextView.setText(R.string.restore_database_dialog_title);
                    TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                    questionTextView.setText(R.string.restore_database_dialog_text);

                    new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                    new RestoreDatabaseAsyncTask(getActivity()).execute()
                            )
                            .setNegativeButton(android.R.string.no, null).show();
                });
                break;
        }

        return view;
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

                publishProgress("Disabling app components...");
                AdhellFactory.getInstance().setAppComponentState(false);

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
