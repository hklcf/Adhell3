package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Date;

@Entity(
        tableName = "WhiteUrl"
)
@TypeConverters(DateConverter.class)
public class WhiteUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public String url;

    public Date insertedAt;

    @Ignore
    public WhiteUrl(String url) {
        this.url = url;
        this.insertedAt = new Date();
    }

    public WhiteUrl() {
    }
}
