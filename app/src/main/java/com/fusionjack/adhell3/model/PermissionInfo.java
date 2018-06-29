package com.fusionjack.adhell3.model;

import com.fusionjack.adhell3.utils.AppPermissionUtils;

public class PermissionInfo implements IComponentInfo {
    public final String name;
    public final String label;
    private final int level;
    private String packageName;

    PermissionInfo(String name, String label, int level, String packageName) {
        this.name = name;
        this.label = label;
        this.level = AppPermissionUtils.fixProtectionLevel(level);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public int getLevel() {
        return level;
    }
}
