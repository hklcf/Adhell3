package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

public class AppTabPageFragment extends AppFragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private AppFlag appFlag;

    public static final int PACKAGE_DISABLER_PAGE = 0;
    public static final int MOBILE_RESTRICTER_PAGE = 1;
    public static final int WIFI_RESTRICTER_PAGE = 2;
    public static final int WHITELIST_PAGE = 3;

    public static AppTabPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AppTabPageFragment fragment = new AppTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.page = getArguments().getInt(ARG_PAGE);

        AppRepository.Type type;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                type = AppRepository.Type.DISABLER;
                break;

            case MOBILE_RESTRICTER_PAGE:
                type = AppRepository.Type.MOBILE_RESTRICTED;
                break;

            case WIFI_RESTRICTER_PAGE:
                type = AppRepository.Type.WIFI_RESTRICTED;
                break;

            case WHITELIST_PAGE:
                type = AppRepository.Type.WHITELISTED;
                break;

            default:
                type = AppRepository.Type.DISABLER;
                break;
        }
        initAppModel(type);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = null;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                view = inflater.inflate(R.layout.fragment_package_disabler, container, false);
                appFlag = AppFlag.createDisablerFlag();
                break;

            case MOBILE_RESTRICTER_PAGE:
                view = inflater.inflate(R.layout.fragment_mobile_restricter, container, false);
                appFlag = AppFlag.createMobileRestrictedFlag();
                break;

            case WIFI_RESTRICTER_PAGE:
                view = inflater.inflate(R.layout.fragment_wifi_restricter, container, false);
                appFlag = AppFlag.createWifiRestrictedFlag();
                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_whitelisted_app, container, false);
                appFlag = AppFlag.createWhitelistedFlag();
                break;
        }

        if (view != null) {
            ListView listView = view.findViewById(appFlag.getLoadLayout());
            listView.setAdapter(adapter);
            if (page != PACKAGE_DISABLER_PAGE || AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                });
            }

            SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
            swipeContainer.setOnRefreshListener(() -> {
                    getAppList("", type);
                    swipeContainer.setRefreshing(false);
                    resetSearchView();
            });

            getAppList("", type);
        }

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.enable_apps_dialog_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.enable_apps_dialog_text);

        new AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                Toast.makeText(getContext(), getString(R.string.enabled_all_apps), Toast.LENGTH_SHORT).show();
                AsyncTask.execute(() -> {
                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                    switch (page) {
                        case PACKAGE_DISABLER_PAGE:
                            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                            List<AppInfo> disabledAppList = appDatabase.applicationInfoDao().getDisabledApps();
                            for (AppInfo app : disabledAppList) {
                                app.disabled = false;
                                if (appPolicy != null) {
                                    appPolicy.setEnableApplication(app.packageName);
                                }
                                appDatabase.applicationInfoDao().update(app);
                            }
                            appDatabase.disabledPackageDao().deleteAll();
                            break;

                        case MOBILE_RESTRICTER_PAGE:
                            List<AppInfo> mobileAppList = appDatabase.applicationInfoDao().getMobileRestrictedApps();
                            for (AppInfo app : mobileAppList) {
                                app.mobileRestricted = false;
                                appDatabase.applicationInfoDao().update(app);
                            }
                            appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.MOBILE_RESTRICTED_TYPE);
                            break;

                        case WIFI_RESTRICTER_PAGE:
                            List<AppInfo> wifiAppList = appDatabase.applicationInfoDao().getWifiRestrictedApps();
                            for (AppInfo app : wifiAppList) {
                                app.wifiRestricted = false;
                                appDatabase.applicationInfoDao().update(app);
                            }
                            appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.WIFI_RESTRICTED_TYPE);
                            break;

                        case WHITELIST_PAGE:
                            List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
                            for (AppInfo app : whitelistedAppList) {
                                app.adhellWhitelisted = false;
                                appDatabase.applicationInfoDao().update(app);
                            }
                            appDatabase.firewallWhitelistedPackageDao().deleteAll();
                            break;
                    }
                    getAppList("", type);
                });
            })
            .setNegativeButton(android.R.string.no, null).show();
    }
}
