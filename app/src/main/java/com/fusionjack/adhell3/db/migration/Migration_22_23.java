package com.fusionjack.adhell3.db.migration;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;

public class Migration_22_23 extends Migration {

    public Migration_22_23(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE RestrictedPackage " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "packageName TEXT, " +
                "policyPackageId TEXT, " +
                "FOREIGN KEY (policyPackageId) REFERENCES PolicyPackage(id))");
        database.execSQL("CREATE INDEX index_RestrictedPackage_policyPackageId " +
                "ON RestrictedPackage (policyPackageId)");
    }
}
