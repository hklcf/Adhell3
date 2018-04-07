package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.fusionjack.adhell3.db.entity.AppInfo;

import java.util.List;

@Dao
public interface AppInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppInfo> apps);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppInfo info);

    @Query("DELETE FROM AppInfo")
    void deleteAll();


    @Query("SELECT * FROM AppInfo WHERE packageName = :packageName")
    AppInfo getByPackageName(String packageName);

    @Query("DELETE FROM AppInfo WHERE packageName = :packageName")
    void deleteAppInfoByPackageName(String packageName);

    @Query("SELECT MAX(id) FROM AppInfo")
    long getMaxId();

    @Query("SELECT * FROM AppInfo WHERE adhellWhitelisted = 1")
    List<AppInfo> getWhitelistedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAllSortedByWhitelist();

    @Query("SELECT * FROM AppInfo WHERE system = 0 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getUserApps();

    @Query("SELECT * FROM AppInfo ORDER BY appName ASC")
    List<AppInfo> getAll();

    @Query("SELECT * FROM AppInfo ORDER BY installTime DESC")
    List<AppInfo> getAllRecentSort();

    @Query("SELECT * FROM AppInfo ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllSortedByDisabled();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY appName ASC")
    List<AppInfo> getAllAppsWithStrInName(String str);

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY installTime DESC")
    List<AppInfo> getAllAppsWithStrInNameTimeOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllAppsWithStrInNameDisabledOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE disabled = 1")
    List<AppInfo> getDisabledApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledApps();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledAppsAlphabetically(String str);

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY installTime DESC")
    List<AppInfo> getEnabledAppsInTimeOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY installTime DESC")
    List<AppInfo> getEnabledAppsInTimeOrder(String str);

    @Query("SELECT * FROM AppInfo ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getEnableAppsByMobileRestricted();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getEnableAppsByMobileRestricted(String str);

    @Query("SELECT * FROM AppInfo WHERE mobileRestricted = 1 AND disabled = 0")
    List<AppInfo> getMobileRestrictedApps();

    @Update
    void update(AppInfo appInfo);

    /*@Query("DELETE FROM AppInfo WHERE id = :id")
    void deleteAppInfoById(int id);

    @Query("SELECT * FROM AppInfo WHERE packageName LIKE :str ORDER BY appName ASC")
    List<AppInfo> getAllPackagesWithStrInName(String str);

    @Query("SELECT * FROM AppInfo WHERE packageName LIKE :str ORDER BY installTime DESC")
    List<AppInfo> getAllPackagesWithStrInNameAlphOrder(String str);*/
}