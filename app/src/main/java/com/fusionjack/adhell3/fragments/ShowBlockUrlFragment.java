package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;
import java.util.List;

public class ShowBlockUrlFragment extends Fragment {
    private AppDatabase appDatabase;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appDatabase = AppDatabase.getAppDatabase(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_blocked_urls, container, false);
        setHasOptionsMenu(true);

        new LoadBlockedUrlAsyncTask(getContext(), getArguments(), appDatabase).execute();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                new FilterUrlAsyncTask(text, getArguments(), getContext(), appDatabase).execute();
                return false;
            }
        });
    }

    private static class LoadBlockedUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<Context> contextReference;
        private Bundle bundle;
        private AppDatabase appDatabase;

        LoadBlockedUrlAsyncTask(Context context, Bundle bundle, AppDatabase appDatabase) {
            this.contextReference = new WeakReference<>(context);
            this.bundle = bundle;
            this.appDatabase = appDatabase;
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            return bundle == null ?
                    BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                    BlockUrlUtils.getBlockedUrls(bundle.getLong("provider"), appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> blockedUrls) {
            Context context = contextReference.get();
            if (context != null) {
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context,
                        android.R.layout.simple_list_item_1, blockedUrls);
                ListView listView = ((Activity)context).findViewById(R.id.blocked_url_list);
                listView.setAdapter(itemsAdapter);

                TextView totalBlockedUrls = ((Activity)context).findViewById(R.id.total_blocked_urls);
                totalBlockedUrls.setText(String.format("%s%s",
                        context.getString(R.string.total_blocked_urls), String.valueOf(blockedUrls.size())));
            }
        }
    }

    private static class FilterUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<Context> contextReference;
        private AppDatabase appDatabase;
        private String text;
        private Bundle bundle;

        FilterUrlAsyncTask(String text, Bundle bundle, Context context, AppDatabase appDatabase) {
            this.text = text;
            this.bundle = bundle;
            this.contextReference = new WeakReference<>(context);
            this.appDatabase = appDatabase;
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            final String filterText = '%' + text + '%';
            if (bundle == null) {
                return text.isEmpty() ? BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                        BlockUrlUtils.getFilteredBlockedUrls(filterText, appDatabase);
            }

            final long providerId = bundle.getLong("provider");
            return text.isEmpty() ? BlockUrlUtils.getBlockedUrls(providerId, appDatabase) :
                    BlockUrlUtils.getFilteredBlockedUrls(filterText, providerId, appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> list) {
            Context context = contextReference.get();
            if (context != null) {
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context,
                        android.R.layout.simple_list_item_1, list);
                ListView listView = ((Activity)context).findViewById(R.id.blocked_url_list);
                listView.setAdapter(itemsAdapter);
                listView.invalidateViews();
            }
        }
    }
}
