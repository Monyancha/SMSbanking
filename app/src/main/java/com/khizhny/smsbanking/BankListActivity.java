package com.khizhny.smsbanking;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.khizhny.smsbanking.gcm.MyDownloadService;
import com.khizhny.smsbanking.gcm.MyUploadService;
import com.khizhny.smsbanking.model.Bank;

import java.io.File;
import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;
import static com.khizhny.smsbanking.MyApplication.forceRefresh;

public class BankListActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private final static String EXPORT_FILE_EXTENSION="dat";
    private final static String EXPORT_FOLDER="/SMS banking";
    private final static String EXPORT_PATH=EXPORT_FOLDER+"/myBank_"+ Bank.serialVersionUID+"."+EXPORT_FILE_EXTENSION;
    private static final int REQUEST_CODE_ASK_PERMISSIONS=111;
    private static final int RC_SIGN_IN = 9001;

    private AlertDialog alertDialog;
    private ListView listView;
	private List<Bank> bankList;
    private List<Bank> bankTemplates;
	private BankListAdapter bankListAdapter;
    private static Bank bank2Share;
	private int selected_row;
    private String country;

    private FirebaseAuth mAuth;
    private GoogleApiClient googleApiClient;

    private ProgressBar progressBar;
    private BroadcastReceiver mBroadcastReceiver;

    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bank_list);

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOG, "onReceive:" + intent);
                showProgress(false);

                String s = intent.getAction();
                if (s.equals(MyDownloadService.DOWNLOAD_COMPLETED)) {

                } else if (s.equals(MyDownloadService.DOWNLOAD_ERROR)) {

                } else if (s.equals(MyUploadService.UPLOAD_COMPLETED) || s.equals(MyUploadService.UPLOAD_ERROR)) {
                    onUploadResultIntent(intent);
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();

        // Google signIn configuration
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(BankListActivity.this,"Error in Google API Client", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);

		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selected_row = position;

                PopupMenu popupMenu = new PopupMenu(BankListActivity.this, view);
				popupMenu.setOnMenuItemClickListener(BankListActivity.this);
                popupMenu.inflate(R.menu.popup_menu_my_banks);
				popupMenu.show();
			}
		});

		PopupMenu popupMenu = new PopupMenu(BankListActivity.this, listView);
		popupMenu.setOnMenuItemClickListener(BankListActivity.this);
        requestPermissions();
    }

    private void onUploadResultIntent(Intent intent) {
        // Got a new intent from MyUploadService with a success or failure
        Uri mDownloadUrl = intent.getParcelableExtra(MyUploadService.EXTRA_DOWNLOAD_URL);
        if (mDownloadUrl==null) {
            // File Upload failed
            Toast.makeText(BankListActivity.this,"File upload failed.",Toast.LENGTH_LONG).show();

        }else{
            // File Uploaded successfully
            Toast.makeText(BankListActivity.this,"File uploaded.",Toast.LENGTH_LONG).show();
        }
        showProgress(false);
    }

    @Override
	protected void onStart() {
		super.onStart();

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, MyDownloadService.getIntentFilter());
        manager.registerReceiver(mBroadcastReceiver, MyUploadService.getIntentFilter());
        country=getCountry();

		setTitle(getString(R.string.mybank_activity_title));
        bankList=db.getMyBanks(country);
        bankTemplates = db.getBankTemplates(country);

		for (int i=0;i<bankList.size();i++) {
			if (bankList.get(i).isActive()) {
				selected_row=i;
			}
		}

		bankListAdapter = new BankListAdapter(this, bankList);

		listView.setAdapter(bankListAdapter);
	}

    @Override
    protected void onStop() {
        if (alertDialog!=null) {
            if (alertDialog.isShowing()) alertDialog.dismiss();
        }
        super.onStop();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_banks, menu);
        return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mAuth.getCurrentUser()==null){
            menu.findItem(R.id.bank_sign_in_and_out).setTitle(R.string.action_sign_in);
        }else{
            menu.findItem(R.id.bank_sign_in_and_out).setTitle(R.string.sign_out);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handles ActionBar Menu item Clicks
		Intent intent = new Intent(this, BankActivity.class);
		switch (item.getItemId()) {
                // if back button clicked
            case android.R.id.home:
                finish();
                return true;

			case R.id.bank_add:
				// Calling Bank Activity to add new Bank
				intent.putExtra("todo", "add");
				startActivity(intent);
				bankListAdapter.notifyDataSetChanged();
				return true;

            case R.id.bank_template: // Copying bank settings from template to myBanks
                showTemplatePickDialog();
                return true;

            case R.id.bank_cloud_download:
                if (mAuth.getCurrentUser()!=null) {
                    importBankFromCloud();
                }else{
                    Toast.makeText(this, R.string.login_first,Toast.LENGTH_SHORT).show();
                }
				return true;

            // Logging  in or out
            case R.id.bank_sign_in_and_out:
                if (mAuth.getCurrentUser()==null) {
                    googleSignIn();
                }else{
                    googleSignOut();
                }
                return true;
        }
		return false;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// handles popup items clicks
		Bank selectedBank= bankListAdapter.getItem(selected_row);
		Intent intent;
         switch (item.getItemId()) {

			case R.id.bank_activate:// Marking selected bank as Active in DB
                db.setActiveBank(selectedBank.getId());
				bankList.clear();
				bankList.addAll(db.getMyBanks(country));
                forceRefresh=true;
				bankListAdapter.notifyDataSetChanged();
				Toast.makeText(BankListActivity.this, selectedBank.getName() + " " + getString(R.string.bank_activate_tip), Toast.LENGTH_SHORT).show();
				return true;

            case R.id.bank_clear_cache:
                db.deleteBankCache(selectedBank.getId());
                forceRefresh=true;
                Toast.makeText(BankListActivity.this, R.string.cache_deleted, Toast.LENGTH_SHORT).show();
                return true;

			case R.id.bank_edit: // Editing Active Bank from myBanks to sdcard
				intent= new Intent(this, BankActivity.class);
				intent.putExtra("todo", "edit");
				startActivity(intent);
				bankListAdapter.notifyDataSetChanged();
				return true;

			case R.id.bank_add: // Adding new Bank to myBanks manualy
				intent= new Intent(this, BankActivity.class);
				intent.putExtra("todo", "add");
				startActivity(intent);
				bankListAdapter.notifyDataSetChanged();
				return true;

            case R.id.bank_delete: // Deleting selected Bank from myBanks
                if (selectedBank.isActive()) {
                    db.deleteBank(selectedBank.getId());
                    db.setActiveAnyBank();
                }else{
                    db.deleteBank(selectedBank.getId());
                }
                bankList.clear();
                bankList.addAll(db.getMyBanks(country));

                bankListAdapter.notifyDataSetChanged();
                return true;

             case R.id.bank_cloud_upload:
                 bank2Share=selectedBank;
                 if (mAuth.getCurrentUser()==null){
                     Toast.makeText(this,R.string.login_first,Toast.LENGTH_SHORT).show();
                 }else {
                     exportBankToCloud();
                 }
                 return true;
		}
		return false;
	}

	public void showBankImportDialog(String filter, String startingPath) {
		File mPath=null;
		try {
			mPath = new File(startingPath);
		} catch(NullPointerException e){
			Log.d(LOG, "Unable to open file:" + startingPath);
		}
        // try to open file dialog from starting path
		FileDialog fileDialog=null;
		try {
			fileDialog = new FileDialog(this, mPath,filter);
		} catch (NullPointerException e) {
			Log.d(LOG,"Unable to open file pick dialog for");
		}
		if (fileDialog!=null) {
			fileDialog.setSelectDirectoryOption(false);
			fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String importPath;
                    importPath = file.getPath();
                    Log.d(LOG, "File picked " + importPath);
                    Bank b = (Bank) Bank.importBank(importPath);
                    if (!(b == null)) {
                        db.useTemplate(b);
                       BankListActivity.this.finish();
                    } else {
                        Toast.makeText(BankListActivity.this, getString(R.string.import_failed), Toast.LENGTH_SHORT).show();
                    }
                }

            });
			fileDialog.showDialog();
		}
	}

	private void showTemplatePickDialog(){
        String templates[] = new String[bankTemplates.size()];
        for (int i=0; i<bankTemplates.size(); i++){
            templates[i]=bankTemplates.get(i).getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pick_a_template);
        builder.setCancelable(true);
        builder.setItems(templates, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.open();
                db.useTemplate(bankTemplates.get(which));

                //Refreshing list
                bankList.clear();
                bankList.addAll(db.getMyBanks(country));
                bankListAdapter.notifyDataSetChanged();

                // Forcing Main Activity to refresh if we return there
                forceRefresh=true;
                alertDialog.dismiss();

                //BankListActivity.this.finish();

            }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }



    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void requestPermissions() {
        // requsting SD card access permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasWritePermission = ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showMessageOKCancel("Starting from Android 6.0 you need to allow access to SD",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                }
                            });
                    return;
                }
                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    private class BankListAdapter extends ArrayAdapter<Bank> {

        BankListAdapter(Context context, List<Bank> bankList) {
            super(context, R.layout.activity_bank_list_row, bankList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = vi.inflate(R.layout.activity_bank_list_row, parent, false);
            }

            TextView bankNameView = (TextView) rowView.findViewById(R.id.label);
            bankNameView.setText(bankList.get(position).getName());

            TextView bankPhoneView = (TextView) rowView.findViewById(R.id.bankPhone);
            bankPhoneView.setText(bankList.get(position).getPhone());

            TextView bankCurrencyView = (TextView) rowView.findViewById(R.id.bankCurrency);
            bankCurrencyView.setText(bankList.get(position).getDefaultCurrency());

            TextView bankValueView = (TextView) rowView.findViewById(R.id.bankValue);
            bankValueView.setText(bankList.get(position).getCurrentAccountState());

            if (!bankList.get(position).isEditable()) {
                // IF activity used for showing templates
                rowView.findViewById(R.id.active).setVisibility(View.GONE);
                rowView.findViewById(R.id.bankValue).setVisibility(View.GONE);
                rowView.findViewById(R.id.bankCurrency).setVisibility(View.GONE);
            } else {
                // IF activity used for showing My Banks
                rowView.findViewById(R.id.active).setVisibility(View.VISIBLE);
                rowView.findViewById(R.id.bankValue).setVisibility(View.VISIBLE);
                rowView.findViewById(R.id.bankCurrency).setVisibility(View.VISIBLE);
            }

            RadioButton RadioButtonView = (RadioButton) rowView.findViewById(R.id.active);
            RadioButtonView.setChecked(bankList.get(position).isActive());
            RadioButtonView.setFocusable(false);
            RadioButtonView.setClickable(false);
            RadioButtonView.setTag(position);
            return rowView;
        }

    }

    private void importBankFromCloud(){
        //showBankImportDialog(EXPORT_FILE_EXTENSION, Environment.getExternalStorageDirectory().getPath());

        requestPermissions();
        Intent intent = new Intent(this, PostsActivity.class);

        startActivity(intent);
        //showBankImportDialog(EXPORT_FILE_EXTENSION,Environment.getExternalStorageDirectory().getPath());

    }

    private void exportBankToCloud(){
        String exportPath=Environment.getExternalStorageDirectory().getPath()+EXPORT_PATH;
        showProgress(true);

        // Saving File locally
        Uri localUri =Bank.exportBank(bank2Share, exportPath);

        if (localUri!=null) {
            // bank file saved to SD successful

            // Start MyUploadService to upload the file, so that the file is uploaded
            // even if this Activity is killed or put in the background
            startService(new Intent(this, MyUploadService.class)
                    .putExtra(MyUploadService.EXTRA_FILE_URI, localUri)
                    .putExtra(MyUploadService.EXTRA_COUNTRY, country)
                    .putExtra(MyUploadService.EXTRA_BANK, bank2Share)
                    .setAction(MyUploadService.ACTION_UPLOAD));
            //Download result is handled in onUploadResultIntent
        } else {
            // bank file saved to SD failed
            Toast.makeText(BankListActivity.this, getString(R.string.export_failed), Toast.LENGTH_LONG).show();
            showProgress(false);
        }
    }


    private void googleSignIn(){
        showProgress(true);
        Intent intent=Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(intent,RC_SIGN_IN);
    }

    private void googleSignOut() {
        //Firebase sign out
        mAuth.signOut();

        //Google signOut
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {

                    }
                }
        );
    }

    private void revokeAccess(){
        //Firebase sign out
        mAuth.signOut();
        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {

            }
        });
    }

    /*
     * Google sign In callback
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode== RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()){
                // Google SignIn successful.
                Log.d(LOG,"onActivityResult:Successful");

                // Now try auth in Firebase
                GoogleSignInAccount googleSignInAccount = result.getSignInAccount();
                String name=googleSignInAccount.getDisplayName();
                fireBaseAuthWithGoogle(googleSignInAccount);
            }else{
                Log.d(LOG,"onActivityResult:Unsuccessful "+result.getStatus());
                // SignIn failed
                Toast.makeText(BankListActivity.this, "Firebase login problem. Or SHA-1 fingerprint mismatch!", Toast.LENGTH_LONG).show();
                showProgress(false);
            }
        }
    }



    /**
     *Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });/**/
    }

    private void fireBaseAuthWithGoogle(GoogleSignInAccount googleSignInAccount){
        Log.d(LOG,"FirebaseAuthWithGoogle:" + googleSignInAccount.getId());
        showProgress(true);

        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(),null);
        mAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(LOG,"signInWithCredential:OnComplete:" + task.isSuccessful());
                        showProgress(false);
                        if (task.isSuccessful()) {
                            // if sign in successful then auth state listener will handle it.
                                        Toast.makeText(BankListActivity.this,"Welcome "+getUserName(),Toast.LENGTH_LONG).show();
                            Log.d(LOG, "signInWithCredential:success");
                        }else{
                            // if sign in fails show message to user.
                            Log.d(LOG, "signInWithCredential:failed");
                            Toast.makeText(BankListActivity.this,"Authentication failed.",Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(MyUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent);
        }
    }


    private void showMessageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    public String getCountry(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return settings.getString("country_preference",null);
    }

    public String getUserName() {
        FirebaseUser user=mAuth.getCurrentUser();
        for (UserInfo i : user.getProviderData()){
            if (i.getDisplayName()!=null) {
                return i.getDisplayName();
            }
        }
        return "Anonymous";
    }
}
