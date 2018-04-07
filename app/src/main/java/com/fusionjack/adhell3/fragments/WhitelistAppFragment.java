package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.fusionjack.adhell3.fragments.LoadAppAsyncTask.SORTED_WHITELISTED;

public class WhitelistAppFragment extends Fragment {
    private Context context;
    private int sortState;
    private int layout;
    private AppFlag appFlag;

    public WhitelistAppFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        appFlag = AppFlag.createWhitelistedFlag();
        sortState = SORTED_WHITELISTED;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        layout = R.id.whitelisted_apps_list;

        View view = inflater.inflate(R.layout.fragment_whitelisted_app, container, false);

        ListView whitelistedAppsView = view.findViewById(layout);
        whitelistedAppsView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
            String packageName = adapter.getItem(position).packageName;
            new SetAppAsyncTask(packageName, view2).execute();
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
        inflater.inflate(R.menu.whitelisted_app_menu, menu);

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
            case R.id.whitelist_enable_all:
                Toast.makeText(context, getString(R.string.enabled_all_restricted), Toast.LENGTH_SHORT).show();
                enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        AsyncTask.execute(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
            for (AppInfo app : whitelistedAppList) {
                app.adhellWhitelisted = false;
                appDatabase.applicationInfoDao().insert(app);
            }
            appDatabase.firewallWhitelistedPackageDao().deleteAll();
            new LoadAppAsyncTask("", sortState, layout, appFlag, context).execute();
        });
    }

    private static class SetAppAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<View> viewWeakReference;
        private AppDatabase appDatabase;
        private String packageName;

        SetAppAsyncTask(String packageName, View view) {
            this.viewWeakReference = new WeakReference<>(view);
            this.packageName = packageName;
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            AppInfo appInfo = appDatabase.applicationInfoDao().getByPackageName(packageName);
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
            return appInfo.adhellWhitelisted;
        }

        @Override
        protected void onPostExecute(Boolean state) {
            ((Switch) viewWeakReference.get().findViewById(R.id.switchDisable)).setChecked(!state);
        }
    }
}
