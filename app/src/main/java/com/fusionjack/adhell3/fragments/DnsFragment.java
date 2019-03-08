package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
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
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.List;

public class DnsFragment extends AppFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initAppModel(AppRepository.Type.DNS);
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
                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

                    boolean isAllEnabled = AppPreferences.getInstance().isDnsAllAppsEnabled();
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

                    AppPreferences.getInstance().setDnsAllApps(!isAllEnabled);

                    loadAppList(type);
                })
            )
            .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_dns, container, false);

        AppFlag appFlag = AppFlag.createDnsFlag();
        ListView listView = view.findViewById(R.id.dns_apps_list);
        listView.setAdapter(adapter);
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
            });
        }

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.dnsSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() -> {
                loadAppList(type);
                dnsSwipeContainer.setRefreshing(false);
                resetSearchView();
        });

        FloatingActionsMenu dnsFloatMenu = view.findViewById(R.id.dns_actions);
        FloatingActionButton actionSetDns = view.findViewById(R.id.action_set_dns);
        actionSetDns.setIcon(R.drawable.ic_dns_white_24dp);
        actionSetDns.setOnClickListener(v -> {
            dnsFloatMenu.collapse();

            View dialogView = inflater.inflate(R.layout.dialog_set_dns, container, false);
            EditText primaryDnsEditText = dialogView.findViewById(R.id.primaryDnsEditText);
            EditText secondaryDnsEditText = dialogView.findViewById(R.id.secondaryDnsEditText);
            if (AppPreferences.getInstance().isDnsNotEmpty()) {
                primaryDnsEditText.setText(AppPreferences.getInstance().getDns1());
                secondaryDnsEditText.setText(AppPreferences.getInstance().getDns2());
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

                        if (AppPreferences.getInstance().isDnsNotEmpty()) {
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

        return view;
    }
}
