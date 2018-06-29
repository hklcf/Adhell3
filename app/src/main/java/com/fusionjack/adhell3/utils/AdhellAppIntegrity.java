package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;
import android.util.Log;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.PolicyPackage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdhellAppIntegrity {
    public static final String ADHELL_STANDARD_PACKAGE = "https://raw.githubusercontent.com/mmotti/mmotti-host-file/master/hosts";
    public static final int BLOCK_URL_LIMIT = 15000;
    public final static String DEFAULT_POLICY_ID = "default-policy";

    private static final String TAG = AdhellAppIntegrity.class.getCanonicalName();
    private static final String DEFAULT_POLICY_CHECKED = "adhell_default_policy_created";
    private static final String DISABLED_PACKAGES_MOVED = "adhell_disabled_packages_moved";
    private static final String FIREWALL_WHITELISTED_PACKAGES_MOVED = "adhell_firewall_whitelisted_packages_moved";
    private static final String MOVE_APP_PERMISSIONS = "adhell_app_permissions_moved";
    private static final String DEFAULT_PACKAGES_FIREWALL_WHITELISTED = "adhell_default_packages_firewall_whitelisted";
    private static final String CHECK_ADHELL_STANDARD_PACKAGE = "adhell_adhell_standard_package";
    private static final String CHECK_PACKAGE_DB = "adhell_packages_filled_db";

    private AppDatabase appDatabase;
    private SharedPreferences sharedPreferences;

    private static AdhellAppIntegrity instance;

    private AdhellAppIntegrity() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
    }

    public static AdhellAppIntegrity getInstance() {
        if (instance == null) {
            instance = new AdhellAppIntegrity();
        }
        return instance;
    }

    public void check() {
        boolean defaultPolicyChecked = sharedPreferences.getBoolean(DEFAULT_POLICY_CHECKED, false);
        if (!defaultPolicyChecked) {
            checkDefaultPolicyExists();
            sharedPreferences.edit().putBoolean(DEFAULT_POLICY_CHECKED, true).apply();
        }
        boolean disabledPackagesMoved = sharedPreferences.getBoolean(DISABLED_PACKAGES_MOVED, false);
        if (!disabledPackagesMoved) {
            copyDataFromAppInfoToDisabledPackage();
            sharedPreferences.edit().putBoolean(DISABLED_PACKAGES_MOVED, true).apply();
        }
        boolean firewallWhitelistedPackagesMoved
                = sharedPreferences.getBoolean(FIREWALL_WHITELISTED_PACKAGES_MOVED, false);
        if (!firewallWhitelistedPackagesMoved) {
            copyDataFromAppInfoToFirewallWhitelistedPackage();
            sharedPreferences.edit().putBoolean(FIREWALL_WHITELISTED_PACKAGES_MOVED, true).apply();
        }
        boolean defaultPackagesFirewallWhitelisted
                = sharedPreferences.getBoolean(DEFAULT_PACKAGES_FIREWALL_WHITELISTED, false);
        if (!defaultPackagesFirewallWhitelisted) {
            addDefaultAdblockWhitelist();
            sharedPreferences.edit().putBoolean(DEFAULT_PACKAGES_FIREWALL_WHITELISTED, true).apply();
        }
        boolean adhellStandardPackageChecked = sharedPreferences.getBoolean(CHECK_ADHELL_STANDARD_PACKAGE, false);
        if (!adhellStandardPackageChecked) {
            checkAdhellStandardPackage();
            sharedPreferences.edit().putBoolean(CHECK_ADHELL_STANDARD_PACKAGE, false).apply();
        }
        boolean packageDbFilled = sharedPreferences.getBoolean(CHECK_PACKAGE_DB, false);
        if (!packageDbFilled) {
            fillPackageDb();
            sharedPreferences.edit().putBoolean(CHECK_PACKAGE_DB, true).apply();
        }
    }

    public void checkDefaultPolicyExists() {
        PolicyPackage policyPackage = appDatabase.policyPackageDao().getPolicyById(DEFAULT_POLICY_ID);
        if (policyPackage != null) {
            Log.d(TAG, "Default PolicyPackage exists");
            return;
        }
        Log.d(TAG, "Default PolicyPackage does not exist. Creating default policy.");
        policyPackage = new PolicyPackage();
        policyPackage.id = DEFAULT_POLICY_ID;
        policyPackage.name = "Default Policy";
        policyPackage.description = "Automatically generated policy from current Adhell app settings";
        policyPackage.active = true;
        policyPackage.createdAt = policyPackage.updatedAt = new Date();
        appDatabase.policyPackageDao().insert(policyPackage);
        Log.d(TAG, "Default PolicyPackage has been added");
    }

    private void copyDataFromAppInfoToDisabledPackage() {
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        if (disabledPackages.size() > 0) {
            Log.d(TAG, "DisabledPackages is not empty. No need to move data from AppInfo table");
            return;
        }
        List<AppInfo> disabledApps = appDatabase.applicationInfoDao().getDisabledApps();
        if (disabledApps.size() == 0) {
            Log.d(TAG, "No disabledgetDisabledApps apps in AppInfo table");
            return;
        }
        Log.d(TAG, "There is " + disabledApps.size() + " to move to DisabledPackage table");
        disabledPackages = new ArrayList<>();
        for (AppInfo appInfo : disabledApps) {
            DisabledPackage disabledPackage = new DisabledPackage();
            disabledPackage.packageName = appInfo.packageName;
            disabledPackage.policyPackageId = DEFAULT_POLICY_ID;
            disabledPackages.add(disabledPackage);
        }
        appDatabase.disabledPackageDao().insertAll(disabledPackages);
    }

    private void copyDataFromAppInfoToFirewallWhitelistedPackage() {
        List<FirewallWhitelistedPackage> firewallWhitelistedPackages
                = appDatabase.firewallWhitelistedPackageDao().getAll();
        if (firewallWhitelistedPackages.size() > 0) {
            Log.d(TAG, "FirewallWhitelist package size is: " + firewallWhitelistedPackages.size() + ". No need to move data");
            return;
        }
        List<AppInfo> whitelistedApps = appDatabase.applicationInfoDao().getWhitelistedApps();
        if (whitelistedApps.size() == 0) {
            Log.d(TAG, "No whitelisted apps in AppInfo table");
            return;
        }
        Log.d(TAG, "There is " + whitelistedApps.size() + " to move");
        firewallWhitelistedPackages = new ArrayList<>();
        for (AppInfo appInfo : whitelistedApps) {
            FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
            whitelistedPackage.packageName = appInfo.packageName;
            whitelistedPackage.policyPackageId = DEFAULT_POLICY_ID;
            firewallWhitelistedPackages.add(whitelistedPackage);
        }
        appDatabase.firewallWhitelistedPackageDao().insertAll(firewallWhitelistedPackages);
    }

    private void addDefaultAdblockWhitelist() {
        List<FirewallWhitelistedPackage> firewallWhitelistedPackages = new ArrayList<>();
        firewallWhitelistedPackages.add(new FirewallWhitelistedPackage("com.google.android.music", DEFAULT_POLICY_ID));
        firewallWhitelistedPackages.add(new FirewallWhitelistedPackage("com.google.android.apps.fireball", DEFAULT_POLICY_ID));
        firewallWhitelistedPackages.add(new FirewallWhitelistedPackage("com.nttdocomo.android.ipspeccollector2", DEFAULT_POLICY_ID));
        appDatabase.firewallWhitelistedPackageDao().insertAll(firewallWhitelistedPackages);
    }

    public void checkAdhellStandardPackage() {
        BlockUrlProvider blockUrlProvider =
                appDatabase.blockUrlProviderDao().getByUrl(ADHELL_STANDARD_PACKAGE);
        if (blockUrlProvider != null) {
            return;
        }

        // Remove existing default
        if (appDatabase.blockUrlProviderDao().getDefault().size() > 0) {
            appDatabase.blockUrlProviderDao().deleteDefault();
        }

        // Add the default package
        blockUrlProvider = new BlockUrlProvider();
        blockUrlProvider.url = ADHELL_STANDARD_PACKAGE;
        blockUrlProvider.lastUpdated = new Date();
        blockUrlProvider.deletable = false;
        blockUrlProvider.selected = true;
        blockUrlProvider.policyPackageId = DEFAULT_POLICY_ID;
        long ids[] = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider);
        blockUrlProvider.id = ids[0];
        List<BlockUrl> blockUrls;
        try {
            blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
            blockUrlProvider.count = blockUrls.size();
            Log.d(TAG, "Number of urls to insert: " + blockUrlProvider.count);
            // Save url provider
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
            // Save urls from providers
            appDatabase.blockUrlDao().insertAll(blockUrls);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void fillPackageDb() {
        if (appDatabase.applicationInfoDao().getAppSize() > 0) {
            return;
        }
        AppCache.reload(null, null);
    }
}
