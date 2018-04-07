package com.fusionjack.adhell3.fragments;

public class AppFlag {

    public enum Flag {
        DISABLER_FLAG,
        RESTRICTED_FLAG,
        WHITELISTED_FLAG
    }

    private Flag flag;

    private AppFlag(Flag flag) {
        this.flag = flag;
    }

    public static AppFlag createDisablerFlag() {
        return new AppFlag(Flag.DISABLER_FLAG);
    }

    public static AppFlag createRestrictedFlag() {
        return new AppFlag(Flag.RESTRICTED_FLAG);
    }

    public static AppFlag createWhitelistedFlag() {
        return new AppFlag(Flag.WHITELISTED_FLAG);
    }

    public Flag getFlag() {
        return flag;
    }
}
