package com.fusionjack.adhell3.utils;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by fusionjack on 15/03/2018.
 */

public class LogUtils {

    private static final String TAG = LogUtils.class.getCanonicalName();
    private static LogUtils instance;

    private LogUtils() {
    }

    public static LogUtils getInstance() {
        if (instance == null) {
            instance = new LogUtils();
        }
        return instance;
    }

    public static String createLogcat() {
        String filename = String.format("adhell_logcat_%s.txt", System.currentTimeMillis());
        File logFile = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            Runtime.getRuntime().exec( "logcat -f " + logFile + " | grep com.fusionjack.adhell3");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return "";
        }
        return filename;
    }

    public void writeInfo(String text, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.i(TAG, text);
    }

    void writeError(String text, Throwable e, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.e(TAG, text, e);
    }
}
