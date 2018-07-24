package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;

import java.lang.ref.WeakReference;
import java.util.Date;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.BLACKLIST_PAGE;
import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.WHITELIST_PAGE;

public class AddUrlAsyncTask extends AsyncTask<Void, Void, Void> {
    private String url;
    private int page;
    private WeakReference<Context> contextWeakReference;

    public AddUrlAsyncTask(String url, int page, Context context) {
        this.url = BlockUrlPatternsMatch.getValidKnoxUrl(url);
        this.page = page;
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        switch (page) {
            case BLACKLIST_PAGE:
                UserBlockUrl userBlockUrl = new UserBlockUrl(url, new Date());
                url = userBlockUrl.url;
                appDatabase.userBlockUrlDao().insert(userBlockUrl);
                break;
            case WHITELIST_PAGE:
                WhiteUrl whiteUrl = new WhiteUrl(url, new Date());
                url = whiteUrl.url;
                appDatabase.whiteUrlDao().insert(whiteUrl);
                break;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Context context = contextWeakReference.get();
        if (context != null) {
            ListView listView = null;
            switch (page) {
                case BLACKLIST_PAGE:
                    listView = ((Activity) context).findViewById(R.id.blackListView);
                    break;

                case WHITELIST_PAGE:
                    listView = ((Activity) context).findViewById(R.id.whiteListView);
                    break;
            }

            if (listView != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                adapter.add(url);
                adapter.notifyDataSetChanged();
            }

            if (url.indexOf('|') == -1) {
                Toast.makeText(context, "網址已新增", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "規則已新增", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
