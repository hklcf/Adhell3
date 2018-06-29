package com.fusionjack.adhell3.utils;

import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public final class AppPermissionUtils {

    private AppPermissionUtils() {
    }

    public static boolean isDangerousLevel(int level) {
        level = level & android.content.pm.PermissionInfo.PROTECTION_MASK_BASE;
        return level == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS;
    }

    public static List<String> getSiblingPermissions(String permissionName) {
        List<String> permissionNames = new ArrayList<>();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        try {
            android.content.pm.PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
            List<android.content.pm.PermissionInfo> permissions = packageManager.queryPermissionsByGroup(info.group, PackageManager.GET_META_DATA);
            for (android.content.pm.PermissionInfo permission : permissions) {
                permissionNames.add(permission.name);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return permissionNames;
    }

    public static String getProtectionLevelLabel(int level) {
        String protLevel = "unknown";
        switch (level & android.content.pm.PermissionInfo.PROTECTION_MASK_BASE) {
            case android.content.pm.PermissionInfo.PROTECTION_DANGEROUS:
                protLevel = "dangerous";
                break;
            case android.content.pm.PermissionInfo.PROTECTION_NORMAL:
                protLevel = "normal";
                break;
            case android.content.pm.PermissionInfo.PROTECTION_SIGNATURE:
                protLevel = "signature";
                break;
            case android.content.pm.PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM:
                protLevel = "signatureOrSystem";
                break;
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_PRIVILEGED) != 0) {
            protLevel += "|privileged";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0) {
            protLevel += "|development";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_APPOP) != 0) {
            protLevel += "|appop";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_PRE23) != 0) {
            protLevel += "|pre23";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_INSTALLER) != 0) {
            protLevel += "|installer";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_VERIFIER) != 0) {
            protLevel += "|verifier";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_PREINSTALLED) != 0) {
            protLevel += "|preinstalled";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_SETUP) != 0) {
            protLevel += "|setup";
        }
        if ((level & android.content.pm.PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY) != 0) {
            protLevel += "|runtime";
        }
        return protLevel;
    }

    public static int fixProtectionLevel(int level) {
        if (level == android.content.pm.PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                level = android.content.pm.PermissionInfo.PROTECTION_SIGNATURE | android.content.pm.PermissionInfo.PROTECTION_FLAG_PRIVILEGED;
            }
        }
        return level;
    }
}
