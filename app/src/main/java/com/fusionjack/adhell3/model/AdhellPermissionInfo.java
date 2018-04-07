package com.fusionjack.adhell3.model;

import java.util.List;

public class AdhellPermissionInfo {
    public final String name;
    public final String label;
    private static List<AdhellPermissionInfo> mPermissionList = null;

    public AdhellPermissionInfo(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public static void cachePermissionList(List<AdhellPermissionInfo> permissionList) {
        mPermissionList = null;
        mPermissionList = permissionList;
    }

    public static List<AdhellPermissionInfo> getPermissionList() {
        return mPermissionList;
    }
}
