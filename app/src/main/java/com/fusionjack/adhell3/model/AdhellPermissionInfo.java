package com.fusionjack.adhell3.model;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.AsyncTask;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class AdhellPermissionInfo {
    public final String name;
    public final String label;
    private static List<AdhellPermissionInfo> permissionList = null;

    private AdhellPermissionInfo(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public static List<AdhellPermissionInfo> createPermissions(AppDatabase appDatabase, PackageManager packageManager) {
        if (permissionList != null && permissionList.size() > 0) {
            return permissionList;
        }
        try {
            permissionList = new CreatePermissionsAsyncTask(appDatabase, packageManager).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return permissionList;
    }

    public static List<AppInfo> getAppsByPermission(String permissionName, AppDatabase appDatabase, PackageManager packageManager) {
        try {
            return new GetAppsByPermissionAsyncTask(permissionName, appDatabase, packageManager).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static class CreatePermissionsAsyncTask extends AsyncTask<Void, Void, List<AdhellPermissionInfo>> {
        private PackageManager packageManager;
        private AppDatabase appDatabase;
        private List<AdhellPermissionInfo> permissionList = new ArrayList<>();

        CreatePermissionsAsyncTask(AppDatabase appDatabase, PackageManager packageManager) {
            this.packageManager = packageManager;
            this.appDatabase = appDatabase;
        }

        @Override
        protected List<AdhellPermissionInfo> doInBackground(Void... voids) {
            Set<String> permissionNameList = new HashSet<>();

            List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
            for (AppInfo userApp : userApps) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(userApp.packageName, PackageManager.GET_PERMISSIONS);
                    if (packageInfo != null) {
                        String[] permissions = packageInfo.requestedPermissions;
                        if (permissions != null) {
                            for (String permissionName : permissions) {
                                if (permissionName.startsWith("android.permission.") ||
                                        permissionName.startsWith("com.android.")) {
                                    permissionNameList.add(permissionName);
                                }
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            List<String> sortedPermissionNameList = new ArrayList<>(permissionNameList);
            Collections.sort(sortedPermissionNameList);
            for (String permissionName : sortedPermissionNameList) {
                try {
                    PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
                    CharSequence description = info.loadDescription(packageManager);
                    permissionList.add(new AdhellPermissionInfo(permissionName, description == null ? "No description" : description.toString()));
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            return permissionList;
        }
    }

    private static class GetAppsByPermissionAsyncTask extends AsyncTask<Void, Void, List<AppInfo>> {
        private String permissionName;
        private AppDatabase appDatabase;
        private PackageManager packageManager;
        private List<AppInfo> permissionsApps = new ArrayList<>();

        GetAppsByPermissionAsyncTask(String permissionName, AppDatabase appDatabase, PackageManager packageManager) {
            this.permissionName = permissionName;
            this.appDatabase = appDatabase;
            this.packageManager = packageManager;
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
            for (AppInfo userApp : userApps) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(userApp.packageName, PackageManager.GET_PERMISSIONS);
                    if (packageInfo != null) {
                        String[] permissions = packageInfo.requestedPermissions;
                        if (permissions != null) {
                            for (String permissionName : permissions) {
                                if (permissionName.equalsIgnoreCase(this.permissionName)) {
                                    permissionsApps.add(userApp);
                                    break;
                                }
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            return permissionsApps;
        }
    }
}
