package com.fusionjack.adhell3.fragments;

import android.app.enterprise.ApplicationPolicy;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;

import java.lang.ref.WeakReference;
import java.util.List;

public class AppsPageFragment extends Fragment {
    private static final String ARG_PAGE = "page";
    private int page;
    private AppFlag appFlag;
    private Context context;

    public static final int PACKAGE_DISABLER_PAGE = 0;
    public static final int MOBILE_RESTRICTER_PAGE = 1;
    public static final int WHITELIST_PAGE = 2;

    public static AppsPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AppsPageFragment fragment = new AppsPageFragment();
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
                appFlag = AppFlag.createRestrictedFlag();
                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_whitelisted_app, container, false);
                appFlag = AppFlag.createWhitelistedFlag();
                break;
        }

        if (view != null) {
            ListView listView = view.findViewById(appFlag.getLoadLayout());
            listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                new SetAppAsyncTask(adapter, position, view2, page).execute();
            });

            SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
            swipeContainer.setOnRefreshListener(() ->
                    new RefreshAppAsyncTask(appFlag, context).execute()
            );

            AppCache.getInstance(context, null);
            new LoadAppAsyncTask("", appFlag, context).execute();
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.apps_management_menu, menu);

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
                AppFlag appFlag = null;
                switch (page) {
                    case PACKAGE_DISABLER_PAGE:
                        appFlag = AppFlag.createDisablerFlag();
                        break;
                    case MOBILE_RESTRICTER_PAGE:
                        appFlag = AppFlag.createRestrictedFlag();
                        break;
                    case WHITELIST_PAGE:
                        appFlag = AppFlag.createWhitelistedFlag();
                        break;
                }
                new LoadAppAsyncTask(text, appFlag, getContext()).execute();
                return false;
            }
        });
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
        new AlertDialog.Builder(context)
            .setTitle(getString(R.string.enable_apps_dialog_title))
            .setMessage(getString(R.string.enable_apps_dialog_text))
            .setIcon(R.drawable.ic_warning_black_24dp)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                Toast.makeText(getContext(), getString(R.string.enabled_all_apps), Toast.LENGTH_SHORT).show();
                AsyncTask.execute(() -> {
                    AppFlag appFlag = null;
                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                    switch (page) {
                        case PACKAGE_DISABLER_PAGE:
                            appFlag = AppFlag.createDisablerFlag();
                            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                            List<AppInfo> disabledAppList = appDatabase.applicationInfoDao().getDisabledApps();
                            for (AppInfo app : disabledAppList) {
                                app.disabled = false;
                                appPolicy.setEnableApplication(app.packageName);
                                appDatabase.applicationInfoDao().insert(app);
                            }
                            appDatabase.disabledPackageDao().deleteAll();
                            break;

                        case MOBILE_RESTRICTER_PAGE:
                            appFlag = AppFlag.createRestrictedFlag();
                            List<AppInfo> restrictedAppList = appDatabase.applicationInfoDao().getMobileRestrictedApps();
                            for (AppInfo app : restrictedAppList) {
                                app.mobileRestricted = false;
                                appDatabase.applicationInfoDao().insert(app);
                            }
                            appDatabase.restrictedPackageDao().deleteAll();
                            break;

                        case WHITELIST_PAGE:
                            appFlag = AppFlag.createWhitelistedFlag();
                            List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
                            for (AppInfo app : whitelistedAppList) {
                                app.adhellWhitelisted = false;
                                appDatabase.applicationInfoDao().insert(app);
                            }
                            appDatabase.firewallWhitelistedPackageDao().deleteAll();
                            break;
                    }
                    new LoadAppAsyncTask("", appFlag, getContext()).execute();
                });
            })
            .setNegativeButton(android.R.string.no, null).show();
    }

    private static class SetAppAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<View> viewWeakReference;
        private AppDatabase appDatabase;
        private ApplicationPolicy appPolicy;
        private int page;
        private int position;
        private AppInfoAdapter adapter;
        private AppInfo appInfo;

        SetAppAsyncTask(AppInfoAdapter adapter, int position, View view, int page) {
            this.viewWeakReference = new WeakReference<>(view);
            this.page = page;
            this.adapter = adapter;
            this.position = position;
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (appPolicy == null) {
                return false;
            }

            boolean state = false;
            String packageName = adapter.getItem(position).packageName;
            appInfo = appDatabase.applicationInfoDao().getAppByPackageName(packageName);
            switch (page) {
                case PACKAGE_DISABLER_PAGE:
                    appInfo.disabled = !appInfo.disabled;
                    if (appInfo.disabled) {
                        appPolicy.setDisableApplication(packageName);
                        DisabledPackage disabledPackage = new DisabledPackage();
                        disabledPackage.packageName = packageName;
                        disabledPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                        appDatabase.disabledPackageDao().insert(disabledPackage);
                    } else {
                        appPolicy.setEnableApplication(packageName);
                        appDatabase.disabledPackageDao().deleteByPackageName(packageName);
                    }
                    appDatabase.applicationInfoDao().insert(appInfo);
                    state = appInfo.disabled;
                    break;

                case MOBILE_RESTRICTER_PAGE:
                    appInfo.mobileRestricted = !appInfo.mobileRestricted;
                    if (appInfo.mobileRestricted) {
                        RestrictedPackage restrictedPackage = new RestrictedPackage();
                        restrictedPackage.packageName = packageName;
                        restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                        appDatabase.restrictedPackageDao().insert(restrictedPackage);
                    } else {
                        appDatabase.restrictedPackageDao().deleteByPackageName(packageName);
                    }
                    appDatabase.applicationInfoDao().insert(appInfo);
                    state = appInfo.mobileRestricted;
                    break;

                case WHITELIST_PAGE:
                    appInfo.adhellWhitelisted = !appInfo.adhellWhitelisted;
                    if (appInfo.adhellWhitelisted) {
                        FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                        whitelistedPackage.packageName = packageName;
                        whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                        appDatabase.firewallWhitelistedPackageDao().insert(whitelistedPackage);
                    } else {
                        appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(packageName);
                    }
                    appDatabase.applicationInfoDao().insert(appInfo);
                    state = appInfo.adhellWhitelisted;
                    break;
            }
            return state;
        }

        @Override
        protected void onPostExecute(Boolean state) {
            ((Switch) viewWeakReference.get().findViewById(R.id.switchDisable)).setChecked(!state);

            adapter.setItem(position, appInfo);
            adapter.notifyDataSetChanged();
        }
    }
}
