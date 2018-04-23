package com.fusionjack.adhell3.db.migration;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;

public class Migration_24_25 extends Migration {

    public Migration_24_25(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE AppInfo ADD COLUMN hasCustomDns INTEGER NOT NULL DEFAULT 0");
        database.execSQL("CREATE TABLE DnsPackage " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "packageName TEXT, " +
                "policyPackageId TEXT, " +
                "FOREIGN KEY (policyPackageId) REFERENCES PolicyPackage(id))");
        database.execSQL("CREATE INDEX index_DnsPackage_policyPackageId " +
                "ON DnsPackage (policyPackageId)");
    }
}
