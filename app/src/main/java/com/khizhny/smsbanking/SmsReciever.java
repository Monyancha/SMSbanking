package com.khizhny.smsbanking;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import xml.SmsBankingWidget;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class SmsReciever extends  android.content.BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Log.d(LOG,"SMS_RECEIVED broadcast recieved.. Sending update intent to all widgets .");
            //update all widgets if new SMS recieve
            ComponentName name = new ComponentName(context, SmsBankingWidget.class);
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
            Intent update = new Intent();
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            context.sendBroadcast(update);
        }
    }

}

