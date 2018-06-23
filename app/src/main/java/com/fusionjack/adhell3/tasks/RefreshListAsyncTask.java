package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.BLACKLIST_PAGE;
import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.WHITELIST_PAGE;

public class RefreshListAsyncTask extends AsyncTask<Void, Void, List<String>> {
    private int page;
    private WeakReference<Context> contextWeakReference;

    public RefreshListAsyncTask(int page, Context context) {
        this.page = page;
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected List<String> doInBackground(Void... voids) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<String> domainList = null;
        switch (page) {
            case BLACKLIST_PAGE:
                domainList = appDatabase.userBlockUrlDao().getAll3();
                break;
            case WHITELIST_PAGE:
                domainList = appDatabase.whiteUrlDao().getAll3();
                break;
        }
        return domainList;
    }

    @Override
    protected void onPostExecute(List<String> domainList) {
        Context context = contextWeakReference.get();
        if (context != null) {
            ListView listView = null;
            SwipeRefreshLayout swipeContainer = null;
            switch (page) {
                case BLACKLIST_PAGE:
                    listView = ((Activity) context).findViewById(R.id.blackListView);
                    swipeContainer = ((Activity) context).findViewById(R.id.blacklistSwipeContainer);
                    break;
                case WHITELIST_PAGE:
                    listView = ((Activity) context).findViewById(R.id.whiteListView);
                    swipeContainer = ((Activity) context).findViewById(R.id.whitelistSwipeContainer);
                    break;
            }
            if (listView != null) {
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, domainList);
                listView.setAdapter(itemsAdapter);
            }
            if (swipeContainer != null) {
                swipeContainer.setRefreshing(false);
            }
        }
    }
}
