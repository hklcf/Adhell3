package com.fusionjack.adhell3.utils;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Nullable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import java.io.File;

import javax.inject.Inject;

import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_2_6;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_2_7;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_2_7_1;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_2_8;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_2_9;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_3_0;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_3_1;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_3_2;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_3_2_1;
import static com.samsung.android.knox.EnterpriseDeviceManager.KNOX_VERSION_CODES.KNOX_3_3;

public final class DeviceAdminInteractor {
    private static final int RESULT_ENABLE = 42;

    private static DeviceAdminInteractor instance;

    private final String KNOX_KEY = "knox_key";

    private static final String KNOX_FIREWALL_PERMISSION = "com.samsung.android.knox.permission.KNOX_FIREWALL";
    private static final String KNOX_APP_MGMT_PERMISSION = "com.samsung.android.knox.permission.KNOX_APP_MGMT";
    private static final String MDM_FIREWALL_PERMISSION = "android.permission.sec.MDM_FIREWALL";
    private static final String MDM_APP_MGMT_PERMISSION = "android.permission.sec.MDM_APP_MGMT";

    @Nullable
    @Inject
    KnoxEnterpriseLicenseManager knoxEnterpriseLicenseManager;

    @Nullable
    @Inject
    EnterpriseLicenseManager enterpriseLicenseManager;

    @Nullable
    @Inject
    EnterpriseDeviceManager enterpriseDeviceManager;

    @Nullable
    @Inject
    DevicePolicyManager devicePolicyManager;

    @Nullable
    @Inject
    ApplicationPolicy applicationPolicy;

    @Inject
    ComponentName componentName;

    private DeviceAdminInteractor() {
        App.get().getAppComponent().inject(this);
    }

    public static DeviceAdminInteractor getInstance() {
        if (instance == null) {
            instance = new DeviceAdminInteractor();
        }
        return instance;
    }

    /**
     * Check if admin enabled
     *
     * @return void
     */
    public boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(componentName);
    }

    /**
     * Force user to enadle administrator
     */
    public void forceEnableAdmin(Context context) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Policy provider");
        ((Activity) context).startActivityForResult(intent, RESULT_ENABLE);
    }

    public void activateKnoxKey(SharedPreferences sharedPreferences, Context context) {
        String knoxKey = getKnoxKey(sharedPreferences);
        if (knoxKey != null) {
            if (knoxKey.startsWith("KLM")) {
                activateKLMKey(context, knoxKey);
            } else {
                activateELMKey(context, knoxKey);
            }
        }
    }

    public void deactivateKnoxKey(SharedPreferences sharedPreferences, Context context) throws Exception {
        String knoxKey = getKnoxKey(sharedPreferences);
        if (knoxKey != null) {
            if (!knoxKey.startsWith("KLM")) {
                throw new Exception("You cannot deactivate ELM key");
            }
            KnoxEnterpriseLicenseManager.getInstance(context).deActivateLicense(knoxKey);
        }
    }

    private void activateKLMKey(Context context, String key) {
        KnoxEnterpriseLicenseManager.getInstance(context).activateLicense(key);
    }

    private void activateELMKey(Context context, String key) {
        EnterpriseLicenseManager.getInstance(context).activateLicense(key);
    }

    /**
     * Check if KNOX enabled
     */
    public boolean isKnoxEnabled(Context context) {
        if (isKnox26()) {
            return context.checkCallingOrSelfPermission(MDM_FIREWALL_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                    context.checkCallingOrSelfPermission(MDM_APP_MGMT_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        }
        return context.checkCallingOrSelfPermission(KNOX_FIREWALL_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                context.checkCallingOrSelfPermission(KNOX_APP_MGMT_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public String getKnoxKey(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(KNOX_KEY, BuildConfig.SKL_KEY);
    }

    public void setKnoxKey(SharedPreferences sharedPreferences, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KNOX_KEY, key);
        editor.apply();
    }

    public boolean installApk(String pathToApk) {
        if (applicationPolicy == null) {
            LogUtils.info( "applicationPolicy variable is null");
            return false;
        }
        try {
            File file = new File(pathToApk);
            if (!file.exists()) {
                LogUtils.info( "apk fail does not exist: " + pathToApk);
                return false;
            }

            boolean result = applicationPolicy.installApplication(pathToApk, false);
            LogUtils.info( "Is Application installed: " + result);
            return result;
        } catch (Throwable e) {
            LogUtils.error( "Failed to install application", e);
            return false;
        }
    }

    public boolean isSupported() {
        return isSamsung() && isKnoxSupported() && isKnoxVersionSupported();
    }

    private boolean isSamsung() {
        LogUtils.info( "Device manufacturer: " + Build.MANUFACTURER);
        return Build.MANUFACTURER.equals("samsung");
    }

    private boolean isKnoxVersionSupported() {
        if (enterpriseDeviceManager == null) {
            LogUtils.info( "Knox is not supported: enterpriseDeviceManager is null");
            return false;
        }

        int apiLevel = EnterpriseDeviceManager.getAPILevel();
        LogUtils.info( "Knox API level: " + apiLevel);
        switch (apiLevel) {
            case KNOX_2_6:
            case KNOX_2_7:
            case KNOX_2_7_1:
            case KNOX_2_8:
            case KNOX_2_9:
            case KNOX_3_0:
            case KNOX_3_1:
            case KNOX_3_2:
            case KNOX_3_2_1:
            case KNOX_3_3:
                return true;
            default:
                return false;
        }
    }

    private boolean isKnoxSupported() {
        if (knoxEnterpriseLicenseManager == null || enterpriseLicenseManager == null) {
            LogUtils.info( "Knox is not supported: knoxEnterpriseLicenseManager or enterpriseLicenseManager is null");
            return false;
        }
        LogUtils.info( "Knox is supported");
        return true;
    }

    private boolean isKnox26() {
        return EnterpriseDeviceManager.getAPILevel() == KNOX_2_6;
    }
}
