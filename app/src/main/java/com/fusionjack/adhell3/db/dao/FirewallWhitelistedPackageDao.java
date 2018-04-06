package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;

import java.util.List;

@Dao
public interface FirewallWhitelistedPackageDao {

    @Query("SELECT * FROM FirewallWhitelistedPackage")
    List<FirewallWhitelistedPackage> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FirewallWhitelistedPackage> firewallWhitelistedPackages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FirewallWhitelistedPackage whitelistedPackage);

    @Query("DELETE FROM FirewallWhitelistedPackage")
    void deleteAll();

    @Query("DELETE FROM FirewallWhitelistedPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);
}
