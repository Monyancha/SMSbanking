package com.khizhny.smsbanking.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.khizhny.smsbanking.R;

/**
 * Base class for Services that keep track of the number of active jobs and self-stop when the
 * count is zero.
 */
public abstract class MyBaseTaskService extends Service {

		static final int PROGRESS_NOTIFICATION_ID = 0;
    static final int FINISHED_NOTIFICATION_ID = 1;

    private static final String TAG = "MyBaseTaskService";
		public static final String SMS_BANKING_NOTIFICATION = "sms_banking_notification";
		private int mNumTasks = 0;

    public void taskStarted() {
        changeNumberOfTasks(1);
    }

    public void taskCompleted() {
        changeNumberOfTasks(-1);
    }

    private synchronized void changeNumberOfTasks(int delta) {
        Log.d(TAG, "changeNumberOfTasks:" + mNumTasks + ":" + delta);
        mNumTasks += delta;

        // If there are no tasks left, stop the service
        if (mNumTasks <= 0) {
            Log.d(TAG, "stopping");
            stopSelf();
        }
    }

    /**
     * Show notification with a progress bar.
     */
    protected void showProgressNotification(String caption, long completedUnits, long totalUnits) {
        int percentComplete = 0;
        if (totalUnits > 0) {
            percentComplete = (int) (100 * completedUnits / totalUnits);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SMS_BANKING_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_notification_icon_upload)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(caption)
                .setProgress(100, percentComplete, false)
                .setOngoing(true)
                .setAutoCancel(false);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(PROGRESS_NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Show notification that the activity finished.
     */
    protected void showFinishedNotification(String caption, Intent intent, boolean success) {
        // Make PendingIntent for notification
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = success ? R.drawable.ic_check_white_24dp: R.drawable.ic_error_white_24dp;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SMS_BANKING_NOTIFICATION)
                .setSmallIcon(icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(caption)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(FINISHED_NOTIFICATION_ID, builder.build());
        }
				Toast.makeText(this,caption,Toast.LENGTH_LONG).show();
    }

    /**
     * Dismiss the progress notification.
     */
    protected void dismissProgressNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.cancel(PROGRESS_NOTIFICATION_ID);
        }
    }
}
