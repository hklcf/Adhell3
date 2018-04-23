package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.LoadAppAsyncTask;
import com.fusionjack.adhell3.tasks.RefreshAppAsyncTask;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.List;

public class DnsFragment extends Fragment {
    private Context context;
    private final static String DNS_ALL_APPS_ENBLED = "dnsAllAppsEnabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (AdhellFactory.getInstance().isDnsAllowed()) {
            inflater.inflate(R.menu.app_menu, menu);
            AppFlag appFlag = AppFlag.createDnsFlag();

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
                toggleAllApps();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleAllApps() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.dialog_toggle_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_toggle_info);
        new AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                AsyncTask.execute(() -> {
                    SharedPreferences sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

                    boolean isAllEnabled = sharedPreferences.getBoolean(DNS_ALL_APPS_ENBLED, false);
                    if (isAllEnabled) {
                        List<AppInfo> dnsApps = appDatabase.applicationInfoDao().getDnsApps();
                        for (AppInfo app : dnsApps) {
                            app.hasCustomDns = false;
                            appDatabase.applicationInfoDao().update(app);
                        }
                        appDatabase.dnsPackageDao().deleteAll();
                    } else {
                        appDatabase.dnsPackageDao().deleteAll();
                        List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
                        for (AppInfo app : userApps) {
                            app.hasCustomDns = true;
                            appDatabase.applicationInfoDao().update(app);
                            DnsPackage dnsPackage = new DnsPackage();
                            dnsPackage.packageName = app.packageName;
                            dnsPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.dnsPackageDao().insert(dnsPackage);
                        }
                    }

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(DNS_ALL_APPS_ENBLED, !isAllEnabled);
                    editor.apply();

                    AppFlag appFlag = AppFlag.createDnsFlag();
                    new LoadAppAsyncTask("", appFlag, getContext()).execute();
                })
            )
            .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = null;
        if (AdhellFactory.getInstance().isDnsAllowed()) {
            view = inflater.inflate(R.layout.fragment_dns, container, false);

            AppFlag appFlag = AppFlag.createDnsFlag();
            ListView listView = view.findViewById(R.id.dns_apps_list);
            if (AdhellFactory.getInstance().isDnsNotEmpty()) {
                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                });
            }

            SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.dnsSwipeContainer);
            dnsSwipeContainer.setOnRefreshListener(() ->
                    new RefreshAppAsyncTask(appFlag, context).execute()
            );

            AppCache.getInstance(context, null);
            new LoadAppAsyncTask("", appFlag, context).execute();

            FloatingActionsMenu dnsFloatMenu = view.findViewById(R.id.dns_actions);
            FloatingActionButton actionSetDns = view.findViewById(R.id.action_set_dns);
            actionSetDns.setIcon(R.drawable.ic_dns_black_24dp);
            actionSetDns.setOnClickListener(v -> {
                dnsFloatMenu.collapse();

                View dialogView = inflater.inflate(R.layout.dialog_set_dns, container, false);
                EditText primaryDnsEditText = dialogView.findViewById(R.id.primaryDnsEditText);
                EditText secondaryDnsEditText = dialogView.findViewById(R.id.secondaryDnsEditText);
                if (AdhellFactory.getInstance().isDnsNotEmpty()) {
                    SharedPreferences sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
                    primaryDnsEditText.setText(sharedPreferences.getString("dns1", "0.0.0.0"));
                    secondaryDnsEditText.setText(sharedPreferences.getString("dns2", "0.0.0.0"));
                }
                primaryDnsEditText.requestFocus();

                new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Handler handler = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    Toast.makeText(context, getString(Integer.parseInt(msg.obj.toString())), Toast.LENGTH_LONG).show();
                                }
                            };

                            String primaryDns = primaryDnsEditText.getText().toString();
                            String secondaryDns = secondaryDnsEditText.getText().toString();
                            AdhellFactory.getInstance().setDns(primaryDns, secondaryDns, handler);

                            if (AdhellFactory.getInstance().isDnsNotEmpty()) {
                                listView.setEnabled(true);
                                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                                });
                            } else {
                                listView.setEnabled(false);
                            }

                            if (listView.getAdapter() instanceof AppInfoAdapter) {
                                ((AppInfoAdapter) listView.getAdapter()).notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
            });
        }

        return view;
    }
}
