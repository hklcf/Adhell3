package com.fusionjack.adhell3.model;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class AppComponent {

    public static List<IComponentInfo> getPermissions(String packageName) {
        List<String> permissionNameList = new ArrayList<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (packageInfo != null) {
                String[] permissions = packageInfo.requestedPermissions;
                if (permissions != null) {
                    permissionNameList.addAll(Arrays.asList(permissions));
                }
            }
            Collections.sort(permissionNameList);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        List<IComponentInfo> permissionList = new ArrayList<>();
        for (String permissionName : permissionNameList) {
            try {
                android.content.pm.PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
                CharSequence description = info.loadDescription(packageManager);
                permissionList.add(new PermissionInfo(permissionName,
                        description == null ? "No description" : description.toString(),
                        info.protectionLevel, packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        return permissionList;
    }

    public static List<IComponentInfo> getServices(String packageName) {
        Set<String> serviceNameList = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedServices = appDatabase.appPermissionDao().getServices(packageName);
        for (AppPermission storedService : storedServices) {
            serviceNameList.add(storedService.permissionName);
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            if (packageInfo != null) {
                android.content.pm.ServiceInfo[] services = packageInfo.services;
                if (services != null) {
                    for (android.content.pm.ServiceInfo serviceInfo : services) {
                        serviceNameList.add(serviceInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        List<IComponentInfo> serviceInfoList = new ArrayList<>();
        for (String serviceName : serviceNameList) {
            serviceInfoList.add(new ServiceInfo(packageName, serviceName));
        }

        return serviceInfoList;
    }

    public static List<IComponentInfo> getReceivers(String packageName) {
        Set<ReceiverPair> receiverNameList = new HashSet<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        // Disabled services won't be appear in the package manager anymore
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<AppPermission> storedReceivers = appDatabase.appPermissionDao().getReceivers(packageName);
        for (AppPermission storedReceiver : storedReceivers) {
            StringTokenizer tokenizer = new StringTokenizer(storedReceiver.permissionName, "|");
            String name = tokenizer.nextToken();
            String permission = tokenizer.nextToken();
            receiverNameList.add(new ReceiverPair(name, permission));
        }

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
            if (packageInfo != null) {
                ActivityInfo[] receivers = packageInfo.receivers;
                if (receivers != null) {
                    for (ActivityInfo activityInfo : receivers) {
                        receiverNameList.add(new ReceiverPair(activityInfo.name, activityInfo.permission));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        List<IComponentInfo> receiverInfoList = new ArrayList<>();
        for (ReceiverPair pair : receiverNameList) {
            receiverInfoList.add(new ReceiverInfo(packageName, pair.getName(), pair.getPermission()));
        }

        return receiverInfoList;
    }

    private static class ReceiverPair {
        private String name;
        private String permission;

        ReceiverPair(String name, String permission) {
            this.name = name;
            this.permission = permission;
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }
    }
}
