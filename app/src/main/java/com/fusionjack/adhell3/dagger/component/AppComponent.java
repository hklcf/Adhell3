package com.fusionjack.adhell3.dagger.component;

import com.fusionjack.adhell3.dagger.module.AdminModule;
import com.fusionjack.adhell3.dagger.module.AppModule;
import com.fusionjack.adhell3.dagger.module.EnterpriseModule;
import com.fusionjack.adhell3.dagger.scope.AdhellApplicationScope;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;

import dagger.Component;

@AdhellApplicationScope
@Component(modules = {AppModule.class, AdminModule.class, EnterpriseModule.class})
public interface AppComponent {
    void inject(DeviceAdminInteractor deviceAdminInteractor);
    void inject(AdhellFactory adhellFactory);
}
