package com.fusionjack.adhell3.blocker;

import android.os.Handler;

import com.fusionjack.adhell3.utils.AdhellFactory;

public class ContentBlocker57 implements ContentBlocker {
    private static final String TAG = ContentBlocker57.class.getCanonicalName();
    private static ContentBlocker57 mInstance = null;

    private ContentBlocker56 contentBlocker56;
    private Handler handler;

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
    public void enableDomainRules() {
        contentBlocker56.enableDomainRules();
        AdhellFactory.getInstance().applyDns(handler);
    }

    @Override
    public void disableDomainRules() {
        contentBlocker56.disableDomainRules();
    }

    @Override
    public void enableFirewallRules() {
        contentBlocker56.enableFirewallRules();
    }

    @Override
    public void disableFirewallRules() {
        contentBlocker56.disableFirewallRules();
    }

    @Override
    public boolean isEnabled() {
        return contentBlocker56.isEnabled();
    }

    @Override
    public boolean isDomainRuleEmpty() {
        return contentBlocker56.isDomainRuleEmpty();
    }

    @Override
    public boolean isFirewallRuleEmpty() {
        return contentBlocker56.isFirewallRuleEmpty();
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
        contentBlocker56.setHandler(handler);
    }
}
