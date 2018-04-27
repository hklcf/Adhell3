package com.fusionjack.adhell3.blocker;

import android.app.enterprise.FirewallPolicy;
import android.os.Handler;
import android.util.Log;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentBlocker20 implements ContentBlocker {
    private static ContentBlocker20 mInstance = null;
    private final String LOG_TAG = ContentBlocker20.class.getCanonicalName();

    private FirewallPolicy firewallPolicy;
    private AppDatabase appDatabase;
    private int urlBlockLimit = 10;

    private ContentBlocker20() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.firewallPolicy = AdhellFactory.getInstance().getFirewallPolicy();
    }

    public static ContentBlocker20 getInstance() {
        if (mInstance == null) {
            mInstance = getSync();
        }
        return mInstance;
    }

    private static synchronized ContentBlocker20 getSync() {
        if (mInstance == null) {
            mInstance = new ContentBlocker20();
        }
        return mInstance;
    }

    public void setUrlBlockLimit(int urlBlockLimit) {
        this.urlBlockLimit = urlBlockLimit;
    }

    @Override
    public void enableDomainRules() {
        disableDomainRules();
        Log.d(LOG_TAG, "Entering enableDomainRules() method...");
        try {
            Log.d(LOG_TAG, "Check if Adhell enabled. Disable if true");
            Log.d(LOG_TAG, "Loading block list rules");
            List<String> denyList = new ArrayList<>();
            denyList.addAll(prepare());
            Log.i(LOG_TAG, "denyList size is: " + denyList.size());
            boolean isAdded = firewallPolicy.addIptablesRerouteRules(denyList);
            boolean isRulesEnabled = firewallPolicy.setIptablesOption(true);
            Log.d(LOG_TAG, "Re-route rules added: " + isAdded);

            Log.d(LOG_TAG, "Rules enabled: " + isRulesEnabled);
            Log.d(LOG_TAG, "Leaving enableDomainRules() method");
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Failed to enable Adhell:", e);
            Log.d(LOG_TAG, "Leaving enableDomainRules() method");
        }
    }

    @Override
    public void disableDomainRules() {
        Log.d(LOG_TAG, "Entering disableDomainRules() method...");
        try {
            firewallPolicy.cleanIptablesAllowRules();
            firewallPolicy.cleanIptablesDenyRules();
            firewallPolicy.cleanIptablesProxyRules();
            firewallPolicy.cleanIptablesRedirectExceptionsRules();
            firewallPolicy.cleanIptablesRerouteRules();
            firewallPolicy.removeIptablesRules();
            firewallPolicy.setIptablesOption(false);
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Failed to disable ContentBlocker", e);
            Log.d(LOG_TAG, "Leaving disableDomainRules() method");
        }
    }

    @Override
    public void enableFirewallRules() {
    }

    @Override
    public void disableFirewallRules() {
    }

    @Override
    public boolean isEnabled() {
        return firewallPolicy.getIptablesOption();
    }

    @Override
    public boolean isDomainRuleEmpty() {
        return !isEnabled();
    }

    @Override
    public boolean isFirewallRuleEmpty() {
        return true;
    }

    @Override
    public void setHandler(Handler handler) {
    }

    private List<String> getDenyUrl() {
        Log.d(LOG_TAG, "Entering prepareUrls");
        BlockUrlProvider standardBlockUrlProvider =
                appDatabase.blockUrlProviderDao().getByUrl(AdhellAppIntegrity.ADHELL_STANDARD_PACKAGE);
        List<BlockUrl> standardList = appDatabase.blockUrlDao().getUrlsByProviderId(standardBlockUrlProvider.id);
        List<UserBlockUrl> userBlockUrls = appDatabase.userBlockUrlDao().getAll2();

        List<String> denyList = new ArrayList<>();
        for (UserBlockUrl userBlockUrl : userBlockUrls) {
            denyList.add(userBlockUrl.url);
        }
        for (BlockUrl blockUrl : standardList) {
            denyList.add(blockUrl.url);
        }

        List<WhiteUrl> whiteUrls = appDatabase.whiteUrlDao().getAll2();

        List<String> whiteUrlsString = new ArrayList<>();
        for (WhiteUrl whiteUrl : whiteUrls) {
            whiteUrlsString.add(whiteUrl.url);
        }
        denyList.removeAll(whiteUrlsString);
        denyList = denyList.subList(0, urlBlockLimit);
        Log.i(LOG_TAG, denyList.toString());
        return denyList;
    }

    private Set<String> prepare() {
        List<String> urlsToCheck = getDenyUrl();
        Set<String> ipsToBlock = new HashSet<>();
        for (String url : urlsToCheck) {
            if (ipsToBlock.size() > 625) {
                break;
            }
            Log.i(LOG_TAG, "Checking url: " + url);
            try {
                InetAddress[] addresses = InetAddress.getAllByName(url);
                for (InetAddress inetAddress : addresses) {
                    ipsToBlock.add(inetAddress.getHostAddress() + ":*;127.0.0.1:80");
                    Log.i(LOG_TAG, "Address: " + inetAddress.getHostAddress());
                }
            } catch (UnknownHostException e) {
                Log.e(LOG_TAG, "Failed to resolve: " + url, e);
            }
        }
        return ipsToBlock;
    }
}
