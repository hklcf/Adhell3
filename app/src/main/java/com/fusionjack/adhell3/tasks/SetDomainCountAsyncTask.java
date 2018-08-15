package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;

public class SetDomainCountAsyncTask extends AsyncTask<Void, Integer, Integer> {

    private WeakReference<Context> contextWeakReference;

    public SetDomainCountAsyncTask(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        return BlockUrlUtils.getAllBlockedUrlsCount(appDatabase);
    }

    @Override
    protected void onPostExecute(Integer count) {
        Context context = contextWeakReference.get();
        if (context != null) {
            TextView infoTextView = ((Activity) context).findViewById(R.id.infoTextView);
            if (infoTextView != null) {
                String strFormat = context.getResources().getString(R.string.total_unique_domains);
                infoTextView.setText(String.format(strFormat, count));
            }
        }
    }
}
