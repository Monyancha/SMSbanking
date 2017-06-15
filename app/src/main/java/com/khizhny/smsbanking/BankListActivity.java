package com.khizhny.smsbanking;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;
import static com.khizhny.smsbanking.MyApplication.forceRefresh;

public class BankListActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private AlertDialog alertDialog;
    private ListView listView;
	private List<Bank> bankList;
    private List<Bank> bankTemplates;
	private BankListAdapter adapter;
	private int selected_row;
    private final static String EXPORT_FILE_EXTENSION="dat";
    private final static String EXPORT_FOLDER="/SMS banking";
    private final static String EXPORT_PATH=EXPORT_FOLDER+"/myBank_"+Bank.serialVersionUID+"."+EXPORT_FILE_EXTENSION;
    private static int REQUEST_CODE_ASK_PERMISSIONS=111;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bank_list);
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


	@Override
	protected void onStart() {
		super.onStart();

        bankList=new ArrayList<Bank>();
		setTitle(getString(R.string.mybank_activity_title));
        bankList=db.getMyBanks();
        bankTemplates = db.getBankTemplates();

		for (int i=0;i<bankList.size();i++) {
			if (bankList.get(i).isActive()) {
				selected_row=i;
			}
		}

		adapter  = new BankListAdapter(this, bankList);

		listView.setAdapter(adapter);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handles Menu item Clicks
		Intent intent = new Intent(this, BankActivity.class);
		switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
			case R.id.bank_import:  // Importing Bank from sdcard to myBanks
				PickFileForImport(EXPORT_FILE_EXTENSION, Environment.getExternalStorageDirectory().getPath());
				return true;
			// and these are common options for My Banks and Template Banks
			case R.id.bank_add:
				// Calligng Bank Activity to add new Bank
				intent.putExtra("todo", "add");
				startActivity(intent);
				adapter.notifyDataSetChanged();
				return true;
            // these options just for bank Templates
            case R.id.bank_template: // Copying bank settings from template to myBanks
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
                        // Going back to Main Activity
                        forceRefresh=true;
                        BankListActivity.this.finish();
                    }
                });
                alertDialog = builder.create();
                alertDialog.show();
                return true;
		}
		return false;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// handles popup items clicks
		Bank selectedBank=adapter.getItem(selected_row);
		Intent intent;
         switch (item.getItemId()) {
			case R.id.bank_activate:// Marking selected bank as Active in DB
                db.setActiveBank(selectedBank.getId());
				bankList.clear();
				bankList.addAll(db.getMyBanks());
                forceRefresh=true;
				adapter.notifyDataSetChanged();
				Toast.makeText(BankListActivity.this, selectedBank.getName() + " " + getString(R.string.bank_activate_tip), Toast.LENGTH_SHORT).show();
				return true;

            case R.id.bank_clear_cache:
                db.deleteBankCache(selectedBank.getId());
                forceRefresh=true;
                Toast.makeText(BankListActivity.this, R.string.cache_deleted, Toast.LENGTH_SHORT).show();
                return true;

			case R.id.bank_share:
			case R.id.bank_export:
				String exportPath=Environment.getExternalStorageDirectory().getPath()+EXPORT_PATH;
				if (Bank.exportBank(selectedBank, exportPath)) {
					Toast.makeText(BankListActivity.this, getString(R.string.export_sucessfull)+" "+exportPath, Toast.LENGTH_LONG).show();
					if (item.getItemId()==R.id.bank_share) {
						Uri path = Uri.fromFile(new File(exportPath));
						Intent emailIntent = new Intent(Intent.ACTION_SEND); // set the type to 'email'
						emailIntent.setType("vnd.android.cursor.dir/email");
						String to[] = {"khizhny@gmail.com"};
						emailIntent.putExtra(Intent.EXTRA_EMAIL, to);// the attachment
						emailIntent.putExtra(Intent.EXTRA_STREAM, path);
						emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I want to share my SMS Banking settings. ("+Bank.serialVersionUID+")"); // the mail subject
						startActivity(Intent.createChooser(emailIntent , "Send email..."));
					}
				} else {
					Toast.makeText(BankListActivity.this, getString(R.string.export_failed), Toast.LENGTH_LONG).show();
				}
				return true;

			case R.id.bank_import:
                requestPermissions();
				PickFileForImport(EXPORT_FILE_EXTENSION,Environment.getExternalStorageDirectory().getPath());
				return true;

			case R.id.bank_edit: // Editing Active Bank from myBanks to sdcard
				intent= new Intent(this, BankActivity.class);
				intent.putExtra("todo", "edit");
				startActivity(intent);
				adapter.notifyDataSetChanged();
				return true;

			case R.id.bank_add: // Adding new Bank to myBanks manualy
				intent= new Intent(this, BankActivity.class);
				intent.putExtra("todo", "add");
				startActivity(intent);
				adapter.notifyDataSetChanged();
				return true;

            case R.id.bank_delete: // Deleting selected Bank from myBanks
                if (selectedBank.isActive()) {
                    db.deleteBank(selectedBank.getId());
                    db.setActiveAnyBank();
                }else{
                    db.deleteBank(selectedBank.getId());
                }
                bankList.clear();
                bankList.addAll(db.getMyBanks());

                adapter.notifyDataSetChanged();
                return true;
		}/**/
		return false;
	}

	public void PickFileForImport(String filter, String startingPath) {
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
}
