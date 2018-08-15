package com.fusionjack.adhell3;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.fragments.AppTabFragment;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.fragments.DomainTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;
import com.roughike.bottombar.BottomBar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    protected DeviceAdminInteractor adminInteractor;
    private FragmentManager fragmentManager;
    private ActivationDialogFragment activationDialogFragment;
    private AlertDialog passwordDialog;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean mainViewInitiated = false;

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count <= 1) {
            if (doubleBackToExitPressedOnce) {
                finish();
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press once again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        } else {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the crash handler to log crash's stack trace into a file
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());
        }

        fragmentManager = getSupportFragmentManager();
    }

    private void init() {
        adminInteractor = DeviceAdminInteractor.getInstance();
        activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);

        if (!adminInteractor.isSupported()) {
            Log.i(TAG, "Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        setContentView(R.layout.activity_main);
        BottomBar bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setTabTitleTextAppearance(R.style.bottomBarTextView);
        bottomBar.setOnTabSelectListener(tabId -> {
            if (adminInteractor.isAdminActive() && adminInteractor.isKnoxEnabled(this)) {
                onTabSelected(tabId);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                fragmentManager.popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String passwordHash = AppPreferences.getInstance().getPasswordHash();
        if (!passwordHash.isEmpty()) {
            if (passwordDialog == null || !passwordDialog.isShowing()) {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_password, findViewById(android.R.id.content), false);
                passwordDialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, null)
                        .create();

                passwordDialog.setOnShowListener(dialogInterface -> {
                    Button button = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(view -> {
                        EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                        String password = passwordEditText.getText().toString();
                        try {
                            if (PasswordStorage.verifyPassword(password, passwordHash)) {
                                passwordDialog.dismiss();
                                if (!mainViewInitiated) {
                                    init();
                                }
                            } else {
                                TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                                infoTextView.setText(R.string.dialog_wrong_password);
                            }
                        } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
                            e.printStackTrace();
                        }
                    });
                });
                passwordDialog.setCancelable(false);
                passwordDialog.show();
            }
        } else {
            if (!mainViewInitiated) {
                init();
            }
        }

        if (!mainViewInitiated) {
            return;
        }

        if (!adminInteractor.isAdminActive()) {
            Log.d(TAG, "Admin is not active. Request enabling");
            if (!activationDialogFragment.isVisible()) {
                activationDialogFragment.show(fragmentManager, "dialog_fragment_activation_adhell");
            }
            return;
        }

        if (!adminInteractor.isKnoxEnabled(this)) {
            Log.d(TAG, "Knox disabled");

            Log.d(TAG, "Checking if internet connection exists");
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                Log.d(TAG, "Is internet connection exists: " + isConnected);
                if (!isConnected) {
                    AdhellFactory.getInstance().createNoInternetConnectionDialog(this);
                }
            }

            if (!activationDialogFragment.isVisible()) {
                activationDialogFragment.show(fragmentManager, "dialog_fragment_activation_adhell");
            }
        }
        Log.d(TAG, "Everything is okay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying activity");
        LogUtils.getInstance().close();
    }

    private void onTabSelected(int tabId) {
        fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment replacing;
        switch (tabId) {
            case R.id.homeTab:
                replacing = new HomeTabFragment();
                break;
            case R.id.appsManagementTab:
                replacing = new AppTabFragment();
                break;
            case R.id.domainsTab:
                replacing = new DomainTabFragment();
                break;
            case R.id.othersTab:
                replacing = new OtherTabFragment();
                break;
            default:
                replacing = new HomeTabFragment();
        }

        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, replacing)
                .addToBackStack(BACK_STACK_TAB_TAG)
                .commit();
    }
}
