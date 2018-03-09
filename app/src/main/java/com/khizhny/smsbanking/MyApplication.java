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

import org.acra.*;
import org.acra.annotation.*;

import java.util.Locale;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo="khizhny@gmail.com",
				reportAsFile = false,
				resSubject = R.string.crash_mail_subject)
@AcraToast(resText = R.string.crash_mail_subject)

public class MyApplication extends Application {

		public static final String KEY_LANGUAGE = "language";
		public static final String KEY_EXTRA_1_NAME = "extra1_name";
		public static final String KEY_EXTRA_2_NAME = "extra2_name";
		public static final String KEY_EXTRA_3_NAME = "extra3_name";
		public static final String KEY_EXTRA_4_NAME = "extra4_name";
		public static final String KEY_HIDE_MATCHED_MESSAGES = "hide_matched_messages";
		public static final String KEY_HIDE_NOT_MATCHED_MESSAGES = "hide_not_matched_messages";
		public static final String KEY_IGNORE_CLONES = "ignore_clones";
		public static String defaultSmsApp;
		public final static String LOG = "SMS_BANKING";
		public static boolean hasReadSmsPermission=true;

		public static DatabaseAccess db;

		static boolean hideMatchedMessages ;
		static boolean hideNotMatchedMessages;
		static boolean ignoreClones;
		public static boolean forceRefresh;

		public static String language;



		@Override
		protected void attachBaseContext(Context base) {
				super.attachBaseContext(base);
				// The following line triggers the initialization of ACRA
				ACRA.init(this);
		}

		@Override
		public void onCreate() {
				super.onCreate();
				Log.d(LOG,"MyApplication.onCreate()");
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
				hideMatchedMessages = settings.getBoolean(KEY_HIDE_MATCHED_MESSAGES, false);
				hideNotMatchedMessages = settings.getBoolean(KEY_HIDE_NOT_MATCHED_MESSAGES, false);
				ignoreClones = settings.getBoolean(KEY_IGNORE_CLONES, false);



				//LoadingDefault preferences
				if (!settings.contains(KEY_LANGUAGE)) settings.edit().putString(KEY_LANGUAGE,getString(R.string.system_language)).apply();
				if (!settings.contains(KEY_EXTRA_1_NAME)) settings.edit().putString(KEY_EXTRA_1_NAME,getString(R.string.define_extra_parameter1)).apply();
				if (!settings.contains(KEY_EXTRA_2_NAME)) settings.edit().putString(KEY_EXTRA_2_NAME,getString(R.string.define_extra_parameter2)).apply();
				if (!settings.contains(KEY_EXTRA_3_NAME)) settings.edit().putString(KEY_EXTRA_3_NAME,getString(R.string.define_extra_parameter3)).apply();
				if (!settings.contains(KEY_EXTRA_4_NAME)) settings.edit().putString(KEY_EXTRA_4_NAME,getString(R.string.define_extra_parameter4)).apply();

				language=settings.getString(KEY_LANGUAGE,getString(R.string.system_language));
				if (!language.equals(getString(R.string.system_language))) {// changing language
						String lang="default";
						if (language.equals("Bulgarian")) lang="bg";
						if (language.equals("Russian")) lang="ru";
						if (language.equals("Ukrainian")) lang="uk";
						Locale locale = new Locale(lang);
						Locale.setDefault(locale);
						Configuration config = getBaseContext().getResources().getConfiguration();
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
		private static void setSystemLocale(Configuration config, Locale locale) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						config.setLocale(locale);
				} else {
						config.locale = locale;
				}
		}


		private void updateConfiguration(Configuration config) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
						getBaseContext().createConfigurationContext(config);
				} else {
						getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
				}
		}

	 public static void restart(Context context) {
				Log.d(LOG, "MyApplication.restart() started");
				Intent restartIntent = context.getPackageManager()
								.getLaunchIntentForPackage(context.getPackageName() );
				PendingIntent intent = PendingIntent.getActivity(context, 0,restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			 if (manager != null) {
					 manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1, intent);
					 System.exit(2);
					 Log.d(LOG, "MyApplication.restart() finished");
			 }
		}

		public static  String[] getExtraParameterNames(Context ctx){
				Log.d(LOG, "MyApplication.getExtraParameterNames()");
				// Restoring preferences
				String[] res=new String[4];
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
				res[0] = settings.getString(KEY_EXTRA_1_NAME, "" );
				res[1] = settings.getString(KEY_EXTRA_2_NAME, "");
				res[2] = settings.getString(KEY_EXTRA_3_NAME, "");
				res[3] = settings.getString(KEY_EXTRA_4_NAME, "");
				return res;
		}
}
