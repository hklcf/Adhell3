package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(
        tableName = "FirewallWhitelistedPackage",
        indices = {@Index("policyPackageId")},
        foreignKeys = @ForeignKey(entity = PolicyPackage.class,
                parentColumns = "id",
                childColumns = "policyPackageId")
)
public class FirewallWhitelistedPackage {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "policyPackageId")
    public String policyPackageId;

    public FirewallWhitelistedPackage() {
    }

    @Ignore
    public FirewallWhitelistedPackage(String packageName, String policyPackageId) {
        this.packageName = packageName;
        this.policyPackageId = policyPackageId;
    }

}
