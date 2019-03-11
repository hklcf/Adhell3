package com.fusionjack.adhell3.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.fusionjack.adhell3.db.DatabaseFactory;

public class BackupDatabaseAsyncTask extends AsyncTask<Void, Void, String> {
    private ProgressDialog dialog;
    private AlertDialog.Builder builder;

    public BackupDatabaseAsyncTask(Activity activity) {
        dialog = new ProgressDialog(activity);
        builder = new AlertDialog.Builder(activity);
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Backup database is running...");
        dialog.show();
    }

    @Override
    protected String doInBackground(Void... args) {
        try {
            DatabaseFactory.getInstance().backupDatabase();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String message) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        if (message == null) {
            builder.setMessage("Backup database is finished");
            builder.setTitle("Info");
        } else {
            builder.setMessage(message);
            builder.setTitle("Error");
        }
        builder.create().show();
    }
}
