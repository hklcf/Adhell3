package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.tasks.LoadAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;

import java.lang.ref.WeakReference;

public class SettingsFragment extends Fragment {
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        ContentBlocker contentBlocker = DeviceAdminInteractor.getInstance().getContentBlocker();
        Button deleteAppButton = view.findViewById(R.id.deleteApp);
        deleteAppButton.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.delete_app_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_app_dialog_text);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        contentBlocker.disableDomainRules();
                        contentBlocker.disableFirewallRules();
                        ComponentName devAdminReceiver = new ComponentName(context, CustomDeviceAdminReceiver.class);
                        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.removeActiveAdmin(devAdminReceiver);
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        String packageName = "package:" + BuildConfig.APPLICATION_ID;
                        intent.setData(Uri.parse(packageName));
                        startActivity(intent);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        Button backupDatabaseButton = view.findViewById(R.id.backup_database);
        backupDatabaseButton.setOnClickListener(view1 -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.backup_database_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.backup_database_dialog_text);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                            new BackupDatabaseAsyncTask(getActivity()).execute()
                    )
                    .setNegativeButton(android.R.string.no, null).show();
        });

        Button restoreDatabaseButton = view.findViewById(R.id.restore_database);
        restoreDatabaseButton.setOnClickListener(view2 -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.restore_database_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.restore_database_dialog_text);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                            new RestoreDatabaseAsyncTask(getActivity()).execute()
                    )
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }

    private static class BackupDatabaseAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;

        BackupDatabaseAsyncTask(Activity activity) {
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

    private static class RestoreDatabaseAsyncTask extends AsyncTask<Void, String, String> {
        private ProgressDialog dialog;
        private AlertDialog.Builder builder;
        private WeakReference<Activity> activityWeakReference;

        RestoreDatabaseAsyncTask(Activity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
            this.builder = new AlertDialog.Builder(activity);
            this.dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Restore database is running...");
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            try {
                DeviceAdminInteractor.getInstance().getContentBlocker().disableDomainRules();
                DeviceAdminInteractor.getInstance().getContentBlocker().disableFirewallRules();
                AppPreferences.getInstance().setAppDisabler(false);
                AdhellFactory.getInstance().applyAppDisabler();

                DatabaseFactory.getInstance().restoreDatabase();

                publishProgress("Updating all providers...");
                AdhellFactory.getInstance().updateAllProviders();

                publishProgress("Disabling app components...");
                AdhellFactory.getInstance().setAppComponentState(false);

                publishProgress("Setting DNS addresses...");
                AdhellFactory.getInstance().applyDns(null);
                new LoadAppAsyncTask("", AppFlag.createDnsFlag(), activityWeakReference.get()).execute();

                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(String message) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (message == null) {
                builder.setMessage("Restore database is finished. Turn on Adhell.");
                builder.setTitle("Info");
            } else {
                builder.setMessage(message);
                builder.setTitle("Error");
            }
            builder.create().show();
        }
    }
}
