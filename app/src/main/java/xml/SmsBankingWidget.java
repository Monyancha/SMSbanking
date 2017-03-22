package xml;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.khizhny.smsbanking.Bank;
import com.khizhny.smsbanking.DatabaseAccess;
import com.khizhny.smsbanking.MainActivity;
import com.khizhny.smsbanking.R;
import com.khizhny.smsbanking.Transaction;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SmsBankingWidgetConfigureActivity SmsBankingWidgetConfigureActivity}
 */
public class SmsBankingWidget extends AppWidgetProvider {

    private static final String LOG = "SMS_BANKING_WIDGET";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.d(LOG,"-=Start updating widget appWidgetId="+appWidgetId);
        int bankId=SmsBankingWidgetConfigureActivity.loadSavedIntFromPref(context, appWidgetId, SmsBankingWidgetConfigureActivity.PREF_BANK_ID);
        int color=SmsBankingWidgetConfigureActivity.loadSavedIntFromPref(context, appWidgetId, SmsBankingWidgetConfigureActivity.PREF_COLOR);
        int backColor=SmsBankingWidgetConfigureActivity.loadSavedIntFromPref(context, appWidgetId, SmsBankingWidgetConfigureActivity.PREF_BACKGROUND);
        int fontSize=SmsBankingWidgetConfigureActivity.loadSavedIntFromPref(context, appWidgetId, SmsBankingWidgetConfigureActivity.PREF_SIZE);

        //delete widget if it has no bank id

        Log.d(LOG,"Opening db for bankId="+bankId);
        DatabaseAccess db = DatabaseAccess.getInstance(context);
        db.open();
        Bank bank= db.getBank(bankId);
        db.close();
        if (bank!=null) {
            Log.d(LOG,"Recalculating balance for bank:"+bank.getName());

            String balance=Transaction.getLastAccountState(Transaction.loadTransactions(bank,context)).toString();

            // Construct the RemoteViews object
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sms_banking_widget);
            remoteViews.setTextViewText(R.id.widget_text, bank.getName());
            remoteViews.setTextViewText(R.id.widget_value, balance);
            remoteViews.setTextColor(R.id.widget_value, color);
            remoteViews.setTextColor(R.id.widget_text, color);
            remoteViews.setFloat(R.id.widget_text, "setTextSize", fontSize);
            remoteViews.setFloat(R.id.widget_value, "setTextSize", fontSize);
            // Setting background
            remoteViews.setInt(R.id.widget_background_image, "setColorFilter", backColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                remoteViews.setInt(R.id.widget_background_image, "setImageAlpha",Color.alpha(backColor));
            } else {
                remoteViews.setInt(R.id.widget_background_image, "setAlpha",Color.alpha(backColor));
            }
            // Create an Intent to launch MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("bank_id", bankId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0);

            // attach an on-click listener to widget
            remoteViews.setOnClickPendingIntent(R.id.widget_value, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_text, pendingIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);/**/
        }
        Log.d(LOG,"Finished updating widget for BankId="+bankId);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d(LOG,"Widget on Update is called.");

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        Log.d(LOG,"Widget onDelete is called.");
        for (int appWidgetId : appWidgetIds) {
            SmsBankingWidgetConfigureActivity.deleteWidgetInfoFromPref(context, appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        SmsBankingWidgetConfigureActivity.deleteWidgetInfoFromPref(context);
    }
}

