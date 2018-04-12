package com.fusionjack.adhell3.db;

import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class DatabaseFactory {
    private static final String BACKUP_FILENAME = "adhell_backup.txt";
    private static DatabaseFactory instance;
    private AppDatabase appDatabase;

    private DatabaseFactory() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static DatabaseFactory getInstance() {
        if (instance == null) {
            instance = new DatabaseFactory();
        }
        return instance;
    }

    public void backupDatabase() throws Exception {
        File file = new File(Environment.getExternalStorageDirectory(), BACKUP_FILENAME);
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            writer.setIndent("  ");
            writer.beginObject();
            writeWhitelistedPackages(writer, appDatabase);
            writeDisabledPackages(writer, appDatabase);
            writeRestrictedPackages(writer, appDatabase);
            writeAppPermissions(writer, appDatabase);
            writeBlockUrlProviders(writer, appDatabase);
            writeUserBlockUrls(writer, appDatabase);
            writeWhiteUrls(writer, appDatabase);
            writer.endObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void restoreDatabase() throws Exception {
        File backupFile = new File(Environment.getExternalStorageDirectory(), BACKUP_FILENAME);
        if (!backupFile.exists()) {
            throw new FileNotFoundException("Backup file " + BACKUP_FILENAME + " cannot be found");
        }

        File dbFolder = new File(Environment.getExternalStorageDirectory(), AppDatabase.DATABASE_FOLDER);
        File dbFile = new File(dbFolder, AppDatabase.DATABASE_FILE);
        File backupDbFile = new File(dbFolder, AppDatabase.DATABASE_FILE + ".bak");
        copy(dbFile, backupDbFile);

        try {
            DeviceAdminInteractor.getInstance().getContentBlocker().disableBlocker();
            AdhellAppIntegrity appIntegrity = new AdhellAppIntegrity();
            appIntegrity.checkDefaultPolicyExists();

            appDatabase.applicationInfoDao().deleteAll();
            appIntegrity.fillPackageDb();

            try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(backupFile), "UTF-8"))) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equalsIgnoreCase("FirewallWhitelistedPackage")) {
                        readWhitelistedPackages(reader);
                    } else if (name.equalsIgnoreCase("DisabledPackage")) {
                        readDisabledPackages(reader);
                    } else if (name.equalsIgnoreCase("RestrictedPackage")) {
                        readRestrictedPackages(reader);
                    } else if (name.equalsIgnoreCase("AppPermission")) {
                        readAppPermissions(reader);
                    } else if (name.equalsIgnoreCase("BlockUrlProvider")) {
                        readBlockUrlProviders(reader);
                    } else if (name.equalsIgnoreCase("UserBlockUrl")) {
                        readUserBlockUrls(reader);
                    } else if (name.equalsIgnoreCase("WhiteUrl")) {
                        readWhiteUrls(reader);
                    }
                }
                reader.endObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
            copy(backupDbFile, dbFile);
            backupDbFile.delete();
            throw e;
        }

        backupDbFile.delete();
    }

    private static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private void writeWhitelistedPackages(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("FirewallWhitelistedPackage");
        writer.beginArray();
        List<FirewallWhitelistedPackage> whitelistedPackages = appDatabase.firewallWhitelistedPackageDao().getAll();
        for (FirewallWhitelistedPackage whitelistedPackage : whitelistedPackages) {
            writer.beginObject();
            writer.name("packageName").value(whitelistedPackage.packageName);
            writer.name("policyPackageId").value(whitelistedPackage.policyPackageId == null ?
                    AdhellAppIntegrity.DEFAULT_POLICY_ID : whitelistedPackage.policyPackageId);
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeDisabledPackages(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("DisabledPackage");
        writer.beginArray();
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        for (DisabledPackage disabledPackage : disabledPackages) {
            writer.beginObject();
            writer.name("packageName").value(disabledPackage.packageName);
            writer.name("policyPackageId").value(disabledPackage.policyPackageId == null ?
                    AdhellAppIntegrity.DEFAULT_POLICY_ID : disabledPackage.policyPackageId);
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeRestrictedPackages(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("RestrictedPackage");
        writer.beginArray();
        List<RestrictedPackage> restrictedPackages = appDatabase.restrictedPackageDao().getAll();
        for (RestrictedPackage restrictedPackage : restrictedPackages) {
            writer.beginObject();
            writer.name("packageName").value(restrictedPackage.packageName);
            writer.name("policyPackageId").value(restrictedPackage.policyPackageId == null ?
                    AdhellAppIntegrity.DEFAULT_POLICY_ID : restrictedPackage.policyPackageId);
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeAppPermissions(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("AppPermission");
        writer.beginArray();
        List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
        for (AppPermission permission : appPermissions) {
            writer.beginObject();
            writer.name("packageName").value(permission.packageName);
            writer.name("permissionName").value(permission.permissionName);
            writer.name("permissionStatus").value(permission.permissionStatus);
            writer.name("policyPackageId").value(permission.policyPackageId == null ?
                    AdhellAppIntegrity.DEFAULT_POLICY_ID : permission.policyPackageId);
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeBlockUrlProviders(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("BlockUrlProvider");
        writer.beginArray();
        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getAll2();
        for (BlockUrlProvider provider: blockUrlProviders) {
            writer.beginObject();
            writer.name("url").value(provider.url);
            writer.name("deletable").value(provider.deletable);
            writer.name("selected").value(provider.selected);
            writer.name("policyPackageId").value(provider.policyPackageId == null ?
                    AdhellAppIntegrity.DEFAULT_POLICY_ID : provider.policyPackageId);
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeUserBlockUrls(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("UserBlockUrl");
        writer.beginArray();
        List<UserBlockUrl> userBlockUrls = appDatabase.userBlockUrlDao().getAll2();
        for (UserBlockUrl blockUrl : userBlockUrls) {
            writer.beginObject();
            writer.name("url").value(blockUrl.url);
            writer.name("insertedAt").value(blockUrl.insertedAt.getTime());
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeWhiteUrls(JsonWriter writer, AppDatabase appDatabase) throws IOException {
        writer.name("WhiteUrl");
        writer.beginArray();
        List<WhiteUrl> whiteUrls = appDatabase.whiteUrlDao().getAll2();
        for (WhiteUrl whiteUrl : whiteUrls) {
            writer.beginObject();
            writer.name("url").value(whiteUrl.url);
            writer.name("insertedAt").value(whiteUrl.insertedAt.getTime());
            writer.endObject();
        }
        writer.endArray();
    }

    private void readWhitelistedPackages(JsonReader reader) throws IOException {
        String packageName = "";
        String policyPackageId = "";
        List<FirewallWhitelistedPackage> whitelistedPackages = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("packageName")) {
                    packageName = reader.nextString();
                } else if (name.equalsIgnoreCase("policyPackageId")) {
                    policyPackageId = reader.nextString();
                }
            }
            reader.endObject();

            FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
            whitelistedPackage.packageName = packageName;
            whitelistedPackage.policyPackageId = policyPackageId;
            whitelistedPackages.add(whitelistedPackage);
        }
        reader.endArray();

        appDatabase.firewallWhitelistedPackageDao().deleteAll();
        appDatabase.firewallWhitelistedPackageDao().insertAll(whitelistedPackages);

        for (FirewallWhitelistedPackage whitelistedPackage : whitelistedPackages) {
            AppInfo appInfo = appDatabase.applicationInfoDao().getAppByPackageName(whitelistedPackage.packageName);
            if (appInfo != null) {
                appInfo.adhellWhitelisted = true;
                appDatabase.applicationInfoDao().update(appInfo);
            }
        }
    }

    private void readDisabledPackages(JsonReader reader) throws IOException {
        String packageName = "";
        String policyPackageId = "";
        List<DisabledPackage> disabledPackages = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("packageName")) {
                    packageName = reader.nextString();
                } else if (name.equalsIgnoreCase("policyPackageId")) {
                    policyPackageId = reader.nextString();
                }
            }
            reader.endObject();

            DisabledPackage disabledPackage = new DisabledPackage();
            disabledPackage.packageName = packageName;
            disabledPackage.policyPackageId = policyPackageId;
            disabledPackages.add(disabledPackage);
        }
        reader.endArray();

        appDatabase.disabledPackageDao().deleteAll();
        appDatabase.disabledPackageDao().insertAll(disabledPackages);

        for (DisabledPackage disabledPackage : disabledPackages) {
            AppInfo appInfo = appDatabase.applicationInfoDao().getAppByPackageName(disabledPackage.packageName);
            if (appInfo != null) {
                appInfo.disabled = true;
                appDatabase.applicationInfoDao().update(appInfo);
            }
        }
    }

    private void readRestrictedPackages(JsonReader reader) throws IOException {
        String packageName = "";
        String policyPackageId = "";
        List<RestrictedPackage> restrictedPackages = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("packageName")) {
                    packageName = reader.nextString();
                } else if (name.equalsIgnoreCase("policyPackageId")) {
                    policyPackageId = reader.nextString();
                }
            }
            reader.endObject();

            RestrictedPackage restrictedPackage = new RestrictedPackage();
            restrictedPackage.packageName = packageName;
            restrictedPackage.policyPackageId = policyPackageId;
            restrictedPackages.add(restrictedPackage);
        }
        reader.endArray();

        appDatabase.restrictedPackageDao().deleteAll();
        appDatabase.restrictedPackageDao().insertAll(restrictedPackages);

        for (RestrictedPackage restrictedPackage : restrictedPackages) {
            AppInfo appInfo = appDatabase.applicationInfoDao().getAppByPackageName(restrictedPackage.packageName);
            if (appInfo != null) {
                appInfo.mobileRestricted = true;
                appDatabase.applicationInfoDao().update(appInfo);
            }
        }
    }

    private void readAppPermissions(JsonReader reader) throws IOException {
        String packageName = "";
        String permissionName = "";
        int permissionStatus = -1;
        String policyPackageId = "";
        List<AppPermission> appPermissions = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("packageName")) {
                    packageName = reader.nextString();
                } else if (name.equalsIgnoreCase("permissionName")) {
                    permissionName = reader.nextString();
                } else if (name.equalsIgnoreCase("permissionStatus")) {
                    permissionStatus = reader.nextInt();
                } else if (name.equalsIgnoreCase("policyPackageId")) {
                    policyPackageId = reader.nextString();
                }
            }
            reader.endObject();

            AppPermission appPermission = new AppPermission();
            appPermission.packageName = packageName;
            appPermission.permissionName = permissionName;
            appPermission.permissionStatus = permissionStatus;
            appPermission.policyPackageId = policyPackageId;
            appPermissions.add(appPermission);
        }
        reader.endArray();

        appDatabase.appPermissionDao().deleteAll();
        appDatabase.appPermissionDao().insertAll(appPermissions);
    }

    private void readBlockUrlProviders(JsonReader reader) throws IOException {
        String url = "";
        boolean deletable = false;
        boolean selected = false;
        String policyPackageId = "";

        appDatabase.blockUrlProviderDao().deleteAll();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("url")) {
                    url = reader.nextString();
                } else if (name.equalsIgnoreCase("deletable")) {
                    deletable = reader.nextBoolean();
                } else if (name.equalsIgnoreCase("selected")) {
                    selected = reader.nextBoolean();
                } else if (name.equalsIgnoreCase("policyPackageId")) {
                    policyPackageId = reader.nextString();
                }
            }
            reader.endObject();

            BlockUrlProvider provider = new BlockUrlProvider();
            provider.url = url;
            provider.deletable = deletable;
            provider.selected = selected;
            provider.policyPackageId = policyPackageId;
            provider.id = appDatabase.blockUrlProviderDao().insertAll(provider)[0];
        }
        reader.endArray();
    }

    private void readUserBlockUrls(JsonReader reader) throws IOException {
        String url = "";
        Date insertedAt = null;
        List<UserBlockUrl> userBlockUrls = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("url")) {
                    url = reader.nextString();
                } else if (name.equalsIgnoreCase("insertedAt")) {
                    insertedAt = new Date(reader.nextLong());
                }
            }
            reader.endObject();

            UserBlockUrl blockUrl = new UserBlockUrl();
            blockUrl.url = url;
            blockUrl.insertedAt = insertedAt;
            userBlockUrls.add(blockUrl);
        }
        reader.endArray();

        appDatabase.userBlockUrlDao().deleteAll();
        appDatabase.userBlockUrlDao().insertAll(userBlockUrls);
    }

    private void readWhiteUrls(JsonReader reader) throws IOException {
        String url = "";
        Date insertedAt = null;
        List<WhiteUrl> whiteUrls = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equalsIgnoreCase("url")) {
                    url = reader.nextString();
                } else if (name.equalsIgnoreCase("insertedAt")) {
                    insertedAt = new Date(reader.nextLong());
                }
            }
            reader.endObject();

            WhiteUrl whiteUrl = new WhiteUrl();
            whiteUrl.url = url;
            whiteUrl.insertedAt = insertedAt;
            whiteUrls.add(whiteUrl);
        }
        reader.endArray();

        appDatabase.whiteUrlDao().deleteAll();
        appDatabase.whiteUrlDao().insertAll(whiteUrls);
    }
}
