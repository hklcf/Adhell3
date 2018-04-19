package com.fusionjack.adhell3.model;

import android.content.pm.PermissionInfo;
import android.os.Build;

import java.util.List;

public class AdhellPermissionInfo {
    public final String name;
    public final String label;
    private final int level;
    private static List<AdhellPermissionInfo> mPermissionList = null;

    public AdhellPermissionInfo(String name, String label, int level) {
        this.name = name;
        this.label = label;
        this.level = fixProtectionLevel(level);
    }

    public static void cachePermissionList(List<AdhellPermissionInfo> permissionList) {
        mPermissionList = null;
        mPermissionList = permissionList;
    }

    public static List<AdhellPermissionInfo> getPermissionList() {
        return mPermissionList;
    }

    public String getProtectionLevelLabel() {
        String protLevel = "unknown";
        switch (level & PermissionInfo.PROTECTION_MASK_BASE) {
            case PermissionInfo.PROTECTION_DANGEROUS:
                protLevel = "dangerous";
                break;
            case PermissionInfo.PROTECTION_NORMAL:
                protLevel = "normal";
                break;
            case PermissionInfo.PROTECTION_SIGNATURE:
                protLevel = "signature";
                break;
            case PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM:
                protLevel = "signatureOrSystem";
                break;
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_PRIVILEGED) != 0) {
            protLevel += "|privileged";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0) {
            protLevel += "|development";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_APPOP) != 0) {
            protLevel += "|appop";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_PRE23) != 0) {
            protLevel += "|pre23";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_INSTALLER) != 0) {
            protLevel += "|installer";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_VERIFIER) != 0) {
            protLevel += "|verifier";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_PREINSTALLED) != 0) {
            protLevel += "|preinstalled";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_SETUP) != 0) {
            protLevel += "|setup";
        }
        if ((level & PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY) != 0) {
            protLevel += "|runtime";
        }
        return protLevel;
    }

    private int fixProtectionLevel(int level) {
        if (level == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                level = PermissionInfo.PROTECTION_SIGNATURE | PermissionInfo.PROTECTION_FLAG_PRIVILEGED;
            }
        }
        return level;
    }
}
