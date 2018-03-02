package com.khizhny.smsbanking.gcm;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.khizhny.smsbanking.R;
import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.BankListActivity;
import com.khizhny.smsbanking.model.Post;
import com.khizhny.smsbanking.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle uploading files to Firebase Storage.
 */
@SuppressWarnings("VisibleForTests")
public class MyUploadService extends MyBaseTaskService implements
        OnProgressListener<UploadTask.TaskSnapshot>,
        OnSuccessListener<UploadTask.TaskSnapshot>,
        OnFailureListener{

    private static final String TAG = "MyUploadService";

    /** Intent Actions **/
    public static final String ACTION_UPLOAD = "action_upload";
    public static final String UPLOAD_COMPLETED = "upload_completed";
    public static final String UPLOAD_ERROR = "upload_error";

    /** Intent Extras **/
    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";
    public static final String EXTRA_BANK = "extra_bank";
    public static final String EXTRA_COUNTRY = "extra_country";

    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private Bank bank;
    private String country;
    private Uri fileLocalUri;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);
        if (ACTION_UPLOAD.equals(intent.getAction())) {
            fileLocalUri = intent.getParcelableExtra(EXTRA_FILE_URI);
            bank= (Bank) intent.getSerializableExtra(EXTRA_BANK);
            country = intent.getStringExtra(EXTRA_COUNTRY);

            long time= System.currentTimeMillis(); // time is used to avoid conflicts
            uploadFromUri(bank.getName()+"_by_"+getUserName()+"_"+time+".dat");
        }

        return START_REDELIVER_INTENT;
    }

    private void uploadFromUri(String firebaseFileName) {
        Log.d(TAG, "uploadFromUri:src:" + fileLocalUri.toString());

        taskStarted();
        showProgressNotification("progress_uploading", 0, 0);

        final StorageReference bankTemplateRef = mStorageRef.child("banks V"+ Bank.serialVersionUID)
                .child(country)
                .child(firebaseFileName);

        // Upload file to Firebase Storage
        Log.d(TAG, "uploadFromUri:dst:" + bankTemplateRef.getPath());
        UploadTask uploadTask = bankTemplateRef.putFile(fileLocalUri);
        uploadTask.addOnSuccessListener(this);
        uploadTask.addOnProgressListener(this);
        uploadTask.addOnFailureListener(this);
    }

    /**
     * Broadcast finished upload (success or failure).
     */
    private void broadcastUploadFinished(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        boolean success = downloadUrl != null;

        String action = success ? UPLOAD_COMPLETED : UPLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_FILE_URI, fileUri)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl);
				LocalBroadcastManager.getInstance(getApplicationContext())
								.sendBroadcast(broadcast);
		}

    /**
     * Show a notification for a finished upload.
     */
    private void showUploadFinishedNotification(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to BankListActivity
        new Intent(this, BankListActivity.class)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                .putExtra(EXTRA_FILE_URI, fileUri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPLOAD_COMPLETED);
        filter.addAction(UPLOAD_ERROR);

        return filter;
    }

    private void writeNewUser() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

				String userId;
				if (firebaseUser != null) {
						userId = firebaseUser.getUid();
						String name=getUserName();
						String email=firebaseUser.getEmail();

						User user = new User(name, email);
						user.photoUri=getPhotoURI();
						mDatabase.child("users").child(userId).setValue(user);
				}

    }

    private void writeNewPost(String url) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String key = mDatabase.child("posts").push().getKey();
				String userId= null;
				if (firebaseUser != null) {
						userId = firebaseUser.getUid();
				}
				String author = getUserName();

        String currency = bank.getDefaultCurrency();
        String title = bank.getName();

        Post post = new Post(userId,author,title,url,currency,System.currentTimeMillis());
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<String, Object>();
        childUpdates.put("/posts V"+ Bank.serialVersionUID +"/"+country+"/" + key, postValues);
        childUpdates.put("/user-posts/"+ userId+"/" + key, postValues);
        mDatabase.updateChildren(childUpdates);
    }

    private String getUserName() {
        String temp;
        FirebaseUser user=mAuth.getCurrentUser();
        if (user!=null) {
						for (UserInfo i : user.getProviderData()) {
								temp = i.getDisplayName();
								if (temp != null) {
										if (!temp.equals("")) return temp;
								}
						}
				}
				return getString(R.string.anonymouse);
		}

    private String getPhotoURI() {
        FirebaseUser user=mAuth.getCurrentUser();
        if (user!=null) {
						for (UserInfo i : user.getProviderData()) {
								if (i.getPhotoUrl() != null) {
										return i.getPhotoUrl().toString();
								}
						}
				}
        return getString(R.string.anonymouse);
    }

    @SuppressWarnings("VisibleForTests")
    @Override
    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
        showProgressNotification("progress_uploading",
                taskSnapshot.getBytesTransferred(),
                taskSnapshot.getTotalByteCount());
    }

    @Override
    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        // Upload succeeded
        Log.d(TAG, "uploadFromUri:onSuccess");

        // Get the public download URL
				StorageMetadata m = taskSnapshot.getMetadata();
				Uri downloadUri;
				if (m!=null) {
						downloadUri=m.getDownloadUrl();
						writeNewUser();
						writeNewPost(m.getPath());

						broadcastUploadFinished(downloadUri, fileLocalUri);
						showUploadFinishedNotification(downloadUri, fileLocalUri);
				}
        taskCompleted();
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
        // Upload failed
        Log.w(TAG, "uploadFromUri:onFailure", exception);

        // [START_EXCLUDE]
        broadcastUploadFinished(null, fileLocalUri);
        showUploadFinishedNotification(null, fileLocalUri);
        taskCompleted();
        // [END_EXCLUDE]
    }
}
