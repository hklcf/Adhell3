package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface ReportBlockedUrlDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReportBlockedUrl reportBlockedUrl);

    @Insert
    void insertAll(List<ReportBlockedUrl> reportBlockedUrls);

    @Query("SELECT * FROM ReportBlockedUrl WHERE blockDate BETWEEN :startDate AND :endDate ORDER BY _id DESC")
    List<ReportBlockedUrl> getReportBlockUrlBetween(long startDate, long endDate);

    @Query("DELETE FROM ReportBlockedUrl WHERE blockDate < :blockDate")
    void deleteBefore(long blockDate);

    @Query("SELECT * FROM ReportBlockedUrl ORDER BY blockDate DESC LIMIT 1")
    ReportBlockedUrl getLastBlockedDomain();

}
