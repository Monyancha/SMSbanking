package com.khizhny.smsbanking;

import android.app.AlertDialog;
import android.content.Intent;
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

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;

public class TransactionActivity extends AppCompatActivity implements View.OnClickListener {
    private Rule rule;
    private Transaction transaction;
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
        db.addOrEditRule(rule);
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
        Log.d(LOG, "TransactionActivity resuming");

        // getting Rule ID from Intent
        Intent intent = getIntent();
        int ruleId = intent.getExtras().getInt("rule_id");
        Log.d(MyApplication.LOG, "Getting Rule from db.");//

        // loading Rule and Bank objects
        rule = db.getRule(ruleId);

        transaction = rule.getSampleTransaction(this);

        TextView smsTextView = (TextView) findViewById(R.id.sms_body);
        String smsText = getString(R.string.begin) + rule.getSmsBody() + getString(R.string.end);
        if (smsTextView != null) {
            smsTextView.setText(smsText);
        }

        TextView stateAfterView = (TextView) findViewById(R.id.state_after_value);
        stateAfterView.setText(transaction.getStateAfterAsString(false));

        TextView stateBeforeView = (TextView) findViewById(R.id.state_before_value);
        stateBeforeView.setText(transaction.getStateBeforeAsString(false));

        TextView stateChangeView = (TextView) findViewById(R.id.state_change_value);
        stateChangeView.setText(transaction.getDifferenceAsString(false, false,false));

        TextView commissionView = (TextView) findViewById(R.id.commision_value);
        commissionView.setText(transaction.getCommissionAsString(false, false));

        TextView currencyView = (TextView) findViewById(R.id.currency_value);
        currencyView.setText(transaction.getTransactionCurrency());

        TextView extra1View = (TextView) findViewById(R.id.extra1_value);
        extra1View.setText(transaction.getExtraParam1());

        TextView extra2View = (TextView) findViewById(R.id.extra2_value);
        extra2View.setText(transaction.getExtraParam2());

        TextView extra3View = (TextView) findViewById(R.id.extra3_value);
        extra3View.setText(transaction.getExtraParam3());

        TextView extra4View = (TextView) findViewById(R.id.extra4_value);
        extra4View.setText(transaction.getExtraParam4());

        Button doneButton = (Button) findViewById(R.id.finish_rule);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // closing activity and going back to MainActivity
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
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