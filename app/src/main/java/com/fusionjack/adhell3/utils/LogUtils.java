package com.fusionjack.adhell3.utils;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by fusionjack on 15/03/2018.
 */

public final class LogUtils {

    private LogUtils() {
    }

    public static String createLogcat() {
        info("Build version: " + BuildConfig.VERSION_NAME);
        info("Knox API: " + Integer.toString(EnterpriseDeviceManager.getAPILevel()));
        info("Android API: " + Integer.toString(Build.VERSION.SDK_INT));
        String filename = String.format("adhell_logcat_%s.txt", System.currentTimeMillis());
        File logFile = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            Runtime.getRuntime().exec( "logcat -f " + logFile + " | grep com.fusionjack.adhell3");
        } catch (IOException e) {
            error(e.getMessage(), e);
            return "";
        }
        return filename;
    }

    public static void info(String text) {
        Log.i(getCallerInfo(), text);
    }

    public static void info(String text, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.i(getCallerInfo(), text);
    }

    public static void error(String text) {
        Log.e(getCallerInfo(), text);
    }

    public static void error(String text, Throwable e) {
        Log.e(getCallerInfo(), text, e);
    }

    public static void error(String text, Throwable e, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.e(getCallerInfo(), text, e);
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length > 4) {
            return stackTraceElements[4].getClassName() + "(" + stackTraceElements[4].getMethodName() + ")";
        }
        return "Empty class name";
    }
}
