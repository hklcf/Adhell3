package com.fusionjack.adhell3.blocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Patterns;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.sec.enterprise.AppIdentity;
import com.sec.enterprise.firewall.DomainFilterRule;
import com.sec.enterprise.firewall.Firewall;

import java.util.ArrayList;
import java.util.List;

public class ContentBlocker57 implements ContentBlocker {
    private static final String TAG = ContentBlocker57.class.getCanonicalName();
    private static ContentBlocker57 mInstance = null;

    private ContentBlocker56 contentBlocker56;

    private ContentBlocker57() {
        contentBlocker56 = ContentBlocker56.getInstance();
    }

    private static synchronized ContentBlocker57 getSync() {
        if (mInstance == null) {
            mInstance = new ContentBlocker57();
        }
        return mInstance;
    }

    public static ContentBlocker57 getInstance() {
        if (mInstance == null) {
            mInstance = getSync();
        }
        return mInstance;
    }

    @Override
    public boolean enableBlocker() {
        if (contentBlocker56.enableBlocker()) {
            SharedPreferences sharedPreferences = App.get().getApplicationContext().getSharedPreferences("dnsAddresses", Context.MODE_PRIVATE);
            if (sharedPreferences.contains("dns1") && sharedPreferences.contains("dns2")) {
                String dns1 = sharedPreferences.getString("dns1", null);
                String dns2 = sharedPreferences.getString("dns2", null);
                if (dns1 != null && dns2 != null
                        && Patterns.IP_ADDRESS.matcher(dns1).matches()
                        && Patterns.IP_ADDRESS.matcher(dns2).matches()) {
                    this.setDns(dns1, dns2);
                }
                Log.d(TAG, "Previous dns addresses has been applied. " + dns1 + " " + dns2);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean disableBlocker() {
        return contentBlocker56.disableBlocker();
    }

    @Override
    public boolean isEnabled() {
        return contentBlocker56.isEnabled();
    }

    @Override
    public void processCustomRules() throws Exception {
        contentBlocker56.processCustomRules();
    }

    @Override
    public void processMobileRestrictedApps() throws Exception {
        contentBlocker56.processMobileRestrictedApps();
    }

    @Override
    public void processWhitelistedApps() throws Exception {
        contentBlocker56.processWhitelistedApps();
    }

    @Override
    public void processWhitelistedDomains() throws Exception {
        contentBlocker56.processWhitelistedDomains();
    }

    @Override
    public void processBlockedDomains() throws Exception {
        contentBlocker56.processBlockedDomains();
    }

    public void setDns(String dns1, String dns2) {
        DomainFilterRule domainFilterRule = new DomainFilterRule(new AppIdentity(Firewall.FIREWALL_ALL_PACKAGES, null));
        domainFilterRule.setDns1(dns1);
        domainFilterRule.setDns2(dns2);
        List<DomainFilterRule> rules = new ArrayList<>();
        rules.add(domainFilterRule);
        AdhellFactory.getInstance().getFirewall().addDomainFilterRules(rules);
        Log.d(TAG, "DNS1: " + domainFilterRule.getDns1());
        Log.d(TAG, "DNS2: " + domainFilterRule.getDns2());
    }
}
