package com.fusionjack.adhell3.blocker;

import android.support.annotation.Nullable;
import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.sec.enterprise.AppIdentity;
import com.sec.enterprise.firewall.DomainFilterRule;
import com.sec.enterprise.firewall.Firewall;
import com.sec.enterprise.firewall.FirewallResponse;
import com.sec.enterprise.firewall.FirewallRule;

import javax.inject.Inject;
import java.util.*;

public class ContentBlocker56 implements ContentBlocker {
    private static ContentBlocker56 mInstance = null;

    @Nullable
    @Inject
    Firewall mFirewall;
    @Inject
    AppDatabase appDatabase;

    private ContentBlocker56() {
        App.get().getAppComponent().inject(this);
    }

    public static ContentBlocker56 getInstance() {
        if (mInstance == null) {
            mInstance = getSync();
        }
        return mInstance;
    }

    private static synchronized ContentBlocker56 getSync() {
        if (mInstance == null) {
            mInstance = new ContentBlocker56();
        }
        return mInstance;
    }

    @Override
    public boolean enableBlocker() {
        if (mFirewall == null) {
            return false;
        }

        try {
            if (!mFirewall.isFirewallEnabled()) {
                LogUtils.getInstance().writeInfo("\nEnabling firewall...");
                mFirewall.enableFirewall(true);
            }
            if (!mFirewall.isDomainFilterReportEnabled()) {
                LogUtils.getInstance().writeInfo("Enabling firewall report...");
                mFirewall.enableDomainFilterReport(true);
            }
        } catch (SecurityException e) {
            LogUtils.getInstance().writeError("Failed to enable firewall: " + e.getMessage(), e);
            return false;
        } finally {
            LogUtils.getInstance().writeInfo("Done");
            LogUtils.getInstance().close();
        }

        return true;
    }

    @Override
    public void processCustomRules() throws Exception {
        LogUtils.getInstance().writeInfo("\nProcessing custom rules...");

        List<UserBlockUrl> userBlockUrls = appDatabase.userBlockUrlDao().getAll2();
        LogUtils.getInstance().writeInfo("User blocked URL size: " + userBlockUrls.size());
        for (UserBlockUrl userBlockUrl : userBlockUrls) {
            if (userBlockUrl.url.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(userBlockUrl.url, "|");
                if (tokens.countTokens() == 3) {
                    String packageName = tokens.nextToken();
                    String ipAddress = tokens.nextToken();
                    String port = tokens.nextToken();

                    // Define firewall rule
                    FirewallRule[] firewallRules = new FirewallRule[1];
                    firewallRules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
                    firewallRules[0].setIpAddress(ipAddress);
                    firewallRules[0].setPortNumber(port);
                    firewallRules[0].setApplication(new AppIdentity(packageName, null));

                    addFirewallRules(firewallRules);
                }
            }
        }
    }

    @Override
    public void processMobileRestrictedApps() throws Exception {
        LogUtils.getInstance().writeInfo("\nProcessing mobile restricted apps...");

        List<AppInfo> restrictedApps = appDatabase.applicationInfoDao().getMobileRestrictedApps();
        LogUtils.getInstance().writeInfo("Restricted apps size: " + restrictedApps.size());
        if (restrictedApps.size() == 0) {
            return;
        }

        // Define DENY rules for mobile data
        FirewallRule[] mobileRules = new FirewallRule[restrictedApps.size()];
        for (int i = 0; i < restrictedApps.size(); i++) {
            mobileRules[i] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
            mobileRules[i].setNetworkInterface(Firewall.NetworkInterface.MOBILE_DATA_ONLY);
            mobileRules[i].setApplication(new AppIdentity(restrictedApps.get(i).packageName, null));
        }

        addFirewallRules(mobileRules);
    }

    @Override
    public void processWhitelistedApps() throws Exception {
        LogUtils.getInstance().writeInfo("\nProcessing white-listed apps...");

        // Create domain filter rule for white listed apps
        List<AppInfo> whitelistedApps = appDatabase.applicationInfoDao().getWhitelistedApps();
        LogUtils.getInstance().writeInfo("Whitelisted apps size: " + whitelistedApps.size());
        if (whitelistedApps.size() == 0) {
            return;
        }
        List<DomainFilterRule> rules = new ArrayList<>();
        List<String> superAllow = new ArrayList<>();
        superAllow.add("*");
        for (AppInfo app : whitelistedApps) {
            LogUtils.getInstance().writeInfo("Whitelisted app: " + app.packageName);
            rules.add(new DomainFilterRule(new AppIdentity(app.packageName, null), new ArrayList<>(), superAllow));
        }
        addDomainFilterRules(rules);
    }

