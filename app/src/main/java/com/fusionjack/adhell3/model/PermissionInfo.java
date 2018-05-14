package com.fusionjack.adhell3.model;

import android.os.Build;

public class PermissionInfo implements IComponentInfo {
    public final String name;
    public final String label;
    private final int level;
    private String packageName;

    PermissionInfo(String name, String label, int level, String packageName) {
        this.name = name;
        this.label = label;
        this.level = fixProtectionLevel(level);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getProtectionLevelLabel() {
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

    private int fixProtectionLevel(int level) {
        if (level == android.content.pm.PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                level = android.content.pm.PermissionInfo.PROTECTION_SIGNATURE | android.content.pm.PermissionInfo.PROTECTION_FLAG_PRIVILEGED;
            }
        }
        return level;
    }
}
