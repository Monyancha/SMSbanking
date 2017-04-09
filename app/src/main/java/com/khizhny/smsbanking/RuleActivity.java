package com.khizhny.smsbanking;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.TextView;

public class RuleActivity extends AppCompatActivity {
	private List<Button> wordButtons;
	private Rule rule;
	private TextView ruleNameView;
	private ImageView imageView;
    private AlertDialog alertDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_rule);
		Intent intent = getIntent();
        DatabaseAccess db = DatabaseAccess.getInstance(this);
        db.open();
        Bank bank = db.getActiveBank();
		String todo = intent.getExtras().getString("todo");
		if (todo!=null){
			if (todo.equals("add")){
				// adding new rule
				rule = new Rule(bank.getId(),"New rule");
				rule.setSmsBody(intent.getExtras().getString("sms_body"));
			} else	{
				// loading existing rule for editing.
				rule=db.getRule(intent.getExtras().getInt("rule_id"));
			}
		}
        db.close();

		imageView = (ImageView) this.findViewById(R.id.image);

		ruleNameView =  (TextView) this.findViewById(R.id.rule_name);
		ruleNameView.setText(rule.getName());

		AppCompatSpinner ruleTypeView = (AppCompatSpinner) this.findViewById(R.id.rule_type);
        if (ruleTypeView != null) {
            ruleTypeView.setSelection(rule.getRuleTypeInt());
        }
        if (ruleTypeView != null) {
            ruleTypeView.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    imageView.setImageResource(Rule.ruleTypeIcons[position]);
                    rule.setRuleType(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    imageView.setImageResource(Rule.ruleTypeIcons[0]);
                    rule.setRuleType(0);/**/
                }
            });
        }


        imageView.setImageResource(rule.getRuleTypeDrawable());

		// Creating "word buttons" on Flow Layout
		String[] words = rule.getSmsBody().split(" ");
		wordButtons = new ArrayList <Button>();
		Button wordButton;

		wordButton = new Button(this);
		wordButton.setText(R.string.begin);
		wordButton.setBackgroundColor(Color.GRAY);
        wordButton.setMinHeight(0);
        wordButton.setMinWidth(0);
        wordButton.setMinimumHeight(0);
        wordButton.setMinimumWidth(0);
        wordButton.setPadding(16,8,16,8);
		// RuleActivity.setBackgroundColor(wordButton,Color.GRAY);
		wordButtons.add(wordButton);

        FlowLayout flowLayout = (FlowLayout) this.findViewById(R.id.rule1_flow_layout);
        if (flowLayout != null) {
            flowLayout.addView(wordButton);
        }

        for (int i=1;i<=words.length;i++){
			wordButton=new Button(this);
			wordButton.setText(words[i-1]);
			wordButton.setBackgroundColor(Color.LTGRAY);
            wordButton.setMinHeight(0);
            wordButton.setMinWidth(0);
            wordButton.setMinimumHeight(0);
            wordButton.setMinimumWidth(0);
            wordButton.setPadding(16,8,16,8);
            //RuleActivity.setBackgroundColor(wordButton,Color.LTGRAY);
			wordButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ColorDrawable buttonColor = (ColorDrawable) v.getBackground();
					int wordIndex = wordButtons.indexOf(v);

					// cheching the color of the word colorInt
					if (buttonColor.getColor() == Color.GRAY) {
						v.setBackgroundColor(Color.LTGRAY);
						rule.deSelectWord(wordIndex);
					}
					else
					{
						v.setBackgroundColor(Color.GRAY);
						rule.selectWord(wordIndex);
					}
                    // refreshing rule name
                    ruleNameView.setText(rule.getRuleNameSuggestion());
				}
			});
			wordButtons.add(wordButton);
            if (flowLayout != null) {
                flowLayout.addView(wordButton);
            }
        }

		wordButton=new Button(this);
		wordButton.setText(R.string.end);
		wordButton.setBackgroundColor(Color.GRAY);
        wordButton.setMinHeight(0);
        wordButton.setMinWidth(0);
        wordButton.setMinimumHeight(0);
        wordButton.setMinimumWidth(0);
        wordButton.setPadding(16,8,16,8);
		wordButtons.add(wordButton);
        if (flowLayout != null) {
            flowLayout.addView(wordButton);
        }

        // Making "word buttons" colored acording to selected words parameter of the rule
		for (int i=1; i<=rule.wordsCount;i++){
			if (rule.wordIsSelected[i]){
				wordButtons.get(i).setBackgroundColor(Color.GRAY);
				//RuleActivity.setBackgroundColor(wordButtons.get(i),Color.GRAY);
			}
		}

		// Adding Next button click handler
		Button nextBtn = (Button) this.findViewById(R.id.rule_next);
        if (nextBtn != null) {
            nextBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    rule.setName(ruleNameView.getText().toString());
                    // Saving or Updating Rule in DB.
                    DatabaseAccess db = DatabaseAccess.getInstance(v.getContext());
                    db.open();
                    rule.setId(db.addOrEditRule(rule));
                    db.close();
                    //finish();
                    //Toast.makeText(v.getContext(), "New rule saved.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(v.getContext(), TransactionActivity.class);
                    intent.putExtra("rule_id", rule.getId());
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rule_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.item_help:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.tip_rules_1));
                alertDialog =builder.create();
                alertDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onStop() {
        if (alertDialog!=null) {
            if (alertDialog.isShowing()) alertDialog.dismiss();
        }
        super.onStop();
    }
}