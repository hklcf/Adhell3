package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;


@Entity(
        tableName = "AppInfo",
        indices = {@Index("appName"), @Index("installTime"), @Index("disabled"), @Index("mobileRestricted")}
)
@TypeConverters(DateConverter.class)
public class AppInfo {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "appName")
    public String appName;

    @ColumnInfo(name = "installTime")
    public long installTime;

    @ColumnInfo(name = "system")
    public boolean system;

    @ColumnInfo(name = "adhellWhitelisted")
    public boolean adhellWhitelisted;

    @ColumnInfo(name = "disabled")
    public boolean disabled;

    @ColumnInfo(name = "mobileRestricted")
    public boolean mobileRestricted;

    @ColumnInfo(name = "wifiRestricted")
    public boolean wifiRestricted;
}
