package com.khizhny.smsbanking;

import android.Manifest;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.util.Log;

public class MyApplication extends Application {

    public static String defaultSmsApp;
    public final static String LOG = "SMS_BANKING";
    public static boolean hasReadSmsPermission=true;

    public static DatabaseAccess db;


    static boolean hideMatchedMessages ;
    static boolean hideNotMatchedMessages;
    static boolean ignoreClones;
    static boolean forceRefresh;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG,"=========================");
        db = DatabaseAccess.getInstance(this);
        db.open();
        Log.d(LOG,"New Application created...");
        // remembering default SMS application to restore on exit. Needed for SMS deleting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);
        }

        //check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasReadSmsPermission=(checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED);
        }

        // Restoring preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        hideMatchedMessages = settings.getBoolean("hide_matched_messages", false);
        hideNotMatchedMessages = settings.getBoolean("hide_not_matched_messages", false);
        ignoreClones = settings.getBoolean("ignore_clones", false);
    }

    @Override
    public void onTerminate() {
        db.close();
        super.onTerminate();
    }

}
