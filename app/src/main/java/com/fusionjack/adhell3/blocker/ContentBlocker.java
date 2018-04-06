package com.fusionjack.adhell3.blocker;

public interface ContentBlocker {

    boolean enableBlocker();
    boolean disableBlocker();
    boolean isEnabled();

    void processCustomRules() throws Exception;
    void processMobileRestrictedApps() throws Exception;
    void processWhitelistedApps() throws Exception;
    void processWhitelistedDomains() throws Exception;
    void processBlockedDomains() throws Exception;
}
