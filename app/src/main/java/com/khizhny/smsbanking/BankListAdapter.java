package com.khizhny.smsbanking;

import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

public class BankListAdapter extends ArrayAdapter<Bank> {

    private final Context context;
    private final List<Bank> bankList;
    
	
	public BankListAdapter(Context context, List<Bank> bankList) {
		super(context, R.layout.activity_bank_list_row, bankList);
		this.context = context;
		this.bankList = bankList;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = vi.inflate(R.layout.activity_bank_list_row, parent, false);
		}

		TextView bankNameView = (TextView) rowView.findViewById(R.id.bankName);
        bankNameView.setText(bankList.get(position).getName());
        
        TextView bankPhoneView = (TextView) rowView.findViewById(R.id.bankPhone);
        bankPhoneView.setText(bankList.get(position).getPhone());

		TextView bankCurrencyView = (TextView) rowView.findViewById(R.id.bankCurrency);
		bankCurrencyView.setText(bankList.get(position).getDefaultCurrency());

		TextView bankValueView = (TextView) rowView.findViewById(R.id.bankValue);
		bankValueView.setText(bankList.get(position).getCurrentAccountState());

        RadioButton RadioButtonView = (RadioButton) rowView.findViewById(R.id.active);
        RadioButtonView.setChecked(bankList.get(position).isActive());
        RadioButtonView.setFocusable(false);
        RadioButtonView.setClickable(false);
        RadioButtonView.setTag(position);
        return rowView;
	}
	
}
