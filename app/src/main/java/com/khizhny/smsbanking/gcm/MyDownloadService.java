package com.khizhny.smsbanking.gcm;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.khizhny.smsbanking.MainActivity;
import com.khizhny.smsbanking.MyApplication;
import com.khizhny.smsbanking.R;
import com.khizhny.smsbanking.model.Bank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.khizhny.smsbanking.MyApplication.db;

/**
 * Service to handle downloading files from Firebase Storage.
 */
@SuppressWarnings("VisibleForTests")
public class MyDownloadService extends MyBaseTaskService {

    private static final String TAG = "Storage#DownloadService";

    /** Actions **/
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String DOWNLOAD_COMPLETED = "download_completed";
    public static final String DOWNLOAD_ERROR = "download_error";

    /** Extras **/
    public static final String EXTRA_DOWNLOAD_PATH = "extra_download_path";
    private static final String EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded";

    private StorageReference mStorageRef;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Storage
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            // Get the path to download from the intent
            String downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH);
            downloadFromPath(downloadPath);
        }

        return START_REDELIVER_INTENT;
    }

    private void downloadFromPath(final String downloadPath) {
        Log.d(TAG, "downloadFromPath:" + downloadPath);

        // Mark task started
        taskStarted();
        showProgressNotification(getString(R.string.progress_downloading), 0, 0);

        // Download and get total bytes
        mStorageRef.child(downloadPath).getStream(
								(taskSnapshot, inputStream) -> {
										long totalBytes = taskSnapshot.getTotalByteCount();
										long bytesDownloaded = 0;

										byte[] buffer = new byte[1024];
										int size;

										// Output stream to write file
										OutputStream outputStream = new FileOutputStream(getApplicationContext().getCacheDir().getAbsolutePath()+"/loaded_bank.dat");

										while ((size = inputStream.read(buffer)) != -1) {
												bytesDownloaded += size;
												outputStream.write(buffer, 0, size);
												showProgressNotification(getString(R.string.progress_downloading),
																bytesDownloaded, totalBytes);
										}
										outputStream.flush();
										outputStream.close();
										inputStream.close();
								})
                .addOnSuccessListener(taskSnapshot -> {
										Log.d(TAG, "download:SUCCESS");

										// Send success broadcast with number of bytes downloaded
										if (broadcastDownloadFinished(downloadPath, taskSnapshot.getTotalByteCount())) {
												showDownloadFinishedNotification(downloadPath, (int) taskSnapshot.getTotalByteCount());
										}
										Bank bank;
										// importing downloaded template to DB
										File cache=getApplicationContext().getCacheDir();
										if (cache!=null) {
												Bank loadedBank = Bank.importBank(cache.getAbsolutePath() + "/loaded_bank.dat");
												if (loadedBank != null) {
														bank = new Bank(loadedBank);
														db.addOrEditBank(bank, true, true);
														db.setActiveBank(bank.getId());
														MyApplication.forceRefresh = true;
												} else {
														// Send failure broadcast
														showImportFailedNotification("Import failed.");
												}
										}else{
												// Send failure broadcast
												showImportFailedNotification("Cache dir not found.");
										}

										// Mark task completed
										taskCompleted();
								})
                .addOnFailureListener(exception -> {
										Log.w(TAG, "download:FAILURE", exception);

										// Send failure broadcast
										if (broadcastDownloadFinished(downloadPath, -1)) {
												showDownloadFinishedNotification(downloadPath, -1);
										}
										// Mark task completed
										taskCompleted();
								});
    }

    /**
     * Broadcast finished download (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    private boolean broadcastDownloadFinished(String downloadPath, long bytesDownloaded) {
        boolean success = bytesDownloaded != -1;
        String action = success ? DOWNLOAD_COMPLETED : DOWNLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded);
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    /**
     * Show a notification for a finished download.
     */
    private void showDownloadFinishedNotification(String downloadPath, int bytesDownloaded) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to MainActivity
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean success = bytesDownloaded != -1;
        String caption = success ? getString(R.string.download_success) : getString(R.string.download_failure);
        showFinishedNotification(caption, intent, success);
    }

    /**
     * Show a notification for a failed import.
     */
    private void showImportFailedNotification(String msg) {
        // Make Intent to MainActivity
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        showFinishedNotification(msg, intent, false);
    }


    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DOWNLOAD_COMPLETED);
        filter.addAction(DOWNLOAD_ERROR);

        return filter;
    }

}
