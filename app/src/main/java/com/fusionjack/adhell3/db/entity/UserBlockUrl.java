package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Date;

@Entity(
        tableName = "UserBlockUrl"
)
@TypeConverters(DateConverter.class)
public class UserBlockUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public String url;

    public Date insertedAt;

    public UserBlockUrl() {
    }

    @Ignore
    public UserBlockUrl(String url) {
        this.insertedAt = new Date();
        this.url = url;
    }
}
