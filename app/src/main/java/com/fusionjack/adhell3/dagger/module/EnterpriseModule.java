package com.fusionjack.adhell3.dagger.module;

import android.content.Context;
import android.support.annotation.Nullable;

import com.fusionjack.adhell3.dagger.scope.AdhellApplicationScope;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;
import com.samsung.android.knox.net.firewall.Firewall;

import dagger.Module;
import dagger.Provides;

@Module(includes = {AppModule.class})
public class EnterpriseModule {

    @Nullable
    @Provides
    @AdhellApplicationScope
    KnoxEnterpriseLicenseManager providesKnoxEnterpriseLicenseManager(Context appContext) {
        try {
            LogUtils.info( "Trying to get EnterpriseLicenseManager");
            return KnoxEnterpriseLicenseManager.getInstance(appContext);
        } catch (Throwable e) {
            LogUtils.error( "Failed to get EnterpriseLicenseManager. So it seems that Knox is not supported on this device", e);
        }
        return null;
    }

    @Nullable
    @Provides
    @AdhellApplicationScope
    EnterpriseLicenseManager providesEnterpriseLicenseManager(Context appContext) {
        try {
            LogUtils.info( "Trying to get EnterpriseLicenseManager");
            return EnterpriseLicenseManager.getInstance(appContext);
        } catch (Throwable e) {
            LogUtils.error( "Failed to get EnterpriseLicenseManager. So it seems that Knox is not supported on this device", e);
        }
        return null;
    }

    @Nullable
    @Provides
    @AdhellApplicationScope
    EnterpriseDeviceManager providesEnterpriseDeviceManager(Context appContext) {
        try {
            LogUtils.info( "Trying to get EnterpriseDeviceManager");
            return EnterpriseDeviceManager.getInstance(appContext);
        } catch (Throwable e) {
            LogUtils.error( "Failed to get EnterpriseDeviceManager", e);
            return null;
        }
    }

    @Nullable
    @Provides
    @AdhellApplicationScope
    ApplicationPolicy providesApplicationPolicy(@Nullable EnterpriseDeviceManager enterpriseDeviceManager) {
        if (enterpriseDeviceManager == null) {
            return null;
        }
        return enterpriseDeviceManager.getApplicationPolicy();
    }

    @Nullable
    @Provides
    @AdhellApplicationScope
    Firewall providesFirewall(@Nullable EnterpriseDeviceManager enterpriseDeviceManager) {
        if (enterpriseDeviceManager == null) {
            LogUtils.info( "enterpriseDeviceManager is null. Can't get firewall");
            return null;
        }
        try {
            LogUtils.info( "Trying to get Firewall");
            return enterpriseDeviceManager.getFirewall();
        } catch (Throwable throwable) {
            LogUtils.error( "Failed to get firewall", throwable);
        }
        return null;
    }

}
