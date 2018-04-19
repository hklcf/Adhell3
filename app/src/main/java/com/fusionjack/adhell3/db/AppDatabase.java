package com.fusionjack.adhell3.db;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.Environment;
import android.os.LocaleList;

import com.fusionjack.adhell3.db.dao.AppInfoDao;
import com.fusionjack.adhell3.db.dao.AppPermissionDao;
import com.fusionjack.adhell3.db.dao.BlockUrlDao;
import com.fusionjack.adhell3.db.dao.BlockUrlProviderDao;
import com.fusionjack.adhell3.db.dao.DisabledPackageDao;
import com.fusionjack.adhell3.db.dao.FirewallWhitelistedPackageDao;
import com.fusionjack.adhell3.db.dao.PolicyPackageDao;
import com.fusionjack.adhell3.db.dao.ReportBlockedUrlDao;
import com.fusionjack.adhell3.db.dao.RestrictedPackageDao;
import com.fusionjack.adhell3.db.dao.UserBlockUrlDao;
import com.fusionjack.adhell3.db.dao.WhiteUrlDao;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.PolicyPackage;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.db.migration.Migration_14_15;
import com.fusionjack.adhell3.db.migration.Migration_15_16;
import com.fusionjack.adhell3.db.migration.Migration_16_17;
import com.fusionjack.adhell3.db.migration.Migration_17_18;
import com.fusionjack.adhell3.db.migration.Migration_18_19;
import com.fusionjack.adhell3.db.migration.Migration_19_20;
import com.fusionjack.adhell3.db.migration.Migration_20_21;
import com.fusionjack.adhell3.db.migration.Migration_21_22;
import com.fusionjack.adhell3.db.migration.Migration_22_23;
import com.fusionjack.adhell3.db.migration.Migration_23_24;

import java.io.File;

@Database(entities = {
        AppInfo.class,
        AppPermission.class,
        BlockUrl.class,
        BlockUrlProvider.class,
        DisabledPackage.class,
        RestrictedPackage.class,
        FirewallWhitelistedPackage.class,
        PolicyPackage.class,
        ReportBlockedUrl.class,
        UserBlockUrl.class,
        WhiteUrl.class
}, version = 24)

public abstract class AppDatabase extends RoomDatabase {
    private static final Migration MIGRATION_14_15 = new Migration_14_15(14, 15);
    private static final Migration MIGRATION_15_16 = new Migration_15_16(15, 16);
    private static final Migration MIGRATION_16_17 = new Migration_16_17(16, 17);
    private static final Migration MIGRATION_17_18 = new Migration_17_18(17, 18);
    private static final Migration MIGRATION_18_19 = new Migration_18_19(18, 19);
    private static final Migration MIGRATION_19_20 = new Migration_19_20(19, 20);
    private static final Migration MIGRATION_20_21 = new Migration_20_21(20, 21);
    private static final Migration MIGRATION_21_22 = new Migration_21_22(21, 22);
    private static final Migration MIGRATION_22_23 = new Migration_22_23(22, 23);
    private static final Migration MIGRATION_23_24 = new Migration_23_24(23, 24);
    private static AppDatabase INSTANCE;

    public static final String DATABASE_FOLDER = "adhell3";
    public static final String DATABASE_FILE = "adhell-database";

    public static AppDatabase getAppDatabase(Context context) {
        if (INSTANCE == null) {
            String location;
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                File adhell3 = new File(sd, DATABASE_FOLDER);
                if (!adhell3.exists()) {
                    adhell3.mkdir();
                }
                File db = new File(adhell3, DATABASE_FILE);
                location = db.getAbsolutePath();
            } else {
                location = DATABASE_FILE;
            }

            INSTANCE =
                    Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, location)
                            .addMigrations(MIGRATION_14_15)
                            .addMigrations(MIGRATION_15_16)
                            .addMigrations(MIGRATION_16_17)
                            .addMigrations(MIGRATION_17_18)
                            .addMigrations(MIGRATION_18_19)
                            .addMigrations(MIGRATION_19_20)
                            .addMigrations(MIGRATION_20_21)
                            .addMigrations(MIGRATION_21_22)
                            .addMigrations(MIGRATION_22_23)
                            .addMigrations(MIGRATION_23_24)
                            .build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    public abstract BlockUrlDao blockUrlDao();

    public abstract BlockUrlProviderDao blockUrlProviderDao();

    public abstract ReportBlockedUrlDao reportBlockedUrlDao();

    public abstract AppInfoDao applicationInfoDao();

    public abstract WhiteUrlDao whiteUrlDao();

    public abstract UserBlockUrlDao userBlockUrlDao();

    public abstract PolicyPackageDao policyPackageDao();

    public abstract DisabledPackageDao disabledPackageDao();

    public abstract RestrictedPackageDao restrictedPackageDao();

    public abstract FirewallWhitelistedPackageDao firewallWhitelistedPackageDao();

    public abstract AppPermissionDao appPermissionDao();

}