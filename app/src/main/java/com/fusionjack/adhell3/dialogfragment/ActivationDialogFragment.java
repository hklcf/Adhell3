package com.fusionjack.adhell3.dialogfragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
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
    private static final String TAG = ActivationDialogFragment.class.getCanonicalName();
    BroadcastReceiver receiver;
    private DeviceAdminInteractor deviceAdminInteractor;
    private Single<String> knoxKeyObservable;
    private Button turnOnAdminButton;
    private Button activateKnoxButton;
    private CompositeDisposable disposable;
    private FragmentManager fragmentManager;
    private SharedPreferences sharedPreferences;

    public ActivationDialogFragment() {
        deviceAdminInteractor = DeviceAdminInteractor.getInstance();
        knoxKeyObservable = Single.create(emmiter -> {
            String knoxKey;
            try {
                knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
                emmiter.onSuccess(knoxKey);
            } catch (Throwable e) {
                emmiter.onError(e);
                Log.e(TAG, "Failed to get knox key", e);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);

        View view = inflater.inflate(R.layout.dialog_fragment_activation, container);
        turnOnAdminButton = view.findViewById(R.id.turnOnAdminButton);
        activateKnoxButton = view.findViewById(R.id.activateKnoxButton);

        fragmentManager = getActivity().getSupportFragmentManager();
        turnOnAdminButton.setOnClickListener(v ->
                deviceAdminInteractor.forceEnableAdmin(this.getActivity())
        );

        EditText knoxKeyEditText = view.findViewById(R.id.knoxKeyEditText);
        String knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
        if (knoxKey != null) {
            knoxKeyEditText.setText(knoxKey);
        }

        EditText backwardKeyEditText = view.findViewById(R.id.backwardKeyEditText);
        boolean useBackwardKey = deviceAdminInteractor.useBackwardCompatibleKey();
        if (useBackwardKey) {
            String backwardKey = deviceAdminInteractor.getBackwardKey(sharedPreferences);
            if (backwardKey != null) {
                backwardKeyEditText.setText(backwardKey);
            }
        } else {
            backwardKeyEditText.setVisibility(View.GONE);
        }

        activateKnoxButton.setOnClickListener(v -> {
            deviceAdminInteractor.setKnoxKey(sharedPreferences, knoxKeyEditText.getText().toString());
            deviceAdminInteractor.setBackwardKey(sharedPreferences, backwardKeyEditText.getText().toString());

            allowActivateKnox(false);
            activateKnoxButton.setText(R.string.activating_knox_license);

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

                        deviceAdminInteractor.activateKnoxKey(sharedPreferences, getContext());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        allowActivateKnox(true);
                        activateKnoxButton.setText(R.string.activate_license);
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
                            allowActivateKnox(false);
                            activateKnoxButton.setText(R.string.license_activated);
                            Log.d(TAG, "License activated");
                            dismiss();
                        }
                    } else {
                        getActivity().unregisterReceiver(receiver);
                        String status = intent.getStringExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                        Toast.makeText(context, "Status: " +  status + ". Error code: " + errorCode, Toast.LENGTH_LONG).show();

                        // Allow the user to try again
                        allowActivateKnox(true);
                        activateKnoxButton.setText(R.string.activate_license);
                        Log.w(TAG, "License activation failed");
                    }
                }

                if (EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    getActivity().unregisterReceiver(receiver);

                    int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                        allowActivateKnox(false);
                        activateKnoxButton.setText(R.string.license_activated);
                        Log.d(TAG, "License activated");
                        dismiss();
                    } else  {
                        String status = intent.getStringExtra(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                        Toast.makeText(context, "Status: " +  status + ". Error code: " + errorCode, Toast.LENGTH_LONG).show();

                        // Allow the user to try again
                        allowActivateKnox(true);
                        activateKnoxButton.setText(R.string.activate_license);
                        Log.w(TAG, "License activation failed");
                    }
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        disposable = new CompositeDisposable();

        boolean adminActive = deviceAdminInteractor.isAdminActive();
        if (adminActive) {
            allowTurnOnAdmin(false);
            turnOnAdminButton.setText(R.string.admin_enabled);
            adminActive = true;
        } else {
            allowTurnOnAdmin(true);
            turnOnAdminButton.setText(R.string.enable_admin);
            adminActive = false;
        }

        boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
        if (knoxEnabled) {
            activateKnoxButton.setText(R.string.license_activated);
            allowActivateKnox(false);
            knoxEnabled = true;
        } else {
            activateKnoxButton.setText(R.string.activate_license);
            if (!adminActive) {
                allowActivateKnox(false);
            } else {
                allowActivateKnox(true);
            }
            knoxEnabled = false;
        }

        if (adminActive && knoxEnabled) {
            dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog){
        super.onDismiss(dialog);
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                .commit();
    }

    private void allowActivateKnox(boolean isAllowed) {
        activateKnoxButton.setEnabled(isAllowed);
        activateKnoxButton.setClickable(isAllowed);
    }

    private void allowTurnOnAdmin(boolean isAllowed) {
        turnOnAdminButton.setClickable(isAllowed);
        turnOnAdminButton.setEnabled(isAllowed);
    }
}
