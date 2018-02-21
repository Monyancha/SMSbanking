package com.khizhny.smsbanking;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;
import com.khizhny.smsbanking.model.Transaction;

import static com.khizhny.smsbanking.MainActivity.KEY_RULE_ID;
import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;
import static com.khizhny.smsbanking.MyApplication.getExtraParameterNames;

public class TransactionActivity extends AppCompatActivity implements View.OnClickListener {



    private Rule rule;
    private Transaction transaction; // sample transaction with sample message
    private AlertDialog alertDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Saving all then subrules in our list.
        db.addOrEditRule(rule,true);
    }

    @Override
    protected void onStop() {
        if (alertDialog != null) {
            if (alertDialog.isShowing()) alertDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // getting Rule object in on resume because we will get back here from Subrule Activity
        Intent intent = getIntent();
        if ( intent.hasExtra( KEY_RULE_ID)) {
            int ruleId;
            Bundle b=intent.getExtras();
            if (b!=null) {
                ruleId = b.getInt(KEY_RULE_ID);
                Log.d(MyApplication.LOG, "Getting Rule from db.");//
                // loading Rule and BankV2 objects
                rule = db.getRule(ruleId);
                transaction = rule.getSampleTransaction();
            }else{
                Log.d(MyApplication.LOG, "No Rule Id passed with intent to transaction Activity.");
            }

        }
        if (rule==null) Log.e(MyApplication.LOG, "Rule for Transaction Activity not found.");

        Log.d(LOG, "Transaction Activity resuming");
				refreshUiElements();
    }

    private void refreshUiElements(){
    		if (rule.isAdvanced()) {
    				findViewById(R.id.impersonalize).setVisibility(View.GONE);
				}else{
						findViewById(R.id.impersonalize).setVisibility(View.VISIBLE);
				}
				TextView smsTextView = findViewById(R.id.sms_body);
				TextView stateAfterView = findViewById(R.id.state_after_value);
				TextView stateBeforeView = findViewById(R.id.state_before_value);
				TextView stateChangeView = findViewById(R.id.state_change_value);
				TextView commissionView =  findViewById(R.id.commision_value);
				TextView currencyView =  findViewById(R.id.currency_value);
				TextView extra1View = findViewById(R.id.extra1_value);
				TextView extra2View =  findViewById(R.id.extra2_value);
				TextView extra3View =  findViewById(R.id.extra3_value);
				TextView extra4View =  findViewById(R.id.extra4_value);

				String extranames[]= getExtraParameterNames(this);
				if (!extranames[0].equals("")) ((Button)findViewById(R.id.extra1_label)).setText(extranames[0]);
				if (!extranames[1].equals("")) ((Button)findViewById(R.id.extra2_label)).setText(extranames[1]);
				if (!extranames[2].equals("")) ((Button)findViewById(R.id.extra3_label)).setText(extranames[2]);
				if (!extranames[3].equals("")) ((Button)findViewById(R.id.extra4_label)).setText(extranames[3]);


				if (smsTextView != null) smsTextView.setText(String.format("%s%s%s", getString(R.string.begin), rule.getSmsBody(), getString(R.string.end)));
				stateAfterView.setText(transaction.getStateAfterAsString(false));
				stateBeforeView.setText(transaction.getStateBeforeAsString(false));
				stateChangeView.setText(transaction.getDifferenceAsString(false, false,false));
				commissionView.setText(transaction.getCommissionAsString(false, false));
				currencyView.setText(transaction.getTransactionCurrency());
				extra1View.setText(transaction.getExtraParam1());
				extra2View.setText(transaction.getExtraParam2());
				extra3View.setText(transaction.getExtraParam3());
				extra4View.setText(transaction.getExtraParam4());


				stateAfterView.setTextColor(transaction.hasCalculatedAccountStateAfter?Color.BLACK:Color.BLUE);
				stateBeforeView.setTextColor(transaction.hasCalculatedAccountStateBefore?Color.BLACK:Color.BLUE);
				stateChangeView.setTextColor(transaction.hasCalculatedAccountDifference?Color.BLACK:Color.BLUE);
				currencyView.setTextColor(!transaction.hasTransactionCurrency?Color.BLACK:Color.BLUE);
				extra1View.setTextColor(Color.BLUE);
				extra2View.setTextColor(Color.BLUE);
				extra3View.setTextColor(Color.BLUE);
				extra4View.setTextColor(Color.BLUE);
				commissionView.setTextColor(transaction.getCommission().signum()==0?Color.BLACK:Color.BLUE);
		}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_transaction_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_help:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.tip_subrules_1));
                alertDialog = builder.create();
                alertDialog.show();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Transaction.Parameters selectedParameter;
        switch (v.getId()) {
            case R.id.state_after_label:
                selectedParameter = Transaction.Parameters.ACCOUNT_STATE_AFTER;
                break;
            case R.id.state_change_label:
                selectedParameter = Transaction.Parameters.ACCOUNT_DIFFERENCE;
                break;
            case R.id.currency_label:
                selectedParameter = Transaction.Parameters.CURRENCY;
                break;
            case R.id.commision_label:
                selectedParameter = Transaction.Parameters.COMMISSION;
                break;
            case R.id.state_before_label:
                selectedParameter = Transaction.Parameters.ACCOUNT_STATE_BEFORE;
                break;
            case R.id.extra1_label:
                selectedParameter = Transaction.Parameters.EXTRA_1;
                break;
            case R.id.extra2_label:
                selectedParameter = Transaction.Parameters.EXTRA_2;
                break;
            case R.id.extra3_label:
                selectedParameter = Transaction.Parameters.EXTRA_3;
                break;
            case R.id.extra4_label:
                selectedParameter = Transaction.Parameters.EXTRA_4;
                break;
						case R.id.impersonalize:
								rule.impersonalize();
								transaction = rule.getSampleTransaction();
								refreshUiElements();
								return;
            case R.id.finish_rule:
                // closing activity and going back to MainActivity
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return;
            default:
                Log.e(LOG, "Unexpected parameter found :(");
                return;
        }

        SubRule subRule = rule.getOrCreateSubRule(selectedParameter);
        // if no subrule already defined adding it to db
        if (subRule.getId() == -1) {
           db.addOrEditSubRule(subRule);
        }
        Intent intent = new Intent(this, SubRuleActivity.class);
        intent.putExtra("subRuleId", subRule.getId());
        intent.putExtra("ruleId", rule.getId());
        intent.putExtra("caption", ((Button)v).getText().toString());
        startActivity(intent);
    }

}