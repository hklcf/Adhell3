package com.fusionjack.adhell3.utils;

import android.content.ComponentName;
import android.os.Environment;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

public final class AppComponentFactory {

    private static final String SERVICE_FILENAME = "adhell3_services.txt";
    private static final String RECEIVER_FILENAME = "adhell3_receivers.txt";
    private static AppComponentFactory instance;

    private ApplicationPolicy appPolicy;
    private AppDatabase appDatabase;

    private AppComponentFactory() {
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static AppComponentFactory getInstance() {
        if (instance == null) {
            instance = new AppComponentFactory();
        }
        return instance;
    }

    public Single<String> processAppComponentInBatch(boolean enabled) {
        Set<String> serviceNames;
        try {
            serviceNames = getFileContent(SERVICE_FILENAME);
        } catch (IOException e) {
            return Single.error(e);
        }

        Set<String> receiverNames;
        try {
            receiverNames = getFileContent(RECEIVER_FILENAME);
        } catch (IOException e) {
            return Single.error(e);
        }

        return Single.create(emitter -> {
            if (enabled) {
                enableServices(serviceNames);
                enableReceivers(receiverNames);
            } else {
                disableServices(serviceNames);
                disableReceivers(receiverNames);
            }
            emitter.onSuccess("Success!");
        });
    }

    private Set<String> getFileContent(String fileName) throws IOException {
        File serviceFile = new File(Environment.getExternalStorageDirectory(), fileName);
        if (!serviceFile.exists()) {
            throw new FileNotFoundException("File name " + fileName + " cannot be found");
        }

        Set<String> lines = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(serviceFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        return lines;
    }

    private void enableServices(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            appDatabase.appPermissionDao().delete(packageName, compName);
                        }
                    }
                }
            }
        }
    }

    private void disableServices(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appService = new AppPermission();
                            appService.packageName = packageName;
                            appService.permissionName = compName;
                            appService.permissionStatus = AppPermission.STATUS_SERVICE;
                            appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appService);
                        }
                    }
                }
            }
        }
    }

    private void enableReceivers(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getReceiverNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (!compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, true);
                        if (success) {
                            String receiverPair = compName + "|Auto";
                            appDatabase.appPermissionDao().delete(packageName, receiverPair);
                        }
                    }
                }
            }
        }
    }

    private void disableReceivers(Set<String> compNames) {
        List<AppInfo> apps = appDatabase.applicationInfoDao().getUserApps();
        for (AppInfo app : apps) {
            String packageName = app.packageName;
            Set<String> availableServiceNames = AppComponent.getReceiverNames(packageName);
            for (String compName : compNames) {
                if (availableServiceNames.contains(compName)) {
                    boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                    if (compState) {
                        ComponentName componentName = new ComponentName(packageName, compName);
                        boolean success = appPolicy.setApplicationComponentState(componentName, false);
                        if (success) {
                            AppPermission appReceiver = new AppPermission();
                            appReceiver.packageName = packageName;
                            appReceiver.permissionName = compName + "|Auto";
                            appReceiver.permissionStatus = AppPermission.STATUS_RECEIVER;
                            appReceiver.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.appPermissionDao().insert(appReceiver);
                        }
                    }
                }
            }
        }
    }
}