    @Override
    public void processWhitelistedDomains() throws Exception {
        LogUtils.getInstance().writeInfo("\nProcessing white-listed domains...");

        // Process user-defined white list
        // 1. URL for all packages: url
        // 2. URL for individual package: packageName|url
        List<WhiteUrl> whiteUrls = appDatabase.whiteUrlDao().getAll2();
        LogUtils.getInstance().writeInfo("User whitelisted URL size: " + whiteUrls.size());
        if (whiteUrls.size() == 0) {
            return;
        }

        Set<String> denyList = BlockUrlUtils.getUniqueBlockedUrls(appDatabase);
        for (WhiteUrl whiteUrl : whiteUrls) {
            if (whiteUrl.url.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(whiteUrl.url, "|");
                if (tokens.countTokens() == 2) {
                    final String packageName = tokens.nextToken();
                    final String url = tokens.nextToken();
                    final AppIdentity appIdentity = new AppIdentity(packageName, null);
                    LogUtils.getInstance().writeInfo("PackageName: " + packageName + ", WhiteUrl: " + url);

                    List<String> whiteList = new ArrayList<>();
                    whiteList.add(url);

                    List<DomainFilterRule> rules = new ArrayList<>();
                    rules.add(new DomainFilterRule(appIdentity, new ArrayList<>(denyList), whiteList));
                    addDomainFilterRules(rules);
                }
            }
        }

        // Whitelist URL for all apps
        Set<String> allowList = new HashSet<>();
        for (WhiteUrl whiteUrl : whiteUrls) {
            if (whiteUrl.url.indexOf('|') == -1) {
                final String url = BlockUrlPatternsMatch.getValidatedUrl(whiteUrl.url);
                allowList.add(url);
                LogUtils.getInstance().writeInfo("WhiteUrl: " + url);
            }
        }
        if (allowList.size() > 0) {
            final AppIdentity appIdentity = new AppIdentity("*", null);
            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, new ArrayList<>(), new ArrayList<>(allowList)));
            addDomainFilterRules(rules);
        }
    }

    @Override
    public void processBlockedDomains() throws Exception {
        LogUtils.getInstance().writeInfo("\nProcessing blocked domains...");

        Set<String> denyList = BlockUrlUtils.getUniqueBlockedUrls(appDatabase);
        List<DomainFilterRule> rules = new ArrayList<>();
        AppIdentity appIdentity = new AppIdentity("*", null);
        rules.add(new DomainFilterRule(appIdentity, new ArrayList<>(denyList), new ArrayList<>()));
        addDomainFilterRules(rules);
    }

    @Override
    public boolean disableBlocker() {
        if (mFirewall == null) {
            return false;
        }

        FirewallResponse[] response;
        try {
            // Clear IP rules
            response = mFirewall.clearRules(Firewall.FIREWALL_ALL_RULES);
            LogUtils.getInstance().writeInfo(response == null ? "No response" : response[0].getMessage());

            // Clear domain filter rules
            response = mFirewall.removeDomainFilterRules(DomainFilterRule.CLEAR_ALL);
            LogUtils.getInstance().writeInfo(response == null ? "No response" : response[0].getMessage());

            if (mFirewall.isFirewallEnabled()) {
                mFirewall.enableFirewall(false);
            }
            if (mFirewall.isDomainFilterReportEnabled()) {
                mFirewall.enableDomainFilterReport(false);
            }
        } catch (SecurityException ex) {
            LogUtils.getInstance().writeError("Failed to remove firewall rules", ex);
            return false;
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return mFirewall != null && mFirewall.isFirewallEnabled();
    }

    private void addDomainFilterRules(List<DomainFilterRule> domainRules) throws Exception {
        if (mFirewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        LogUtils.getInstance().writeInfo("Adding domain filter rule to Knox Firewall...");
        FirewallResponse[] response;
        try {
            response = mFirewall.addDomainFilterRules(domainRules);
            if (response == null) {
                throw new Exception("There was no response from Knox Firewall");
            } else {
                LogUtils.getInstance().writeInfo("Result: " + response[0].getMessage());
                if (FirewallResponse.Result.SUCCESS != response[0].getResult()) {
                    throw new Exception(response[0].getMessage());
                }
            }
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add domain filter rule to Knox Firewall", ex);
        }
    }

    private void addFirewallRules(FirewallRule[] firewallRules) throws Exception {
        if (mFirewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        LogUtils.getInstance().writeInfo("Adding firewall rule to Knox Firewall...");
        FirewallResponse[] response;
        try {
            response = mFirewall.addRules(firewallRules);
            if (response == null) {
                throw new Exception("There was no response from Knox Firewall");
            } else {
                LogUtils.getInstance().writeInfo("Result: " + response[0].getMessage());
                if (FirewallResponse.Result.SUCCESS != response[0].getResult()) {
                    throw new Exception(response[0].getMessage());
                }
            }
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add firewall rules to Knox Firewall", ex);
        }
    }
}