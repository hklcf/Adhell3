package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.fusionjack.adhell3.db.entity.AppPermission;

import java.util.List;

@Dao
public interface AppPermissionDao {

    @Query("SELECT * FROM AppPermission")
    List<AppPermission> getAll();

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 2")
    List<AppPermission> getServices(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 5")
    List<AppPermission> getReceivers(String packageName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppPermission appPermission);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppPermission> appPermissions);

    @Query("DELETE FROM AppPermission WHERE packageName = :packageName AND permissionName = :permissionName")
    void delete(String packageName, String permissionName);

    @Query("DELETE FROM AppPermission WHERE permissionStatus = -1")
    void deletePermissions();

    @Query("DELETE FROM AppPermission WHERE permissionStatus = 2")
    void deleteServices();

    @Query("DELETE FROM AppPermission WHERE permissionStatus = 5")
    void deleteReceivers();

    @Query("DELETE FROM AppPermission")
    void deleteAll();
}
