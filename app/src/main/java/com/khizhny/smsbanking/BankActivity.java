package com.khizhny.smsbanking;

import java.util.ArrayList;
import java.util.List;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BankActivity extends AppCompatActivity {
	private Bank bank;
	private TextView nameView;
	private AppCompatSpinner currencyView;
	private EditText phonesView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bank);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// getting available Views
		nameView = (TextView) findViewById(R.id.name);
		phonesView = (EditText) findViewById(R.id.bank_phones);
		currencyView = (AppCompatSpinner) findViewById(R.id.currency);
		Button saveView = (Button) findViewById(R.id.sub_rule_save);
		ImageButton addPhone = (ImageButton) findViewById(R.id.add_phone_button);
		ImageButton clearPhones = (ImageButton) findViewById(R.id.clear_phones);

		String todo = getIntent().getExtras().getString("todo");
		if (todo != null) {
			if (todo.equals("edit")) { // Form is used for editing active bank info. Filling bank info from DB
                DatabaseAccess db = DatabaseAccess.getInstance(this);
                db.open();
				bank=db.getActiveBank();
                db.close();
				nameView.setText(bank.getName());
				phonesView.setText(bank.getPhone());
				currencyView.setSelection(getIndex(currencyView, bank.getDefaultCurrency()));
			}else{  // Form is used for adding new bank info.
				bank=new Bank();
			}
		}

		// getting phone list from messages
		final AppCompatSpinner phoneListView = (AppCompatSpinner) findViewById(R.id.phone_list);

        Cursor c=null;
        if (MyApplication.hasReadSmsPermission) {
            Uri uri = Uri.parse("content://sms/inbox");
            c = getContentResolver().query(uri, new String[]{"Distinct address"}, null, null, null);
        } else {
            Toast.makeText(BankActivity.this, "Read SMS permission denied.", Toast.LENGTH_SHORT).show();
        }
        int msgCount;
		List <String> list;  // list for storing all available message senders for dropdown list
		list= new ArrayList<String>();
		list.clear();
		if (c != null) {
			msgCount=c.getCount();
			if(c.moveToFirst()) {
				for(int ii=0; ii < msgCount; ii++) {
					list.add(c.getString(c.getColumnIndexOrThrow("address")));
					c.moveToNext();
				}
			}
			c.close();
		}

        if (phoneListView != null) {
            phoneListView.setAdapter(new ArrayAdapter<String>(BankActivity.this, android.R.layout.simple_dropdown_item_1line, list));
        }

        if (saveView != null) {
            saveView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (phonesView.getText().toString().length() < 3) {
                        Toast.makeText(BankActivity.this, getString(R.string.phone_number_not_set), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BankActivity.this, getString(R.string.saving_changes), Toast.LENGTH_SHORT).show();
                        bank.setName(nameView.getText().toString());
                        bank.setPhone(phonesView.getText().toString());
                        bank.setDefaultCurrency(currencyView.getSelectedItem().toString().replace("\n", ""));
                        DatabaseAccess db = DatabaseAccess.getInstance(BankActivity.this);
                        db.open();
                        db.addOrEditBank(bank);
                        db.close();
                        BankActivity.this.finish();
                    }
                }
            });
        }

        if (addPhone != null) {
            addPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String s=phonesView.getText().toString();
                    if (!s.equals("")) s=s+";";
                    if (phoneListView != null) {
                        phonesView.setText(String.format("%s%s", s, phoneListView.getSelectedItem().toString()));
                    }
                    if (phoneListView != null) {
                        phoneListView.setSelection(0);
                    }
                }
            });
        }

        if (clearPhones != null) {
            clearPhones.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // if clear button clicked we will delete last added phone number.
                    String[] s=phonesView.getText().toString().split(";");
                    String s2="";
                    for (int i=0;i<s.length-1;i++) {
                        if (i==0) {
                            s2=s[0];
                        } else {
                            s2=s2+";"+s[i];
                        }

                    }
                    phonesView.setText(s2);
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
}