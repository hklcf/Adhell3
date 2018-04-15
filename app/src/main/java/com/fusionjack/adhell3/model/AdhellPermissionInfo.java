package com.fusionjack.adhell3.model;

import android.content.pm.PermissionInfo;

import java.util.List;
import java.util.Properties;

public class AdhellPermissionInfo {
    public final String name;
    public final String label;
    private final int protectionLevel;
    private static List<AdhellPermissionInfo> mPermissionList = null;

    public AdhellPermissionInfo(String name, String label, int protectionLevel) {
        this.name = name;
        this.label = label;
        this.protectionLevel = protectionLevel;
    }

    public static void cachePermissionList(List<AdhellPermissionInfo> permissionList) {
        mPermissionList = null;
        mPermissionList = permissionList;
    }

    public static List<AdhellPermissionInfo> getPermissionList() {
        return mPermissionList;
    }

    public static boolean includePermission(int protectionLevel) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return protectionLevel == PermissionInfo.PROTECTION_NORMAL ||
                    protectionLevel == PermissionInfo.PROTECTION_DANGEROUS ||
                    protectionLevel == PermissionInfo.PROTECTION_SIGNATURE ||
                    protectionLevel == (PermissionInfo.PROTECTION_SIGNATURE | PermissionInfo.PROTECTION_FLAG_PRIVILEGED);
        } else {
            return protectionLevel == PermissionInfo.PROTECTION_NORMAL ||
                    protectionLevel == PermissionInfo.PROTECTION_DANGEROUS ||
                    protectionLevel == PermissionInfo.PROTECTION_SIGNATURE;
        }
    }

    public String getProtectionLevelLabel() {
        switch (protectionLevel) {
            case PermissionInfo.PROTECTION_NORMAL:
                return "normal";
            case PermissionInfo.PROTECTION_DANGEROUS:
                return "dangerous";
                case PermissionInfo.PROTECTION_SIGNATURE:
                return "signature";
            case PermissionInfo.PROTECTION_SIGNATURE | PermissionInfo.PROTECTION_FLAG_PRIVILEGED:
                return "signature|privileged";
        }
        return "Unknown";
    }
}
