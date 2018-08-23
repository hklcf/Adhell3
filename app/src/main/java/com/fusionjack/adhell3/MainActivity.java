package com.fusionjack.adhell3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.fragments.AppTabFragment;
import com.fusionjack.adhell3.fragments.DomainTabFragment;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.fragments.OtherTabFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.CrashHandler;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    private FragmentManager fragmentManager;
    private ActivationDialogFragment activationDialogFragment;
    private AlertDialog passwordDialog;
    private int selectedTabId = -1;
    private boolean doubleBackToExitPressedOnce = false;

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
        activationDialogFragment = new ActivationDialogFragment();
        activationDialogFragment.setCancelable(false);
        passwordDialog = createPasswordDialog();

        // Early exit if the device doesn't support Knox
        if (!DeviceAdminInteractor.getInstance().isSupported()) {
            Log.i(TAG, "Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });

        // Nasty workaround to show text and icon if the tab count more than 3
        removeShiftMode(bottomBar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show password dialog if password has been set and wait until the user enter the password
        if (isPasswordShowing()) {
            return;
        }

        // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
        if (!isKnoxValid()) {
            return;
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
        Log.d(TAG, "Tab '" + tabId + "' is selected");
        fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment replacing;
        switch (tabId) {
            case R.id.homeTab:
                selectedTabId = R.id.homeTab;
                replacing = new HomeTabFragment();
                break;
            case R.id.appsManagementTab:
                selectedTabId = R.id.appsManagementTab;
                replacing = new AppTabFragment();
                break;
            case R.id.domainsTab:
                selectedTabId = R.id.domainsTab;
                replacing = new DomainTabFragment();
                break;
            case R.id.othersTab:
                selectedTabId = R.id.othersTab;
                replacing = new OtherTabFragment();
                break;
            default:
                selectedTabId = -1;
                replacing = new HomeTabFragment();
        }

        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, replacing)
                .addToBackStack(BACK_STACK_TAB_TAG)
                .commit();
    }

    private boolean isPasswordShowing() {
        String passwordHash = AppPreferences.getInstance().getPasswordHash();
        if (!passwordHash.isEmpty()) {
            if (!passwordDialog.isShowing()) {
                Log.d(TAG, "Showing password dialog");
                passwordDialog.show();
            }
            return true;
        }
        return false;
    }

    private boolean isKnoxValid() {
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            Log.d(TAG, "Admin is not active, showing activation dialog");
            if (!activationDialogFragment.isVisible()) {
                activationDialogFragment.show(fragmentManager, "dialog_fragment_activation_adhell");
            }
            return false;
        }

        if (!DeviceAdminInteractor.getInstance().isKnoxEnabled(this)) {
            Log.d(TAG, "Knox is disabled, showing activation dialog");
            Log.d(TAG, "Check if internet connection exists");
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
            return false;
        }

        // Select the Home tab manually if nothing is selected
        if (selectedTabId == -1) {
            onTabSelected(R.id.homeTab);
        }

        return true;
    }

    private AlertDialog createPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_password, findViewById(android.R.id.content), false);
        AlertDialog passwordDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(false)
                .create();

        passwordDialog.setOnShowListener(dialogInterface -> {
            Button button = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                String password = passwordEditText.getText().toString();
                try {
                    TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                    String passwordHash = AppPreferences.getInstance().getPasswordHash();
                    if (PasswordStorage.verifyPassword(password, passwordHash)) {
                        infoTextView.setText(R.string.dialog_enter_password_summary);
                        passwordEditText.setText("");
                        passwordDialog.dismiss();
                        isKnoxValid();
                    } else {
                        infoTextView.setText(R.string.dialog_wrong_password);
                    }
                } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
                    e.printStackTrace();
                }
            });
        });

        return passwordDialog;
    }

    @SuppressLint("RestrictedApi")
    private void removeShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BottomNav", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BottomNav", "Unable to change value of shift mode", e);
        }
    }
}
