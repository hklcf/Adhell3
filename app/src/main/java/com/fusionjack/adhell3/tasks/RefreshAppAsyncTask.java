package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.model.AppFlag;

import java.lang.ref.WeakReference;

public class RefreshAppAsyncTask extends AsyncTask<Void, Void, Void> {
    private WeakReference<Context> contextReference;
    private AppFlag appFlag;

    public RefreshAppAsyncTask(AppFlag appFlag, Context context) {
        this.appFlag = appFlag;
        this.contextReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Context context = contextReference.get();
        if (context != null) {
            SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(appFlag.getRefreshLayout());
            if (swipeContainer != null) {
                swipeContainer.setRefreshing(false);
            }

            new LoadAppAsyncTask("", appFlag, true, context).execute();
        }
    }
}
