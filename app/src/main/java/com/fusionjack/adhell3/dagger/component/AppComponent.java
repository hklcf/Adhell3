package com.fusionjack.adhell3.dagger.component;

import com.fusionjack.adhell3.adapter.AdhellPermissionInAppsAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker20;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.dagger.module.AdminModule;
import com.fusionjack.adhell3.dagger.module.AppModule;
import com.fusionjack.adhell3.dagger.module.EnterpriseModule;
import com.fusionjack.adhell3.dagger.module.NetworkModule;
import com.fusionjack.adhell3.dagger.scope.AdhellApplicationScope;
import com.fusionjack.adhell3.fragments.AdhellPermissionInAppsFragment;
import com.fusionjack.adhell3.fragments.AdhellPermissionInfoFragment;
import com.fusionjack.adhell3.fragments.AdhellReportsFragment;
import com.fusionjack.adhell3.fragments.AppFlag;
import com.fusionjack.adhell3.fragments.BlockedUrlSettingFragment;
import com.fusionjack.adhell3.fragments.BlockerFragment;
import com.fusionjack.adhell3.fragments.MobileRestricterFragment;
import com.fusionjack.adhell3.fragments.PackageDisablerFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AppsListDBInitializer;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.viewmodel.AdhellWhitelistAppsViewModel;
import com.fusionjack.adhell3.viewmodel.SharedAppPermissionViewModel;

import dagger.Component;

@AdhellApplicationScope
@Component(modules = {AppModule.class, AdminModule.class, EnterpriseModule.class, NetworkModule.class})
public interface AppComponent {
    void inject(DeviceAdminInteractor deviceAdminInteractor);

    void inject(ContentBlocker56 contentBlocker56);

    void inject(ContentBlocker20 contentBlocker20);

    void inject(AdhellReportsFragment adhellReportsFragment);

    void inject(BlockedUrlSettingFragment blockedUrlSettingFragment);

    void inject(PackageDisablerFragment packageDisablerFragment);

    void inject(MobileRestricterFragment mobileRestricterFragment);

    void inject(AdhellWhitelistAppsViewModel adhellWhitelistAppsViewModel);

    void inject(SharedAppPermissionViewModel sharedAppPermissionViewModel);

    void inject(AdhellPermissionInAppsAdapter adhellPermissionInAppsAdapter);

    void inject(AppsListDBInitializer appsListDBInitializer);

    void inject(BlockerFragment blockerFragment);

    void inject(AdhellAppIntegrity adhellAppIntegrity);

    void inject(AdhellPermissionInfoFragment adhellPermissionInfoFragment);

    void inject(AdhellPermissionInAppsFragment adhellPermissionInAppsFragment);

    void inject(DatabaseFactory databaseFactory);

    void inject(AppFlag flag);

}
