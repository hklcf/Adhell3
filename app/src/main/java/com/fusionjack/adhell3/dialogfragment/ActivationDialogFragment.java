package com.fusionjack.adhell3.dialogfragment;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class ActivationDialogFragment extends DialogFragment {
    private BroadcastReceiver receiver;
    private DeviceAdminInteractor deviceAdminInteractor;
    private Single<String> knoxKeyObservable;
    private Button turnOnAdminButton;
    private Button activateKnoxButton;
    private CompositeDisposable disposable;
    private SharedPreferences sharedPreferences;
    private EditText knoxKeyEditText;
    private EditText backwardKeyEditText;

    public ActivationDialogFragment() {
        deviceAdminInteractor = DeviceAdminInteractor.getInstance();
        knoxKeyObservable = Single.create(emmiter -> {
            String knoxKey;
            try {
                knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
                emmiter.onSuccess(knoxKey);
            } catch (Throwable e) {
                emmiter.onError(e);
                LogUtils.error( "Failed to get knox key", e);
            }
        });
    }

    @android.support.annotation.NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        deviceAdminInteractor.setKnoxKey(sharedPreferences, BuildConfig.SKL_KEY);
        deviceAdminInteractor.setBackwardKey(sharedPreferences, BuildConfig.BACKWARDS_KEY);
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@android.support.annotation.NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_fragment_activation, container);

        turnOnAdminButton = view.findViewById(R.id.turnOnAdminButton);
        activateKnoxButton = view.findViewById(R.id.activateKnoxButton);
        knoxKeyEditText = view.findViewById(R.id.knoxKeyEditText);
        backwardKeyEditText = view.findViewById(R.id.backwardKeyEditText);

        String knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
        knoxKeyEditText.setText(knoxKey);

        boolean useBackwardKey = deviceAdminInteractor.useBackwardCompatibleKey();
        if (useBackwardKey) {
            String backwardKey = deviceAdminInteractor.getBackwardKey(sharedPreferences);
            backwardKeyEditText.setText(backwardKey);
        } else {
            backwardKeyEditText.setVisibility(View.GONE);
        }

        turnOnAdminButton.setOnClickListener(v ->
                deviceAdminInteractor.forceEnableAdmin(this.getActivity())
        );

        activateKnoxButton.setOnClickListener(v -> {
            deviceAdminInteractor.setKnoxKey(sharedPreferences, knoxKeyEditText.getText().toString());
            deviceAdminInteractor.setBackwardKey(sharedPreferences, backwardKeyEditText.getText().toString());

            disableActiveButton();
            boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
            if (knoxEnabled) {
                activateKnoxButton.setText(R.string.deactivating_knox_license);
            } else {
                activateKnoxButton.setText(R.string.activating_knox_license);
            }

            Disposable subscribe = knoxKeyObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<String>() {

                    @Override
                    public void onSuccess(@NonNull String knoxKey) {
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                        filter.addAction(EnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                        getActivity().registerReceiver(receiver, filter);

                        if (knoxEnabled) {
                            deviceAdminInteractor.deactivateKnoxKey(sharedPreferences, getContext());
                        } else {
                            deviceAdminInteractor.activateKnoxKey(sharedPreferences, getContext());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        setLicenseState(true);
                    }
                });

            disposable.add(subscribe);
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == KnoxEnterpriseLicenseManager.ERROR_NONE) {
                        boolean useBackwardKey = deviceAdminInteractor.useBackwardCompatibleKey();
                        if (useBackwardKey) {
                            deviceAdminInteractor.activateBackwardKey(sharedPreferences, getContext());
                        } else {
                            getActivity().unregisterReceiver(receiver);
                            handleResult(intent);
                        }
                    } else {
                        getActivity().unregisterReceiver(receiver);
                        handleError(intent, context, errorCode);
                    }
                }

                if (EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    getActivity().unregisterReceiver(receiver);

                    int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent);
                    } else  {
                        handleError(intent, context, errorCode);
                    }
                }
            }
        };

        return view;
    }

    private void handleResult(Intent intent) {
        int result_type = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
            setLicenseState(true);
            LogUtils.info("License activated");
            dismiss();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                    .commit();
        } else if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_DEACTIVATION) {
            setLicenseState(false);
            LogUtils.info("License deactivated");
            setCancelable(false);
        }
    }

    private void handleError(Intent intent, Context context, int errorCode) {
        String status = intent.getStringExtra(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
        Toast.makeText(context, "Status: " +  status + ". Error code: " + errorCode, Toast.LENGTH_LONG).show();

        // Allow the user to try again
        setLicenseState(false);
        LogUtils.info( "License activation failed");
    }

    @Override
    public void onResume() {
        super.onResume();
        disposable = new CompositeDisposable();

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

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
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
        backwardKeyEditText.setEnabled(!isActivated);
    }

    private void disableActiveButton() {
        activateKnoxButton.setEnabled(false);
        activateKnoxButton.setClickable(false);
        knoxKeyEditText.setEnabled(false);
        backwardKeyEditText.setEnabled(false);
    }
}
