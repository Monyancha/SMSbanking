package com.khizhny.smsbanking;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class SubRuleListActivity extends AppCompatActivity {
	private List<SubRule> subRuleList;
	private SubRuleListAdapter adapter;
	private Rule rule;
	protected static List<String> phrases;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sub_rule);

	}
	@Override
	protected void onPause() {
		super.onPause();
		// Saving all then subrules in our list.
        int subRulesCount = adapter.getCount();
        DatabaseAccess db = DatabaseAccess.getInstance(this);
        db.open();
		for (int i=0; i<subRulesCount;i++){
            db.addOrEditSubRule(adapter.getItem(i));
		}
        db.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG,"SubRuleListActivity resuming");
		// getting Rule ID from Intent
		Intent intent = getIntent();
		int ruleId = intent.getExtras().getInt("rule_id");

		Log.d(MyApplication.LOG,"Getting subRules from db to array list");//
		subRuleList=new ArrayList<SubRule>();
        DatabaseAccess db = DatabaseAccess.getInstance(this);
        db.open();
        rule=db.getRule(ruleId);
		subRuleList=db.getSubRules(ruleId);
        db.close();
		//
		phrases=rule.getConstantPhrases();

		Log.d(LOG,"inflating list view with subrules");
		adapter  = new SubRuleListAdapter(this, subRuleList);
		adapter.rule=rule;
		ListView listView = (ListView) findViewById(R.id.sub_rule_list);
        if (listView != null) {
            listView.setAdapter(adapter);
        }

        TextView smsTextView=(TextView) findViewById(R.id.sms_text);
		String smsText=getString(R.string.begin)+rule.getSmsBody()+getString(R.string.end);
        if (smsTextView != null) {
            smsTextView.setText(smsText);
        }

        Button addView = (Button)  findViewById(R.id.sub_rule_add);
        if (addView != null) {
            addView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // addin new SubRule to the list and db.
                    DatabaseAccess db = DatabaseAccess.getInstance(v.getContext());
                    db.open();
                    SubRule sr = new SubRule(rule.getId());
                    sr.setId(db.addOrEditSubRule(sr));
                    subRuleList.add(sr);
                    db.close();
                    adapter.notifyDataSetChanged();
                }
            });
        }
        Button helpView = (Button)  findViewById(R.id.sub_rule_show_tip);
        if (helpView != null) {
            helpView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(SubRuleListActivity.this,Tip.class);
                    intent.putExtra("tip_res_id", R.string.tip_subrules_1);
                    startActivity(intent);
                }
            });
        }
    }

}
