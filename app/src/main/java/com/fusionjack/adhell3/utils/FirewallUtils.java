package com.fusionjack.adhell3.utils;

import android.os.Handler;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.net.firewall.DomainFilterReport;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.net.firewall.FirewallResponse;
import com.samsung.android.knox.net.firewall.FirewallRule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class FirewallUtils {
    private static FirewallUtils instance;
    private Firewall firewall;
    private AppDatabase appDatabase;

    private FirewallUtils() {
        firewall = AdhellFactory.getInstance().getFirewall();
        appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static FirewallUtils getInstance() {
        if (instance == null) {
            instance = new FirewallUtils();
        }
        return instance;
    }

    public void addDomainFilterRules(List<DomainFilterRule> domainRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.addDomainFilterRules(domainRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add domain filter rule to Knox Firewall", ex, handler);
        }
    }

    public void addFirewallRules(FirewallRule[] firewallRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.addRules(firewallRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.getInstance().writeError("Failed to add firewall rules to Knox Firewall", ex, handler);
        }
    }

    public FirewallRule[] createFirewallRules(String packageName, Firewall.NetworkInterface networkInterface) {
        FirewallRule[] rules = new FirewallRule[2];

        rules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
        rules[0].setNetworkInterface(networkInterface);
        rules[0].setApplication(new AppIdentity(packageName, null));

        rules[1] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV6);
        rules[1].setNetworkInterface(networkInterface);
        rules[1].setApplication(new AppIdentity(packageName, null));

        return rules;
    }

    public DomainStat getDomainStatFromKnox() {
        DomainStat stat = new DomainStat();
        if (firewall == null) {
            return stat;
        }

        if (BlockUrlUtils.isDomainLimitAboveDefault()) {
            // If the domain count more than 15k, calling firewall.getDomainFilterRules() might crash the firewall
            stat.blackListSize = AppPreferences.getInstance().getBlockedDomainsCount();
            stat.whiteListSize = appDatabase.whiteUrlDao().getAll3().size();
        } else {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(Firewall.FIREWALL_ALL_PACKAGES);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            if (domainRules != null && domainRules.size() > 0) {
                stat.blackListSize = domainRules.get(0).getDenyDomains().size();
                stat.whiteListSize = domainRules.get(0).getAllowDomains().size();
            }
        }

        return stat;
    }

    public int getWhitelistAppCountFromKnox() {
        if (firewall == null) {
            return 0;
        }

        int whitelistedSize = 0;
        List<String> packageNameList = new ArrayList<>();
        List<AppInfo> appInfos = appDatabase.applicationInfoDao().getWhitelistedApps();
        for (AppInfo appInfo : appInfos) {
            packageNameList.clear();
            packageNameList.add(appInfo.packageName);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            if (domainRules != null && domainRules.size() > 0) {
                whitelistedSize += domainRules.get(0).getAllowDomains().size();
            }
        }
        return whitelistedSize;
    }

    public FirewallStat getFirewallStatFromKnox() {
        FirewallStat stat = new FirewallStat();
        if (firewall == null) {
            return stat;
        }

        FirewallRule[] firewallRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, null);
        if (firewallRules != null) {
            for (FirewallRule firewallRule : firewallRules) {
                Firewall.NetworkInterface networkInterfaces = firewallRule.getNetworkInterface();
                switch (networkInterfaces) {
                    case ALL_NETWORKS:
                        stat.allNetworkSize++;
                        break;
                    case MOBILE_DATA_ONLY:
                        stat.mobileDataSize++;
                        break;
                    case WIFI_DATA_ONLY:
                        stat.wifiDataSize++;
                        break;
                }
            }
        }
        return stat;
    }

    public List<ReportBlockedUrl> getReportBlockedUrl() {
        List<ReportBlockedUrl> reportBlockedUrls = new ArrayList<>();
        if (firewall == null) {
            return reportBlockedUrls;
        }

        List<DomainFilterReport> reports = firewall.getDomainFilterReport(null);
        if (reports == null) {
            return reportBlockedUrls;
        }

        long yesterday = yesterday();
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        appDatabase.reportBlockedUrlDao().deleteBefore(yesterday);

        ReportBlockedUrl lastBlockedUrl = appDatabase.reportBlockedUrlDao().getLastBlockedDomain();
        long lastBlockedTimestamp = 0;
        if (lastBlockedUrl != null) {
            lastBlockedTimestamp = lastBlockedUrl.blockDate / 1000;
        }

        for (DomainFilterReport b : reports) {
            if (b.getTimeStamp() > lastBlockedTimestamp) {
                ReportBlockedUrl reportBlockedUrl =
                        new ReportBlockedUrl(b.getDomainUrl(), b.getPackageName(), b.getTimeStamp() * 1000);
                reportBlockedUrls.add(reportBlockedUrl);
            }
        }
        appDatabase.reportBlockedUrlDao().insertAll(reportBlockedUrls);

        return appDatabase.reportBlockedUrlDao().getReportBlockUrlBetween(yesterday, System.currentTimeMillis());
    }

    private long yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTimeInMillis();
    }

    private void handleResponse(FirewallResponse[] response, Handler handler) throws Exception {
        if (response == null) {
            Exception ex = new Exception("There was no response from Knox Firewall");
            LogUtils.getInstance().writeError("There was no response from Knox Firewall", ex, handler);
            throw ex;
        } else {
            if (FirewallResponse.Result.SUCCESS == response[0].getResult()) {
                LogUtils.getInstance().writeInfo("Result: Success", handler);
            } else {
                LogUtils.getInstance().writeInfo("Result: Failed", handler);
                Exception ex = new Exception(response[0].getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                LogUtils.getInstance().writeError(sw.toString(), ex, handler);
                throw ex;
            }
        }
    }

    public class FirewallStat {
        public int mobileDataSize;
        public int wifiDataSize;
        public int allNetworkSize;
    }

    public class DomainStat {
        public int blackListSize;
        public int whiteListSize;
    }
}
