package com.fusionjack.adhell3.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Created by fusionjack on 15/03/2018.
 */

public class LogUtils {

    private final String TAG = LogUtils.class.getCanonicalName();
    private static LogUtils instance;
    private PrintStream ps;
    private boolean closed = false;

    private LogUtils() {
        File logFile = new File(Environment.getExternalStorageDirectory(), "adhell_log.txt");
        if (logFile.exists()) {
            logFile.delete();
        }
        try {
            ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static LogUtils getInstance() {
        if (instance == null) {
            instance = new LogUtils();
        }
        return instance;
    }

    public void close() {
        ps.close();
        closed = true;
    }

    public void writeInfo(String text) {
        Log.i(TAG, text);
        writeText(text);
    }

    public void writeError(String text, Throwable e) {
        Log.e(TAG, text, e);
        writeText(text);
    }

    private void writeText(String text) {
        if (closed) {
            return;
        }
        ps.append(text);
        ps.append("\n");
        ps.flush();
    }
}
