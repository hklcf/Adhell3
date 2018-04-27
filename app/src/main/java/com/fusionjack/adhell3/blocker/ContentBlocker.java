package com.fusionjack.adhell3.blocker;

import android.os.Handler;

public interface ContentBlocker {
    void enableDomainRules();
    void disableDomainRules();
    void enableFirewallRules();
    void disableFirewallRules();
    boolean isEnabled();
    boolean isDomainRuleEmpty();
    boolean isFirewallRuleEmpty();
    void setHandler(Handler handler);
}
