package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.DisabledPackage;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface DisabledPackageDao {

    @Query("SELECT * FROM DisabledPackage")
    List<DisabledPackage> getAll();

    @Insert
    void insertAll(List<DisabledPackage> disabledPackages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DisabledPackage disabledPackage);

    @Query("DELETE FROM DisabledPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM DisabledPackage")
    void deleteAll();
}
