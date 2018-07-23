package com.fusionjack.adhell3.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.fusionjack.adhell3.db.entity.BlockUrlProvider;

import java.util.List;

@Dao
public interface BlockUrlProviderDao {

    @Query("SELECT * FROM BlockUrlProviders ORDER BY deletable ASC")
    List<BlockUrlProvider> getAll2();

    @Query("SELECT * FROM BlockUrlProviders ORDER BY deletable ASC")
    LiveData<List<BlockUrlProvider>> getAll();

    @Query("SELECT * FROM BlockUrlProviders WHERE selected = :selected")
    List<BlockUrlProvider> getBlockUrlProviderBySelectedFlag(int selected);

    @Query("SELECT DISTINCT BlockUrl.url FROM BlockUrlProviders INNER JOIN BlockUrl ON BlockUrl.urlProviderId = BlockUrlProviders._id WHERE selected = 1 ORDER BY BlockUrl.url")
    List<String> getUniqueBlockedUrls();

    @Query("SELECT COUNT(DISTINCT BlockUrl.url) FROM BlockUrlProviders INNER JOIN BlockUrl ON BlockUrl.urlProviderId = BlockUrlProviders._id WHERE selected = 1 ORDER BY BlockUrl.url")
    int getUniqueBlockedUrlsCount();

    @Query("SELECT * FROM BlockUrlProviders WHERE url = :url")
    BlockUrlProvider getByUrl(String url);

    @Query("SELECT * FROM BlockUrlProviders WHERE _id = :id")
    BlockUrlProvider getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(BlockUrlProvider... urlProviders);

    @Update
    void updateBlockUrlProviders(BlockUrlProvider... blockUrlProviders);

    @Delete
    void delete(BlockUrlProvider blockUrlProvider);

    @Query("SELECT * FROM BlockUrlProviders WHERE deletable = 0")
    List<BlockUrlProvider> getDefault();

    @Query("DELETE FROM BlockUrlProviders WHERE deletable = 0")
    void deleteDefault();

    @Query("DELETE FROM BlockUrlProviders")
    void deleteAll();

}
