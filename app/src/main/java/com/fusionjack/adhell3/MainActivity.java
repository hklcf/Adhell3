package com.fusionjack.adhell3;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import static com.fusionjack.adhell3.fragments.SettingsFragment.SET_NIGHT_MODE_PREFERENCE;

public class MainActivity extends AppCompatActivity {
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    private FragmentManager fragmentManager;
    private ActivationDialogFragment activationDialogFragment;
    private AlertDialog passwordDialog;
    private BottomNavigationView bottomBar;
    private int selectedTabId = -1;
    private boolean doubleBackToExitPressedOnce = false;
    private String themeChange;

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
        SharedPreferences  mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
            getDelegate().setDefaultNightMode(getDelegate().MODE_NIGHT_YES);
        }
        else {
            getDelegate().setDefaultNightMode(getDelegate().MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        themeChange = getIntent().getStringExtra("settingsFragment");

        // Remove elevation shadow of ActionBar
        getSupportActionBar().setElevation(0);

        // Change status bar icon tint based on theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            if (!mPrefs.getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decor.setSystemUiVisibility(0);
            }
        }

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
            LogUtils.info("Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });

        // Nasty workaround to show text and icon if the tab count more than 3
        removeShiftMode(bottomBar);
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

        // Show password dialog if password has been set and wait until the user enter the password
        if (isPasswordShowing()) {
            return;
        }

        // Check whether Knox is still valid. Show activation dialog if it is not valid anymore.
        if (!isKnoxValid()) {
            return;
        }

        LogUtils.info("Everything is okay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.info("Destroying activity");
    }

    private void onTabSelected(int tabId) {
        LogUtils.info( "Tab '" + tabId + "' is selected");
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
                if(themeChange != null){
                    if (themeChange.matches(SET_NIGHT_MODE_PREFERENCE)){
                        Bundle bundle = new Bundle();
                        bundle.putString("viewpager_position", "Settings");
                        replacing.setArguments(bundle);
                    }
                }
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
                LogUtils.info( "Showing password dialog");
                passwordDialog.show();
            }
            return true;
        }
        return false;
    }

    private boolean isKnoxValid() {
        if (!DeviceAdminInteractor.getInstance().isAdminActive()) {
            LogUtils.info( "Admin is not active, showing activation dialog");
            if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        if (!DeviceAdminInteractor.getInstance().isKnoxEnabled(this)) {
            LogUtils.info( "Knox is disabled, showing activation dialog");
            LogUtils.info( "Check if internet connection exists");
            boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(this);
            if (!hasInternetAccess) {
                AdhellFactory.getInstance().createNoInternetConnectionDialog(this);
            }
            if (!isActivationDialogVisible()) {
                activationDialogFragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
            }
            return false;
        }

        // Select the Home tab manually if nothing is selected
        if (selectedTabId == -1) {
            if (themeChange != null) {
                if (themeChange.matches(SET_NIGHT_MODE_PREFERENCE)){
                    bottomBar.setSelectedItemId(R.id.othersTab);
                    onTabSelected(R.id.othersTab);

                }
                else {
                    onTabSelected(R.id.homeTab);
                }
            }
            else {
                onTabSelected(R.id.homeTab);
            }
        }

        return true;
    }

    private boolean isActivationDialogVisible() {
        Fragment activationDialog = getSupportFragmentManager().findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
        return activationDialog != null;
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
            LogUtils.error( "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            LogUtils.error( "Unable to change value of shift mode", e);
        }
    }
}
