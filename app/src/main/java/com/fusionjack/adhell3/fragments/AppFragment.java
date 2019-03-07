package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import java.util.ArrayList;
import java.util.List;

public class AppFragment extends Fragment {

    protected Context context;
    protected AppInfoAdapter adapter;
    protected AppRepository.Type type;
    private AppViewModel viewModel;
    private List<AppInfo> appInfoList;
    private String searchText;
    private SearchView searchView;

    protected void initAppModel(AppRepository.Type type) {
        this.context = getContext();
        this.type = type;
        this.searchText = "";

        AppCache.getInstance(context, null);

        appInfoList = new ArrayList<>();
        adapter = new AppInfoAdapter(appInfoList, type, false, context);

        viewModel = ViewModelProviders.of(this).get(AppViewModel.class);
        getAppList("", type);
    }

    protected void getAppList(String text, AppRepository.Type type) {
        viewModel.getAppList(text, type).observe(this, appInfos -> {
            appInfoList.clear();
            appInfoList.addAll(appInfos);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.app_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        if (!searchText.isEmpty()) {
            searchView.setQuery(searchText, false);
            searchView.setIconified(false);
            searchView.requestFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                searchText = text;
                getAppList(text, type);
                return false;
            }
        });
    }

    protected void resetSearchView() {
        if (searchView != null) {
            searchText = "";
            searchView.setQuery(searchText, false);
            searchView.setIconified(true);
        }
    }
}
