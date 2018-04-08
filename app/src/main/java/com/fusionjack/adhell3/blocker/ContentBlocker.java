package com.fusionjack.adhell3.blocker;

import android.os.Handler;

public interface ContentBlocker {
    boolean enableBlocker();
    boolean disableBlocker();
    boolean isEnabled();
    void setHandler(Handler handler);
}
