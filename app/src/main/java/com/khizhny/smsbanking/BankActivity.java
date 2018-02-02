package com.khizhny.smsbanking;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.khizhny.smsbanking.model.Bank;

import static com.khizhny.smsbanking.MyApplication.db;

public class BankActivity extends AppCompatActivity {
	private Bank bank;
	private TextView nameView;
    private AppCompatSpinner currencyView;
    private AppCompatSpinner countryView;
    private EditText phonesView;
    private String[] senders;  // list for storing all available message senders for dropdown list

    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bank);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// getting available Views
		nameView =  findViewById(R.id.name);
		phonesView =  findViewById(R.id.bank_phones);
        currencyView =  findViewById(R.id.currency);
        countryView = findViewById(R.id.country);
		Button saveView =  findViewById(R.id.sub_rule_save);
		ImageButton addPhone =  findViewById(R.id.add_phone_button);
		ImageButton clearPhones = findViewById(R.id.clear_phones);

        final String arr[]=getResources().getStringArray(R.array.countries_array);
        String country=getCountry();
        for (int i=0; i<arr.length; i++){
            if (arr[i].equals(country)) countryView.setSelection(i);
        }

		String todo = getIntent().getExtras().getString("todo");
		if (todo != null) {
			if (todo.equals("edit")) { // Form is used for editing active bank info. Filling bank info from DB
				bank=db.getActiveBank();
				nameView.setText(bank.getName());
				phonesView.setText(bank.getPhone());
				currencyView.setSelection(getIndex(currencyView, bank.getDefaultCurrency()));
			}else{  // Form is used for adding new bank info.
				bank=new Bank();
			}
		}

		// getting sms senders list if granted
        Cursor c=null;
        if (MyApplication.hasReadSmsPermission) {
            Uri uri = Uri.parse("content://sms/inbox");
            c = getContentResolver().query(uri, new String[]{"Distinct address"}, null, null, "address desc");
        } else {
            Toast.makeText(BankActivity.this, "Read SMS permission denied.", Toast.LENGTH_SHORT).show();
        }

		if (c != null) {
            int sendersCount = c.getCount();
            senders = new String[sendersCount];
			if(c.moveToFirst()) {
				for(int ii = 0; ii < sendersCount; ii++) {
                    senders[ii]=c.getString(c.getColumnIndexOrThrow("address"));
					c.moveToNext();
				}
			}
			c.close();
		}

        if (saveView != null) {
            saveView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (phonesView.getText().toString().length() < 3) {
                        Toast.makeText(BankActivity.this, getString(R.string.phone_number_not_set), Toast.LENGTH_SHORT).show();
                    } else if (nameView.getText().toString().equals("")) {
                        Toast.makeText(BankActivity.this, getString(R.string.bank_name_must_be_set), Toast.LENGTH_SHORT).show();
                    }else {
                        // Updating country settings in preferences
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String country=countryView.getSelectedItem().toString();
                        settings.edit().putString("country_preference",country).apply();

                        Toast.makeText(BankActivity.this, getString(R.string.saving_changes), Toast.LENGTH_SHORT).show();
                        bank.setName(nameView.getText().toString());
                        bank.setPhone(phonesView.getText().toString());
                        bank.setDefaultCurrency(currencyView.getSelectedItem().toString().replace("\n", ""));
                        bank.setCountry(arr[countryView.getSelectedItemPosition()]);
                        db.addOrEditBank(bank,false,false);
                        BankActivity.this.finish();
                    }
                }
            });
        }

        if (addPhone != null) {
            addPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(BankActivity.this);
                    builder.setTitle(R.string.pick_phone_number);
                    builder.setItems(senders, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String s = phonesView.getText().toString();
                                    if (!s.equals("")) s=s+";";
                                    phonesView.setText(String.format("%s%s", s, senders[which]));
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });
        }

        if (clearPhones != null) {
            clearPhones.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // if clear button clicked we will delete last added phone number.
                    String[] s=phonesView.getText().toString().split(";");
                    StringBuilder s2= new StringBuilder();
                    for (int i=0;i<s.length-1;i++) {
                        if (i==0) {
                            s2 = new StringBuilder(s[0]);
                        } else {
                            s2.append(";").append(s[i]);
                        }

                    }
                    phonesView.setText(s2.toString());
                }
            });
        }
    }

	/**
	 * Functon gets the index of myString in spinner view.
	 * @param spinner spinner view
	 * @param myString string to be searched in spinner view
	 * @return index of myString in spinner view.
	 */
	private int getIndex(AppCompatSpinner spinner, String myString)
	{
		int index = 0;
		for (int i=0;i<spinner.getCount();i++){
			if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
				index = i;
				break;
			}
		}
		return index;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId() ){
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private String getCountry(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return settings.getString("country_preference",null);
    }
}