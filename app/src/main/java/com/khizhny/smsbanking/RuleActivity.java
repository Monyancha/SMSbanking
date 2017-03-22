package com.khizhny.smsbanking;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.TextView;
import android.widget.Toast;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class RuleActivity extends AppCompatActivity {
	private List<Button> wordButtons;
	private Rule rule;
	private TextView ruleNameView;
	private ImageView imageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(LOG,"RuleActivity starting...");
        setContentView(R.layout.activity_rule);
		FlowLayout myLayout = (FlowLayout) this.findViewById(R.id.rule1_flow_layout);
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
		Button W;

		W = new Button(this);
		W.setText(R.string.begin);
		W.setBackgroundColor(Color.GRAY);
		// RuleActivity.setBackgroundColor(W,Color.GRAY);
		wordButtons.add(W);
        if (myLayout != null) {
            myLayout.addView(W);
        }

        for (int i=1;i<=words.length;i++){
			W=new Button(this);
			W.setText(words[i-1]);
			W.setBackgroundColor(Color.LTGRAY);
			//RuleActivity.setBackgroundColor(W,Color.LTGRAY);
			W.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					ColorDrawable buttonColor = (ColorDrawable) v.getBackground();
					int wordIndex = wordButtons.indexOf(v);
                    int colorInt=0;
					// cheching the color of the word colorInt
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						colorInt=buttonColor.getColor();
					}else {
						try {
							Field field = buttonColor.getClass().getDeclaredField("mState");
							field.setAccessible(true);
							Object object = field.get(buttonColor);
							field = object.getClass().getDeclaredField("mUseColor");
							field.setAccessible(true);
							colorInt=field.getInt(object);
						} catch (NoSuchFieldException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					if (colorInt == Color.GRAY) {
						v.setBackgroundColor(Color.LTGRAY);
						rule.deSelectWord(wordIndex);
					}
					else
					{
						v.setBackgroundColor(Color.GRAY);
						rule.selectWord(wordIndex);
					}
				}
			});
			wordButtons.add(W);
            if (myLayout != null) {
                myLayout.addView(W);
            }
        }

		W=new Button(this);
		W.setText(R.string.end);
		W.setBackgroundColor(Color.GRAY);
		wordButtons.add(W);
        if (myLayout != null) {
            myLayout.addView(W);
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
                    finish();
                    Toast.makeText(v.getContext(), "New rule saved.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(v.getContext(), SubRuleListActivity.class);
                    intent.putExtra("rule_id", rule.getId());
                    startActivity(intent);
                }
            });
        }
    }

}