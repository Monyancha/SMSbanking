package com.khizhny.smsbanking;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;

public class SubRuleActivity extends AppCompatActivity implements View.OnClickListener
{

    private Rule rule;       // Parent rule
    private SubRule subRule; // Sub rule which is editing in activity


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_rule);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);

        // getting sub rule ID from intent
        int subRuleId = getIntent().getIntExtra("subRuleId",-1);
        int ruleId = getIntent().getIntExtra("ruleId",-1);
        setTitle(getIntent().getStringExtra("caption"));

        if (ruleId>=0) {
            rule=db.getRule(ruleId);
            for (SubRule sr: rule.subRuleList) {
                if (sr.getId()==subRuleId) {
                    subRule=sr;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView bodyView = findViewById(R.id.sms_body);
        bodyView.setText(rule.getSmsBody());

        //==  Method ====
        AppCompatSpinner methodView =  findViewById(R.id.sub_rule_method);
        switch (subRule.getExtractionMethod()){
            case WORD_AFTER_PHRASE:
            case WORD_BEFORE_PHRASE:
            case WORDS_BETWEEN_PHRASES:
                Log.e(LOG,"SubRuleActivity is trying to edit old subrule!");
                break;
            case USE_CONSTANT:
                methodView.setSelection(0);
                break;
            case USE_REGEX:
                methodView.setSelection(1);
                break;
        }

        methodView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            // method Spinner change listener
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                switch (selectedPosition){
                    case 0:  subRule.setExtractionMethod(SubRule.Method.USE_CONSTANT); break;
                    case 1:  subRule.setExtractionMethod(SubRule.Method.USE_REGEX); break;
                }
                refreshResult();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //==  Phrase ====
        AppCompatSpinner phraseView =findViewById(R.id.sub_rule_phrase);
        ArrayAdapter phraseAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, rule.getVariablePhrases());
       phraseView.setAdapter(phraseAdapter);
        phraseView.setSelection(subRule.regexPhraseIndex);
        phraseView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View v, int selectedPosition, long id) {
                if (subRule.regexPhraseIndex!=selectedPosition){
                    subRule.regexPhraseIndex=selectedPosition;
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //=Constant===========================================================================
        EditText constantView = findViewById(R.id.sub_rule_constant_value);
        constantView.setText(subRule.getConstantValue());
        constantView.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        //KeyEvent: If triggered by an enter key, this is the event; otherwise, this is null.
                        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                            return false;
                        } else if (actionId == EditorInfo.IME_ACTION_SEARCH
                                || event == null
                                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            // the user is done typing.
                            subRule.setConstantValue(v.getText().toString());
                            refreshResult();
                            return true;
                        }
                        return false; // pass on to other listeners.
                    }
                });/**/
        //========================================================================================
        CheckBox negateView = findViewById(R.id.sub_rule_negate);
        negateView.setChecked(subRule.negate);
        negateView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                // Adding Negate checkbox listener
                if (isChecked != subRule.negate) {
                    subRule.negate=isChecked;
                    refreshResult();
                }
            }
        });

        //========================================================================================
        AppCompatSpinner separatorView = findViewById(R.id.sub_rule_separator);
        separatorView.setSelection(subRule.getDecimalSeparator().ordinal());
        separatorView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.getDecimalSeparator().ordinal() != selectedPosition) {
                    subRule.setDecimalSeparator(SubRule.DECIMAL_SEPARATOR.values()[selectedPosition]);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner ignoreNLeftView =  findViewById(R.id.sub_rule_ignore_n_first);
        ignoreNLeftView.setSelection(subRule.trimLeft);
        ignoreNLeftView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.trimLeft != selectedPosition) {
                    subRule.trimLeft=selectedPosition;
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner ignoreNRightView = findViewById(R.id.sub_rule_ignore_n_last);
        ignoreNRightView.setSelection(subRule.trimRight);
        ignoreNRightView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.trimRight != selectedPosition) {
                    subRule.trimRight=selectedPosition;
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        refreshResult();
    }

    private void refreshResult(){
        TextView resultView = findViewById(R.id.sub_rule_result_value);
        TextView tipView =  findViewById(R.id.subrule_tip);
        tipView.setText("");
        // hiding unused views depending on used method
        switch (subRule.getExtractionMethod()){
            case WORD_AFTER_PHRASE:
            case WORD_BEFORE_PHRASE:
            case WORDS_BETWEEN_PHRASES:
                break;
            case USE_CONSTANT:
                findViewById(R.id.sub_rule_phrase).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_phrase_label).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.VISIBLE);
                break;
            case USE_REGEX:
                findViewById(R.id.sub_rule_phrase).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_phrase_label).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.GONE);
                break;
        }

        Log.d(LOG,"refreshResult is called");
        switch (subRule.getExtractedParameter()){
            // getting result as a currency
            case CURRENCY:
                resultView.setText(subRule.applySubRule(rule.getSmsBody(), 1));
                break;
            // getting result as a decimal value
            case COMMISSION:
                tipView.setText(R.string.commission_tip);
            case ACCOUNT_STATE_BEFORE:
            case ACCOUNT_STATE_AFTER:
            case ACCOUNT_DIFFERENCE:
                resultView.setText(subRule.applySubRule(rule.getSmsBody(), 0));
                break;
            // getting result as a string
            case EXTRA_1:
            case EXTRA_2:
            case EXTRA_3:
            case EXTRA_4:
                resultView.setText(subRule.applySubRule(rule.getSmsBody(), 2));
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.subrule_delete:
                // Deleting subrule and going back in stack
                db.deleteSubRule(subRule.getId());
                finish();
                break;
            case R.id.subrule_finish_button:
                onBackPressed();
            default:
                Log.e(LOG, "Unsupported view!");
        }
    }

    @Override
    public void onBackPressed() {
        //Saving sub rule changes to db and going back in stack
        db.addOrEditSubRule(subRule);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Saving sub rule changes to db and going back in stack
                db.addOrEditSubRule(subRule);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
