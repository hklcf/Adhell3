package com.fusionjack.adhell3.blocker;

import android.os.Handler;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.common.collect.Lists;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.net.firewall.FirewallResponse;
import com.samsung.android.knox.net.firewall.FirewallRule;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class ContentBlocker56 implements ContentBlocker {
    private static ContentBlocker56 mInstance = null;

    private Firewall firewall;
    private AppDatabase appDatabase;
    private Handler handler;
    private FirewallUtils firewallUtils;

    private ContentBlocker56() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.firewall = AdhellFactory.getInstance().getFirewall();
        this.firewallUtils = FirewallUtils.getInstance();
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
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void enableFirewallRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Enabling firewall rules...", handler);

        try {
            processCustomRules();
            processMobileRestrictedApps();
            processWifiRestrictedApps();

            LogUtils.info("\nFirewall rules are enabled.", handler);

            if (!firewall.isFirewallEnabled()) {
                LogUtils.info("\nEnabling Knox firewall...", handler);
                firewall.enableFirewall(true);
                LogUtils.info("Knox firewall is enabled.", handler);
            }
        } catch (Exception e) {
            disableFirewallRules();
            e.printStackTrace();
        }
    }

    @Override
    public void disableFirewallRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Disabling firewall rules...", handler);

        // Clear firewall rules
        LogUtils.info("\nClearing firewall rules...", handler);
        FirewallResponse[] response = firewall.clearRules(Firewall.FIREWALL_ALL_RULES);
        LogUtils.info(response == null ? "No response" : response[0].getMessage(), handler);

        LogUtils.info("\nFirewall rules are disabled.", handler);

        if (firewall.isFirewallEnabled() && isDomainRuleEmpty()) {
            LogUtils.info("\nDisabling Knox firewall...", handler);
            firewall.enableFirewall(false);
            LogUtils.info("\nKnox firewall is disabled.", handler);
        }
    }

    @Override
    public void enableDomainRules(boolean updateProviders) {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Enabling domain rules...", handler);

        try {
            processWhitelistedApps();
            processWhitelistedDomains();
            processUserBlockedDomains();
            processBlockedDomains(updateProviders);
            AdhellFactory.getInstance().applyDns(handler);

            LogUtils.info("\nDomain rules are enabled.", handler);

            if (!firewall.isFirewallEnabled()) {
                LogUtils.info("\nEnabling Knox firewall...", handler);
                firewall.enableFirewall(true);
                LogUtils.info("Knox firewall is enabled.", handler);
            }
            if (!firewall.isDomainFilterReportEnabled()) {
                LogUtils.info("\nEnabling firewall report...", handler);
                firewall.enableDomainFilterReport(true);
                LogUtils.info("Firewall report is enabled.", handler);
            }
        } catch (Exception e) {
            disableDomainRules();
            e.printStackTrace();
        }
    }

    @Override
    public void disableDomainRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Disabling domain rules...", handler);

        // Clear domain filter rules
        LogUtils.info("\nClearing domain rules...", handler);
        FirewallResponse[] response = firewall.removeDomainFilterRules(DomainFilterRule.CLEAR_ALL);
        LogUtils.info(response == null ? "No response" : response[0].getMessage(), handler);

        LogUtils.info("\nDomain rules are disabled.", handler);

        if (firewall.isFirewallEnabled() && isFirewallRuleEmpty()) {
            LogUtils.info("\nDisabling Knox firewall...", handler);
            firewall.enableFirewall(false);
            LogUtils.info("Knox firewall is disabled.", handler);
        }
        if (firewall.isDomainFilterReportEnabled()) {
            firewall.enableDomainFilterReport(false);
        }

        AppPreferences.getInstance().resetBlockedDomainsCount();
    }

    private void processCustomRules() throws Exception {
        LogUtils.info("\nProcessing custom rules...", handler);

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        int count = 0;
        List<String> urls = appDatabase.userBlockUrlDao().getAll3();
        for (String url : urls) {
            if (url.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(url, "|");
                if (tokens.countTokens() == 3) {
                    String packageName = tokens.nextToken().trim();
                    String ip = tokens.nextToken().trim();
                    String port = tokens.nextToken().trim();

                    boolean add = true;
                    for (FirewallRule enabledRule : enabledRules) {
                        String packageName1 = enabledRule.getApplication().getPackageName();
                        String ip1 = enabledRule.getIpAddress();
                        String port1 = enabledRule.getPortNumber();
                        if (packageName1.equalsIgnoreCase(packageName) && ip1.equalsIgnoreCase(ip) && port1.equalsIgnoreCase(port)) {
                            add = false;
                        }
                    }

                    LogUtils.info("\nRule: " + packageName + "|" + ip + "|" + port, handler);
                    if (add) {
                        FirewallRule[] firewallRules;
                        if (ip.equalsIgnoreCase("*")) {
                            firewallRules = new FirewallRule[2];
                            firewallRules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
                            firewallRules[0].setIpAddress(ip);
                            firewallRules[0].setPortNumber(port);
                            firewallRules[0].setApplication(new AppIdentity(packageName, null));

                            firewallRules[1] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV6);
                            firewallRules[1].setIpAddress(ip);
                            firewallRules[1].setPortNumber(port);
                            firewallRules[1].setApplication(new AppIdentity(packageName, null));
                        } else {
                            Firewall.AddressType type;
                            InetAddress address = InetAddress.getByName(ip);
                            if (address instanceof Inet6Address) {
                                type = Firewall.AddressType.IPV6;
                            } else if (address instanceof Inet4Address) {
                                type = Firewall.AddressType.IPV4;
                            } else {
                                throw new Exception("Unknown ip address type");
                            }

                            firewallRules = new FirewallRule[1];
                            firewallRules[0] = new FirewallRule(FirewallRule.RuleType.DENY, type);
                            firewallRules[0].setIpAddress(ip);
                            firewallRules[0].setPortNumber(port);
                            firewallRules[0].setApplication(new AppIdentity(packageName, null));
                        }

                        firewallUtils.addFirewallRules(firewallRules, handler);
                    } else {
                        LogUtils.info("The firewall rule is already been enabled", handler);
                    }

                    ++count;
                }
            }
        }

        LogUtils.info("Custom rule size: " + count, handler);
    }

    private void processMobileRestrictedApps() throws Exception {
        LogUtils.info("\nProcessing mobile restricted apps...", handler);

        List<AppInfo> restrictedApps = appDatabase.applicationInfoDao().getMobileRestrictedApps();
        int size = restrictedApps.size();
        LogUtils.info("Size: " + size, handler);
        if (size == 0) {
            return;
        }

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        for (AppInfo app : restrictedApps) {
            String packageName = app.packageName;

            boolean add = true;
            for (FirewallRule enabledRule : enabledRules) {
                String packageName1 = enabledRule.getApplication().getPackageName();
                Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
                if (packageName1.equalsIgnoreCase(packageName) && networkInterface == Firewall.NetworkInterface.MOBILE_DATA_ONLY) {
                    add = false;
                    break;
                }
            }

            LogUtils.info("Package name: " + packageName, handler);
            if (add) {
                FirewallRule[] mobileRules = firewallUtils.createFirewallRules(packageName,
                        Firewall.NetworkInterface.MOBILE_DATA_ONLY);
                firewallUtils.addFirewallRules(mobileRules, handler);
            } else {
                LogUtils.info("The firewall rule is already been enabled", handler);
            }
        }
    }

    private void processWifiRestrictedApps() throws Exception {
        LogUtils.info("\nProcessing wifi restricted apps...", handler);

        List<AppInfo> restrictedApps = appDatabase.applicationInfoDao().getWifiRestrictedApps();
        int size = restrictedApps.size();
        LogUtils.info("Size: " + size, handler);
        if (size == 0) {
            return;
        }

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        for (AppInfo app : restrictedApps) {
            String packageName = app.packageName;

            boolean add = true;
            for (FirewallRule enabledRule : enabledRules) {
                String packageName1 = enabledRule.getApplication().getPackageName();
                Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
                if (packageName1.equalsIgnoreCase(packageName) && networkInterface == Firewall.NetworkInterface.WIFI_DATA_ONLY) {
                    add = false;
                    break;
                }
            }

            LogUtils.info("Package name: " + packageName, handler);
            if (add) {
                FirewallRule[] wifiRules = firewallUtils.createFirewallRules(packageName,
                        Firewall.NetworkInterface.WIFI_DATA_ONLY);
                firewallUtils.addFirewallRules(wifiRules, handler);
            } else {
                LogUtils.info("The firewall rule is already been enabled", handler);
            }
        }
    }

    private void processWhitelistedApps() throws Exception {
        LogUtils.info("\nProcessing white-listed apps...", handler);

        // Create domain filter rule for white listed apps
        List<AppInfo> whitelistedApps = appDatabase.applicationInfoDao().getWhitelistedApps();
        LogUtils.info("Size: " + whitelistedApps.size(), handler);
        if (whitelistedApps.size() == 0) {
            return;
        }

        List<DomainFilterRule> rules = new ArrayList<>();
        List<String> superAllow = new ArrayList<>();
        superAllow.add("*");
        for (AppInfo app : whitelistedApps) {
            LogUtils.info("Package name: " + app.packageName, handler);
            rules.add(new DomainFilterRule(new AppIdentity(app.packageName, null), new ArrayList<>(), superAllow));
        }
        firewallUtils.addDomainFilterRules(rules, handler);
    }

    private void processWhitelistedDomains() throws Exception {
        LogUtils.info("\nProcessing whitelist...", handler);

        // Process user-defined white list
        // 1. URL for all packages: url
        // 2. URL for individual package: packageName|url
        List<String> whiteUrls = appDatabase.whiteUrlDao().getAll3();
        LogUtils.info("Size: " + whiteUrls.size(), handler);
        if (whiteUrls.size() == 0) {
            return;
        }

        List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
        List<String> userList = BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null);
        denyList.addAll(userList);
        for (String whiteUrl : whiteUrls) {
            if (whiteUrl.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(whiteUrl, "|");
                if (tokens.countTokens() == 2) {
                    final String packageName = tokens.nextToken();
                    final String url = tokens.nextToken();
                    LogUtils.info("PackageName: " + packageName + ", Domain: " + url, handler);

                    final AppIdentity appIdentity = new AppIdentity(packageName, null);
                    List<String> allowList = new ArrayList<>();
                    allowList.add(url);
                    processDomains(appIdentity, denyList, allowList);
                }
            }
        }

        // Whitelist URL for all apps
        Set<String> allowList = new HashSet<>();
        for (String whiteUrl : whiteUrls) {
            if (whiteUrl.indexOf('|') == -1) {
                allowList.add(whiteUrl);
                LogUtils.info("Domain: " + whiteUrl, handler);
            }
        }
        if (allowList.size() > 0) {
            final AppIdentity appIdentity = new AppIdentity("*", null);
            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, new ArrayList<>(), new ArrayList<>(allowList)));
            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }

    private void processUserBlockedDomains() throws Exception {
        LogUtils.info("\nProcessing blacklist...", handler);

        List<String> denyList = BlockUrlUtils.getUserBlockedUrls(appDatabase, true, handler);
        if (denyList.size() > 0) {
            List<DomainFilterRule> rules = new ArrayList<>();
            final AppIdentity appIdentity = new AppIdentity("*", null);
            rules.add(new DomainFilterRule(appIdentity, denyList, new ArrayList<>()));
            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }

    private void processBlockedDomains(boolean updateProviders) throws Exception {
        LogUtils.info("\nProcessing blocked domains...", handler);

        if (updateProviders) {
            LogUtils.info("Updating providers...", handler);
            AdhellFactory.getInstance().updateAllProviders();
        }

        List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
        LogUtils.info("Total unique domains to block: " + denyList.size(), handler);
        AppPreferences.getInstance().setBlockedDomainsCount(denyList.size());

        final AppIdentity appIdentity = new AppIdentity("*", null);
        processDomains(appIdentity, denyList, new ArrayList<>());
    }

    private void processDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList) throws Exception {
        int start = 0;
        List<List<String>> chunks = Lists.partition(denyList, 5000);
        for (List<String> chunk : chunks) {
            LogUtils.info("\nProcessing " + start + " to " + (start + chunk.size()) + " domains...", handler);
            start += chunk.size();

            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, chunk, allowList));
            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }

    @Override
    public boolean isEnabled() {
        return firewall != null && firewall.isFirewallEnabled();
    }

    @Override
    public boolean isDomainRuleEmpty() {
        if (isEnabled()) {
            if (BlockUrlUtils.isDomainLimitAboveDefault()) {
                // If the domain count more than 15k, calling firewall.getDomainFilterRules() might crash the firewall
                int domainCount = AppPreferences.getInstance().getBlockedDomainsCount();
                return domainCount == 0;
            }

            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(Firewall.FIREWALL_ALL_PACKAGES);
            List<DomainFilterRule> rules = firewall.getDomainFilterRules(packageNameList);
            return rules != null && rules.size() == 0;
        }
        return true;
    }

    @Override
    public boolean isFirewallRuleEmpty() {
        if (isEnabled()) {
            FirewallRule[] rules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, null);
            return rules == null || rules.length == 0;
        }
        return true;
    }
}