package com.khizhny.smsbanking;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
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

import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.Word;

import static com.khizhny.smsbanking.MyApplication.db;
import static com.khizhny.smsbanking.MyApplication.forceRefresh;

public class RuleActivity extends AppCompatActivity implements View.OnClickListener{

    private List<Button> wordButtons= new ArrayList <Button>();
	private Rule rule;
	private TextView ruleNameView;
	private ImageView imageView;
    private AlertDialog alertDialog;
    private boolean editingOldRule=false;  // if flag is set then old subrules will be deleted because regex mask is now changed.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_rule);
		Intent intent = getIntent();
        Bank bank = db.getActiveBank();
		String todo=null;
        if (intent.hasExtra("todo")) todo = intent.getExtras().getString("todo");
		if (todo!=null){
			if (todo.equals("add")){
				// adding new rule
				rule = new Rule(bank.getId(),"");
				rule.setSmsBody(intent.getExtras().getString("sms_body"));
                rule.makeInitialWordSplitting();
			} else	{
				// loading existing rule for editing.
				rule=db.getRule(intent.getExtras().getInt("rule_id"));
				if (!rule.getMask().startsWith("^")) editingOldRule=true;
			}
		}

        imageView = this.findViewById(R.id.image);

		ruleNameView =  this.findViewById(R.id.rule_name);
		ruleNameView.setText(rule.getName());

		AppCompatSpinner ruleTypeView = this.findViewById(R.id.rule_type);
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
        updateWordsLayout();
    }
    private void changeColor(View v, Word.WORD_TYPES word_type){
        int color_id;
        switch (word_type){
            case WORD_CONST:
                color_id=R.color.word_constant;
                break;
            case WORD_VARIABLE:
                color_id=R.color.word_variable;
                break;
            case WORD_VARIABLE_FIXED_SIZE:
                color_id=R.color.word_variable_fixed_size;
                break;
            default:
                color_id=R.color.word_constant;
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            v.getBackground().setColorFilter(getResources().getColor(color_id,null), PorterDuff.Mode.MULTIPLY);
        }else{
            v.getBackground().setColorFilter(getResources().getColor(color_id), PorterDuff.Mode.MULTIPLY);
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
                showDialogHelp();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            // next button click handler
            case R.id.rule_next:
                rule.updateMask();
                if (ruleNameView.getText().toString().equals("")) {
                    ruleNameView.setText(rule.getRuleNameSuggestion());
                    rule.setName(ruleNameView.getText().toString());
                }else{
                    rule.setName(ruleNameView.getText().toString());
                }

                //  If we were editing old rule then  delete all defined old subrules
                if (editingOldRule) {
                    rule.subRuleList.clear();
                    db.deleteRule(rule.getId());
                }

                // Saving or Updating Rule in DB.
                rule.setId(db.addOrEditRule(rule));

                if (rule.getRuleType()== Rule.transactionType.IGNORE) {
                    forceRefresh=true;
                    super.onBackPressed();

                } else {
                    Intent intent = new Intent(v.getContext(), TransactionActivity.class);
                    intent.putExtra("rule_id", rule.getId());
                    startActivity(intent);
                }
                break;
            default:
                break;

        }
    }

    private void showDialogHelp(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.tip_rules_1));
        alertDialog =builder.create();
        alertDialog.show();
    }

    private void showDialogToChangeWord(final Word w){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.split);
        builder.setNeutralButton(R.string.merge_left, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                w.rule.mergeLeft(w);
                dialog.dismiss();
                updateWordsLayout();
            }
        });

        builder.setPositiveButton(R.string.merge_right, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                w.rule.mergeRight(w);
                dialog.dismiss();
                updateWordsLayout();
            }
        });

        if (w.getBody().length()>=2) {
            String splitOptions[]=new String[w.getBody().length()-1];
            for (int i=1;i<=w.getBody().length()-1;i++){
                splitOptions[i-1]=w.getBody().substring(0,i)+" >< "+w.getBody().substring(i);
            }

            builder.setSingleChoiceItems(splitOptions, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    w.rule.split(w,which+1);
                    dialog.dismiss();
                    updateWordsLayout();
                }
            });
        }
        alertDialog=builder.create();
        alertDialog.show();
    }

    private void updateWordsLayout(){
        FlowLayout flowLayout = this.findViewById(R.id.rule1_flow_layout);
        // Deleting all buttons if any existed
        for (int i=wordButtons.size()-1; i>=0; i--){
            flowLayout.removeView(wordButtons.get(i));
        }
        //Creating "word buttons" on Flow Layout
        wordButtons = new ArrayList <Button>();/**/
        Button wordButton;
        wordButton = new Button(this);
        wordButton.setText(R.string.begin);
        changeColor(wordButton,Word.WORD_TYPES.WORD_CONST);
        wordButton.setMinHeight(0);
        wordButton.setMinWidth(0);
        wordButton.setMinimumHeight(0);
        wordButton.setMinimumWidth(0);
        //wordButton.setPadding(16,8,16,8);
        wordButtons.add(wordButton);
        Word word;

        if (flowLayout != null) {
            flowLayout.addView(wordButton);
            for (int i=1;i<=rule.words.size();i++){
                wordButton=new Button(this);
                word=rule.words.get(i-1);
                wordButton.setText(String.format("\"%s\"", word.getBody()));
                changeColor(wordButton,word.getWordType());
                wordButton.setMinHeight(0);
                wordButton.setMinWidth(0);
                wordButton.setMinimumHeight(0);
                wordButton.setMinimumWidth(0);
                //wordButton.setPadding(8,8,8,8);
                wordButton.setTag(rule.words.get(i-1));
                wordButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Word word = (Word) v.getTag();
                        word.changeWordType();
                        changeColor(v,word.getWordType());
                    }
                });
                wordButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showDialogToChangeWord((Word) v.getTag());
                        return false;
                    }
                });
                wordButtons.add(wordButton);
                flowLayout.addView(wordButton);
            }

            wordButton=new Button(this);
            wordButton.setText(R.string.end);
            changeColor(wordButton,Word.WORD_TYPES.WORD_CONST);
            wordButton.setMinHeight(0);
            wordButton.setMinWidth(0);
            wordButton.setMinimumHeight(0);
            wordButton.setMinimumWidth(0);
            //wordButton.setPadding(16,8,16,8);
            wordButtons.add(wordButton);
            flowLayout.addView(wordButton);
        }
    }
}