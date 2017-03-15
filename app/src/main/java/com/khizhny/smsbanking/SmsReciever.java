package com.khizhny.smsbanking;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import xml.SmsBankingWidget;

public class SmsReciever extends  android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Bundle bundle = intent.getExtras();
        /*}

        if (bundle != null)
        {*/
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
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

