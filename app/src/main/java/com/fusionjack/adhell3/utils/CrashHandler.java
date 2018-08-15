package com.fusionjack.adhell3.utils;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = LogUtils.class.getCanonicalName();
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static CrashHandler instance;

    private Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            PrintStream ps = createFile();
            ps.append("Build version: ").append(BuildConfig.VERSION_NAME).append(System.lineSeparator());
            ps.append("Knox API: ").append(Integer.toString(EnterpriseDeviceManager.getAPILevel())).append(System.lineSeparator());
            ps.append("Android API: ").append(Integer.toString(Build.VERSION.SDK_INT)).append(System.lineSeparator());
            ps.append(System.lineSeparator());
            throwable.printStackTrace(ps);
            ps.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        defaultHandler.uncaughtException(thread, throwable);
    }

    private PrintStream createFile() throws FileNotFoundException {
        File logFile = new File(Environment.getExternalStorageDirectory(), String.format("adhell_crash_%s.txt", getTimestamp()));
        return new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
    }

    private String getTimestamp() {
        return dateFormat.format(new Date());
    }
}
