package com.fusionjack.adhell3.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface UserBlockUrlDao {
    @Query("SELECT * FROM UserBlockUrl")
    LiveData<List<UserBlockUrl>> getAll();

    @Query("SELECT * FROM UserBlockUrl")
    List<UserBlockUrl> getAll2();

    @Query("SELECT url FROM UserBlockUrl")
    List<String> getAll3();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserBlockUrl userBlockUrl);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserBlockUrl> userBlockUrls);

    @Delete
    void delete(UserBlockUrl userBlockUrl);

    @Query("DELETE FROM UserBlockUrl WHERE url = :url")
    void deleteByUrl(String url);

    @Query("DELETE FROM UserBlockUrl")
    void deleteAll();

    @Query("SELECT * FROM UserBlockUrl WHERE url LIKE :url")
    List<UserBlockUrl> getByUrl(String url);
}
