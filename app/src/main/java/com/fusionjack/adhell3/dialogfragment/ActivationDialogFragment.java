package com.fusionjack.adhell3.dialogfragment;


import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.tasks.BackupDatabaseAsyncTask;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class ActivationDialogFragment extends DialogFragment {
    private DeviceAdminInteractor deviceAdminInteractor;
    private Completable knoxKeyObservable;
    private CompletableObserver knoxKeyObserver;
    private BroadcastReceiver receiver;
    private Button turnOnAdminButton;
    private Button activateKnoxButton;
    private SharedPreferences sharedPreferences;
    private EditText knoxKeyEditText;

    public static final String DIALOG_TAG = "activation_dialog";

    public ActivationDialogFragment() {
        deviceAdminInteractor = DeviceAdminInteractor.getInstance();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == KnoxEnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent, context);
                    } else {
                        handleError(intent, context, errorCode);
                    }
                }

                if (EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent, context);
                    } else  {
                        handleError(intent, context, errorCode);
                    }
                }
            }
        };

        knoxKeyObservable = Completable.create(emmiter -> {
            try {
                emmiter.onComplete();
            } catch (Throwable e) {
                emmiter.onError(e);
            }
        });

        knoxKeyObserver = new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                filter.addAction(EnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                getActivity().registerReceiver(receiver, filter);
            }

            @Override
            public void onComplete() {
                boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
                if (knoxEnabled) {
                    try {
                        deviceAdminInteractor.deactivateKnoxKey(sharedPreferences, getContext());
                    } catch (Exception ex) {
                        Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                        setLicenseState(true);
                    }
                } else {
                    deviceAdminInteractor.activateKnoxKey(sharedPreferences, getContext());
                }
            }

            @Override
            public void onError(Throwable e) {
                getActivity().unregisterReceiver(receiver);
                setLicenseState(false);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @android.support.annotation.NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        deviceAdminInteractor.setKnoxKey(sharedPreferences, BuildConfig.SKL_KEY);
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@android.support.annotation.NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_fragment_activation, container);

        turnOnAdminButton = view.findViewById(R.id.turnOnAdminButton);
        activateKnoxButton = view.findViewById(R.id.activateKnoxButton);
        knoxKeyEditText = view.findViewById(R.id.knoxKeyEditText);

        String knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
        knoxKeyEditText.setText(knoxKey);

        turnOnAdminButton.setOnClickListener(v ->
                deviceAdminInteractor.forceEnableAdmin(this.getActivity())
        );

        activateKnoxButton.setOnClickListener(v -> {
            deviceAdminInteractor.setKnoxKey(sharedPreferences, knoxKeyEditText.getText().toString());

            disableActiveButton();
            boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
            if (knoxEnabled) {
                activateKnoxButton.setText(R.string.deactivating_knox_license);
            } else {
                activateKnoxButton.setText(R.string.activating_knox_license);
            }

            knoxKeyObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(knoxKeyObserver);
        });

        Button backupButton = view.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.backup_database_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.backup_database_dialog_text);

            new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                            new BackupDatabaseAsyncTask(getActivity()).execute()
                    )
                    .setNegativeButton(android.R.string.no, null).show();
        });

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
            TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
            titlTextView.setText(R.string.delete_app_dialog_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.delete_app_dialog_text);

            Context context = getContext();
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        ContentBlocker contentBlocker = ContentBlocker56.getInstance();
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

        return view;
    }

    private void handleResult(Intent intent, Context context) {
        getActivity().unregisterReceiver(receiver);

        int result_type = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                setLicenseState(true);
                LogUtils.info("License activated");
                Toast.makeText(context, "License activated", Toast.LENGTH_LONG).show();
                dismiss();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                        .commit();
            } else if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_DEACTIVATION) {
                setLicenseState(false);
                LogUtils.info("License deactivated");
                Toast.makeText(context, "License deactivated", Toast.LENGTH_LONG).show();
                setCancelable(false);
            }
        }

        result_type = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == EnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                setLicenseState(true);
                LogUtils.info("License activated");
                Toast.makeText(context, "License activated", Toast.LENGTH_LONG).show();
                dismiss();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                        .commit();
            }
        }
    }

    private void handleError(Intent intent, Context context, int errorCode) {
        getActivity().unregisterReceiver(receiver);

        if (intent != null) {
            String status = intent.getStringExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            if (status == null || status.isEmpty()) {
                status = intent.getStringExtra(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            }
            LogUtils.error("Status: " + status + ". Error code: " + errorCode);
            Toast.makeText(context, "Status: " + status + ". Error code: " + errorCode, Toast.LENGTH_LONG).show();
        }

        // Allow the user to try again
        setLicenseState(false);
        LogUtils.error( "License activation failed");
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean adminActive = deviceAdminInteractor.isAdminActive();
        if (adminActive) {
            setAdminState(true);
            boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
            if (knoxEnabled) {
                setLicenseState(true);
            } else {
                setLicenseState(false);
            }
        } else {
            setAdminState(false);
            disableActiveButton();
        }
    }

    private void setAdminState(boolean enabled) {
        if (enabled) {
            turnOnAdminButton.setText(R.string.admin_enabled);
        } else {
            turnOnAdminButton.setText(R.string.enable_admin);
        }
        turnOnAdminButton.setClickable(!enabled);
        turnOnAdminButton.setEnabled(!enabled);
    }

    private void setLicenseState(boolean isActivated) {
        if (isActivated) {
            activateKnoxButton.setText(R.string.deactivate_license);
        } else {
            activateKnoxButton.setText(R.string.activate_license);
        }
        activateKnoxButton.setEnabled(true);
        activateKnoxButton.setClickable(true);
        knoxKeyEditText.setEnabled(!isActivated);
    }

    private void disableActiveButton() {
        activateKnoxButton.setEnabled(false);
        activateKnoxButton.setClickable(false);
        knoxKeyEditText.setEnabled(false);
    }
}
