package com.fusionjack.adhell3.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;


@Entity(
        tableName = "BlockUrl",
        indices = {@Index("urlProviderId")},
        foreignKeys = @ForeignKey(
                entity = BlockUrlProvider.class,
                parentColumns = "_id",
                childColumns = "urlProviderId",
                onDelete = ForeignKey.CASCADE
        )
)
public class BlockUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "urlProviderId")
    public long urlProviderId;

    public BlockUrl(String url, long urlProviderId) {
        this.url = BlockUrlPatternsMatch.getValidKnoxUrl(url);
        this.urlProviderId = urlProviderId;
    }
}
