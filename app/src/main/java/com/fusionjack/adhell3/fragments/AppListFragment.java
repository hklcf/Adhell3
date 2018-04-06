package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppWhitelistAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.viewmodel.AdhellWhitelistAppsViewModel;

public class AppListFragment extends Fragment {
    private static final String TAG = AppListFragment.class.getCanonicalName();
    private ListView appListView;
    private AppWhitelistAdapter appWhitelistAdapter;
    private EditText adblockEnabledAppSearchEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        appListView = view.findViewById(R.id.appList);
        adblockEnabledAppSearchEditText = view.findViewById(R.id.adblockEnabledAppSearchEditText);

        adblockEnabledAppSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (appWhitelistAdapter != null) {
                    appWhitelistAdapter.getFilter().filter(editable.toString());
                }
            }
        });

        AdhellWhitelistAppsViewModel adhellWhitelistAppsViewModel = ViewModelProviders.of(getActivity()).get(AdhellWhitelistAppsViewModel.class);
        adhellWhitelistAppsViewModel.getSortedAppInfo().observe(this, appInfos -> {
            if (appWhitelistAdapter == null) {
                appWhitelistAdapter = new AppWhitelistAdapter(this.getContext(), appInfos);
                appListView.setAdapter(appWhitelistAdapter);
            } else {
                appWhitelistAdapter.notifyDataSetChanged();
            }
        });

        appListView.setOnItemClickListener((parent, view1, position, id) -> {
            AppInfo appInfo = (AppInfo) parent.getItemAtPosition(position);
            Log.d(TAG, "Will toggle app: " + appInfo.packageName);
            adhellWhitelistAppsViewModel.toggleApp(appInfo);
        });
        return view;
    }
}
