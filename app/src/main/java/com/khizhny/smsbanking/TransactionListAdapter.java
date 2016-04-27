package com.khizhny.smsbanking;

import java.math.BigDecimal;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TransactionListAdapter extends ArrayAdapter<Transaction> {

    private final Context context;
    private List<Transaction> transactions;
	private boolean hideCurrency;
	private boolean inverseRate;
	
	public TransactionListAdapter(Context context, List<Transaction> transactions, boolean hideCurrency, boolean inverseRate) {
		super(context, R.layout.activity_main_list_row, transactions);
		this.context = context;
		this.transactions = transactions;
		this.hideCurrency=hideCurrency;
		this.inverseRate=inverseRate;
	}

	@SuppressLint("DefaultLocale")
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.activity_main_list_row, parent, false);
		}
		Transaction t = transactions.get(position);
		TextView smsTextView;		

        // Filling Massage text
		smsTextView = (TextView) rowView.findViewById(R.id.smsBody);
		if (t.ruleOptionsCount()>=2){
			((TextView) rowView.findViewById(R.id.warning_sign)).setText(String.format("(%d)", t.ruleOptionsCount()));
			smsTextView.setText(t.getBody());
		}else{
			((TextView) rowView.findViewById(R.id.warning_sign)).setText("");
			smsTextView.setText(t.getBody());
		}

		// Filling Before state
        TextView accountBeforeView = (TextView) rowView.findViewById(R.id.stateBefore);
        if (t.hasStateBefore){
        	accountBeforeView.setText(t.getAccountStateBeforeAsString(hideCurrency));
        } else {
        	accountBeforeView.setText("");
        }
		// Filling Transaction Date
        TextView dateView = (TextView) rowView.findViewById(R.id.transanction_date);
        if (t.hasTransactionDate){
        	dateView.setText(t.getTransanctionDateAsString("dd.MM.yyyy"));
        }else {
        	dateView.setText("");
        }
		// Filling After state
        TextView accountAfterView = (TextView) rowView.findViewById(R.id.stateAfter);
        if (t.hasStateAfter){
	        accountAfterView.setText(t.getAccountStateAfterAsString(hideCurrency));
        }else {
        	accountAfterView.setText("");
        }
		// Filling comission
        TextView accountComissionView = (TextView) rowView.findViewById(R.id.transactionComission);
        if (t.getComission().equals(new BigDecimal("0.00"))) {
			accountComissionView.setVisibility(View.GONE);
		} else {
			accountComissionView.setVisibility(View.VISIBLE);
			accountComissionView.setText(t.getComissionAsString(hideCurrency));
			accountComissionView.setTextColor(Color.rgb(218, 48, 192)); //pink
		}
		// Filling difference
        TextView accountDifferenceView = (TextView) rowView.findViewById(R.id.stateDifference);
        if (t.hasStateDifference){
			accountDifferenceView.setVisibility(View.VISIBLE);
        	accountDifferenceView.setText(t.getAccountDifferenceAsString(hideCurrency,inverseRate));
	        switch (t.getStateDifference().signum()) {
	        	case -1:
	        		accountDifferenceView.setTextColor(Color.RED);     
	        		break;
	        	case 0:
	        		accountDifferenceView.setTextColor(Color.GRAY);
	        		break;
	        	case 1:
	            	accountDifferenceView.setTextColor(Color.rgb(0,100,0)); // dark green
	        }
        } else {
			//accountDifferenceView.setVisibility(View.GONE);
        	accountDifferenceView.setText("");
        }
        // Changing icon
        ImageView iconView = (ImageView) rowView.findViewById(R.id.transanctionIcon);
        iconView.setImageResource(t.icon);

		// Filling Extra parameters
        if (t.hasExtras()){
            ((TextView) rowView.findViewById(R.id.extra1)).setText(t.getExtraParam1());
            ((TextView) rowView.findViewById(R.id.extra2)).setText(t.getExtraParam2());
            ((TextView) rowView.findViewById(R.id.extra3)).setText(t.getExtraParam3());
            ((TextView) rowView.findViewById(R.id.extra4)).setText(t.getExtraParam4());
            rowView.findViewById(R.id.extras_ll).setVisibility(View.VISIBLE);
        } else {
            rowView.findViewById(R.id.extras_ll).setVisibility(View.GONE);
        }

        return rowView;
	}
	
}
