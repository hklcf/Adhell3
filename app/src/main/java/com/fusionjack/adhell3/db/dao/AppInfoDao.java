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
    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppInfo> apps);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppInfo info);

    // Delete
    @Query("DELETE FROM AppInfo")
    void deleteAll();

    @Query("DELETE FROM AppInfo WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    // Update
    @Update
    void update(AppInfo appInfo);


    // Get based on appName/packageName
    @Query("SELECT * FROM AppInfo ORDER BY appName ASC")
    List<AppInfo> getAppsAlphabetically();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY appName ASC")
    List<AppInfo> getAppsAlphabetically(String str);

    @Query("SELECT * FROM AppInfo WHERE packageName = :packageName")
    AppInfo getAppByPackageName(String packageName);


    // Get based on installTime
    @Query("SELECT * FROM AppInfo ORDER BY installTime DESC")
    List<AppInfo> getAppsInTimeOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY installTime DESC")
    List<AppInfo> getAppsInTimeOrder(String str);


    // Get max id
    @Query("SELECT MAX(id) FROM AppInfo")
    long getMaxId();


    // Disabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 1 ORDER BY appName ASC")
    List<AppInfo> getDisabledApps();

    @Query("SELECT * FROM AppInfo ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAppsInDisabledOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAppsInDisabledOrder(String str);


    // Mobile restricted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE mobileRestricted = 1 AND disabled = 0")
    List<AppInfo> getMobileRestrictedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAppsInMobileRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAppsInMobileRestrictedOrder(String str);


    // Whitelisted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE adhellWhitelisted = 1 ORDER BY appName ASC")
    List<AppInfo> getWhitelistedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAppsInWhitelistedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAppsInWhitelistedOrder(String str);


    // User apps
    @Query("SELECT * FROM AppInfo WHERE system = 0 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getUserApps();


    // Enabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledAppsAlphabetically();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledAppsAlphabetically(String str);

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY installTime DESC")
    List<AppInfo> getEnabledAppsInTimeOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY installTime DESC")
    List<AppInfo> getEnabledAppsInTimeOrder(String str);
}