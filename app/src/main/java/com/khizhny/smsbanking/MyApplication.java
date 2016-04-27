package com.khizhny.smsbanking;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

public class MyApplication extends Application {

    public static String defaultSmsApp;
    public final static String LOG = "SMS_BANKING";
    public static boolean hasReadSmsPermission=true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG,"=========================");
        Log.d(LOG,"New Application created...");
        // remembering default SMS application to restore on exit. Needed for SMS deleting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);
        }

        //check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasReadSmsPermission=(checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED);
        }
    }
}
