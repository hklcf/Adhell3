package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;


@Entity(
        tableName = "AppPermission",
        indices = {@Index("policyPackageId")},
        foreignKeys = @ForeignKey(entity = PolicyPackage.class,
                parentColumns = "id",
                childColumns = "policyPackageId")
)
public class AppPermission {

    @Ignore
    public static final int STATUS_PERMISSION = -1;

    @Ignore
    public static final int STATUS_SERVICE = 2;

    @Ignore
    public static final int STATUS_RECEIVER = 5;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "permissionName")
    public String permissionName;

    @ColumnInfo(name = "permissionStatus")
    public int permissionStatus;

    @ColumnInfo(name = "policyPackageId")
    public String policyPackageId;
}
