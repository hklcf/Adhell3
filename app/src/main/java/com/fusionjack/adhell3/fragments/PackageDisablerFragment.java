package com.fusionjack.adhell3.fragments;

import android.app.enterprise.ApplicationPolicy;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_DISABLED;
import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_DISABLED_ALPHABETICALLY;
import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_DISABLED_INSTALL_TIME;

public class PackageDisablerFragment extends Fragment {

    @Nullable
    @Inject
    ApplicationPolicy appPolicy;

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    private Context context;
    private int sortState;
    private int layout;
    private AppFlag appFlag;

    public PackageDisablerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get().getAppComponent().inject(this);
        context = getContext();
        appFlag = AppFlag.createDisablerFlag();
        sortState = SORTED_DISABLED_ALPHABETICALLY;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.package_disabler_fragment_title));
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
        }
        setHasOptionsMenu(true);

        layout = R.id.installed_apps_list;

        View view = inflater.inflate(R.layout.fragment_package_disabler, container, false);
        ListView installedAppsView = view.findViewById(R.id.installed_apps_list);
        installedAppsView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            AppInfoAdapter disablerAppAdapter = (AppInfoAdapter) adView.getAdapter();
            String packageName = disablerAppAdapter.getItem(position).packageName;
            new SetAppAsyncTask(packageName, view2, appDatabase, appPolicy).execute();
        });

        SwipeRefreshLayout swipeContainer = view.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(() ->
                new RefreshAppAsyncTask(sortState, layout, appFlag, context).execute()
        );

        new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.package_disabler_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                new LoadAppAsyncTask(text, sortState, layout, appFlag, context).execute();
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pack_dis_sort:
                break;
            case R.id.sort_alphabetically_item:
                if (sortState == SORTED_DISABLED_ALPHABETICALLY) break;
                sortState = SORTED_DISABLED_ALPHABETICALLY;
                Toast.makeText(context, getString(R.string.app_list_sorted_by_alphabet), Toast.LENGTH_SHORT).show();
                new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
                break;
            case R.id.sort_by_time_item:
                if (sortState == SORTED_DISABLED_INSTALL_TIME) break;
                sortState = SORTED_DISABLED_INSTALL_TIME;
                Toast.makeText(context, getString(R.string.app_list_sorted_by_date), Toast.LENGTH_SHORT).show();
                new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
                break;
            case R.id.sort_disabled_item:
                if (sortState == SORTED_DISABLED) break;
                sortState = SORTED_DISABLED;
                Toast.makeText(context, getString(R.string.app_list_sorted_by_disabled), Toast.LENGTH_SHORT).show();
                new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
                break;
            case R.id.disabler_enable_all:
                Toast.makeText(context, getString(R.string.enabled_all_disabled), Toast.LENGTH_SHORT).show();
                enableAllPackages();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        AsyncTask.execute(() -> {
            if (appPolicy == null) {
                return;
            }

            List<AppInfo> disabledAppList = appDatabase.applicationInfoDao().getDisabledApps();
            for (AppInfo app : disabledAppList) {
                app.disabled = false;
                appPolicy.setEnableApplication(app.packageName);
                appDatabase.applicationInfoDao().insert(app);
            }
            appDatabase.disabledPackageDao().deleteAll();
            new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
        });
    }

    private static class SetAppAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<View> viewWeakReference;
        private AppDatabase appDatabase;
        private ApplicationPolicy appPolicy;
        private String packageName;

        SetAppAsyncTask(String packageName, View view, AppDatabase appDatabase, ApplicationPolicy appPolicy) {
            this.viewWeakReference = new WeakReference<>(view);
            this.packageName = packageName;
            this.appDatabase = appDatabase;
            this.appPolicy = appPolicy;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (appPolicy == null) {
                return false;
            }

            AppInfo appInfo = appDatabase.applicationInfoDao().getByPackageName(packageName);
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

            return appInfo.disabled;
        }

        @Override
        protected void onPostExecute(Boolean state) {
            ((Switch) viewWeakReference.get().findViewById(R.id.switchDisable)).setChecked(!state);
        }
    }
}
