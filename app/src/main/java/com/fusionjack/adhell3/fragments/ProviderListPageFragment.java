package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;
import java.util.List;

public class ProviderListPageFragment extends DomainsPageFragment {

    private String searchText;
    private Long providerId;

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
        new LoadBlockedUrlAsyncTask(getContext(), providerId).execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.searchText = "";
        this.providerId = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_show_blocked_urls, container, false);
        new LoadBlockedUrlAsyncTask(getContext(), providerId).execute();

        SwipeRefreshLayout swipeContainer = view.findViewById(R.id.providerListSwipeContainer);
        swipeContainer.setOnRefreshListener(() -> {
            providerId = null;
            new LoadBlockedUrlAsyncTask(getContext(), null).execute();
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.list_provider_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
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
                new FilterUrlAsyncTask(text, providerId, getContext()).execute();
                return false;
            }
        });
    }

    private static class LoadBlockedUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<Context> contextReference;
        private AppDatabase appDatabase;
        private Long providerId;

        LoadBlockedUrlAsyncTask(Context context, Long providerId) {
            this.contextReference = new WeakReference<>(context);
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.providerId = providerId;
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            return providerId == null ?
                    BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                    BlockUrlUtils.getBlockedUrls(providerId, appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> blockedUrls) {
            Context context = contextReference.get();
            if (context != null) {
                ListView listView = ((Activity)context).findViewById(R.id.blocked_url_list);
                if (listView != null) {
                    ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, blockedUrls);
                    listView.setAdapter(itemsAdapter);
                }

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.providerListSwipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }

                TextView totalBlockedUrls = ((Activity)context).findViewById(R.id.total_blocked_urls);
                if (totalBlockedUrls != null) {
                    totalBlockedUrls.setText(String.format("%s%s",
                            context.getString(R.string.total_domains), String.valueOf(blockedUrls.size())));
                }
            }
        }
    }

    private static class FilterUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<Context> contextReference;
        private AppDatabase appDatabase;
        private String text;
        private Long providerId;

        FilterUrlAsyncTask(String text, Long providerId, Context context) {
            this.text = text;
            this.providerId = providerId;
            this.contextReference = new WeakReference<>(context);
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            final String filterText = '%' + text + '%';
            if (providerId == null) {
                return text.isEmpty() ? BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                        BlockUrlUtils.getFilteredBlockedUrls(filterText, appDatabase);
            }
            return text.isEmpty() ? BlockUrlUtils.getBlockedUrls(providerId, appDatabase) :
                    BlockUrlUtils.getFilteredBlockedUrls(filterText, providerId, appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> list) {
            Context context = contextReference.get();
            if (context != null) {
                ListView listView = ((Activity)context).findViewById(R.id.blocked_url_list);
                if (listView != null) {
                    ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, list);
                    listView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
