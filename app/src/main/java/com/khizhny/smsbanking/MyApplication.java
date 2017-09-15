package com.khizhny.smsbanking;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.util.Log;

import java.util.Locale;

public class MyApplication extends Application {

    public static String defaultSmsApp;
    public final static String LOG = "SMS_BANKING";
    public static boolean hasReadSmsPermission=true;

    public static DatabaseAccess db;


    static boolean hideMatchedMessages ;
    static boolean hideNotMatchedMessages;
    static boolean ignoreClones;
    public static boolean forceRefresh;

    public static String language;
    private Locale locale = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG,"MyApplication.onCreate() started");
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

        // changing language
        super.onCreate();
        String lang;
        Configuration config = getBaseContext().getResources().getConfiguration();
        String systemLocale = getSystemLocale(config).getLanguage();
        language = settings.getString("language","(System language)");
        if (!language.equals("(System language)")) {
            lang="default";
            if (language.equals("Bulgarian")) lang="bg";
            if (language.equals("Russian")) lang="ru";
            if (language.equals("Ukrainian")) lang="uk";
            locale = new Locale(lang);
            Locale.setDefault(locale);
            setSystemLocale(config, locale);
            updateConfiguration(config);
        }
        Log.d(LOG,"MyApplication.onCreate() finished");
    }

    @Override
    public void onTerminate() {
        Log.d(LOG,"MyApplication.onTerminate() started");
        db.close();
        super.onTerminate();
    }

    @SuppressWarnings("deprecation")
    private static Locale getSystemLocale(Configuration config) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }

    @SuppressWarnings("deprecation")
    private static void setSystemLocale(Configuration config, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
    }

    @SuppressWarnings("deprecation")
    private void updateConfiguration(Configuration config) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getBaseContext().createConfigurationContext(config);
        } else {
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }

   public static void restart(Context context, int delay) {
        Log.d(LOG, "MyApplication.restart() started");
        if (delay == 0) delay = 1;

        Intent restartIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName() );
        PendingIntent intent = PendingIntent.getActivity(context, 0,restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
        System.exit(2);
        Log.d(LOG, "MyApplication.restart() finished");
    }

}
