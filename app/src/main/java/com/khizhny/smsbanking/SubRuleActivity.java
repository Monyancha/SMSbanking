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

import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class SubRuleActivity extends AppCompatActivity implements View.OnClickListener
{

    private Rule rule;
    private SubRule subRule; // Subrule thich is editing in activity
    private List<String> phrases;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_rule);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);

        // getting subrule ID from intent
        int subRuleId = getIntent().getIntExtra("subRuleId",-1);
        int ruleId = getIntent().getIntExtra("ruleId",-1);
        setTitle(getIntent().getStringExtra("caption"));
        if (subRuleId>=0) {
            DatabaseAccess db = DatabaseAccess.getInstance(getApplicationContext());
            db.open();
            rule=db.getRule(ruleId);
            db.close();
            for (SubRule sr: rule.subRuleList) {
                if (sr.getId()==subRuleId) {
                    subRule=sr;
                }
            }
            phrases = rule.getConstantPhrases();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        //========================================================================================
        TextView bodyView = (TextView) findViewById(R.id.sms_body);
        bodyView.setText(rule.getSmsBody());

        //========================================================================================
        AppCompatSpinner methodView = (AppCompatSpinner) findViewById(R.id.sub_rule_method);
        methodView.setSelection(subRule.getExtractionMethodInt());
        methodView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            // method Spinner change listener
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.getExtractionMethodInt() != selectedPosition) {
                    subRule.setExtractionMethod(selectedPosition);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner leftNView = (AppCompatSpinner) findViewById(R.id.sub_rule_left_n);
        leftNView.setSelection(subRule.getDistanceToLeftPhrase() - 1);
        leftNView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                // Left N value change listener
                if ((selectedPosition + 1) != subRule.getDistanceToLeftPhrase()) {
                    subRule.setDistanceToLeftPhrase(selectedPosition + 1);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner rightNView = (AppCompatSpinner) findViewById(R.id.sub_rule_right_n);
        rightNView.setSelection(subRule.getDistanceToRightPhrase() - 1);
        rightNView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            // Right N value change listener
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if ((selectedPosition + 1) != subRule.getDistanceToRightPhrase()) {
                    subRule.setDistanceToRightPhrase(selectedPosition + 1);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });



        //========================================================================================
        ArrayAdapter<String> PhraseAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, phrases);
        PhraseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        AppCompatSpinner leftPhraseView = (AppCompatSpinner) findViewById(R.id.sub_rule_left_n_phrase);
        leftPhraseView.setAdapter(PhraseAdapter);
        leftPhraseView.setSelection(phrases.indexOf(subRule.getLeftPhrase()));
        leftPhraseView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View v, int selectedPosition, long id) {
                //Left phrase spinner change listener
                if (!phrases.get(selectedPosition).equals(subRule.getLeftPhrase())) {
                    subRule.setLeftPhrase(phrases.get(selectedPosition));
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner rightPhraseView = (AppCompatSpinner) findViewById(R.id.sub_rule_right_n_phrase);
        rightPhraseView.setAdapter(PhraseAdapter);
        rightPhraseView.setSelection(phrases.indexOf(subRule.getRightPhrase()));
        rightPhraseView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
               if (!phrases.get(selectedPosition).equals(subRule.getRightPhrase())) {
                    subRule.setRightPhrase(phrases.get(selectedPosition));
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        TextView constantView = (TextView) findViewById(R.id.sub_rule_constant_value);
        switch (subRule.getExtractionMethod()) {
            case WORD_AFTER_PHRASE:
            case WORD_BEFORE_PHRASE:
            case WORDS_BETWEEN_PHRASES:
                constantView.setText("");
                break;
            case USE_CONSTANT:
                constantView.setText(subRule.getConstantValue());
                break;
        }

        constantView.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                            return false;
                        } else if (actionId == EditorInfo.IME_ACTION_SEARCH
                                || event == null
                                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            // the user is done typing.
                            if (subRule.getExtractionMethod() == SubRule.Method.USE_CONSTANT) {
                                subRule.setConstantValue(v.getText().toString());
                                refreshResult();
                            }
                            return true;
                        }
                        return false; // pass on to other listeners.
                    }
                });

        //========================================================================================
        CheckBox negateView = (CheckBox) findViewById(R.id.sub_rule_negate);
        negateView.setChecked(subRule.isNegate());
        negateView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                // Adding Negate checkbox listener
                if (isChecked != subRule.isNegate()) {
                    subRule.setNegate(isChecked);
                    refreshResult();
                }
            }
        });

        //========================================================================================
        AppCompatSpinner separatorView = (AppCompatSpinner) findViewById(R.id.sub_rule_separator);
        separatorView.setSelection(subRule.getDecimalSeparator());
        separatorView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.getDecimalSeparator() != selectedPosition) {
                    subRule.setDecimalSeparator(selectedPosition);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner ignoreNLeftView = (AppCompatSpinner) findViewById(R.id.sub_rule_ignore_n_first);
        ignoreNLeftView.setSelection(subRule.getTrimLeft());
        ignoreNLeftView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.getTrimLeft() != selectedPosition) {
                    subRule.setTrimLeft(selectedPosition);
                    refreshResult();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        //========================================================================================
        AppCompatSpinner ignoreNRightView = (AppCompatSpinner) findViewById(R.id.sub_rule_ignore_n_last);
        ignoreNRightView.setSelection(subRule.getTrimRight());
        ignoreNRightView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int selectedPosition, long id) {
                if (subRule.getTrimRight() != selectedPosition) {
                    subRule.setTrimRight(selectedPosition);
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
        TextView resultView = (TextView) findViewById(R.id.sub_rule_result_value);
        // hiding unused views depending on used method
        switch (subRule.getExtractionMethod()){
            case WORD_AFTER_PHRASE:
                findViewById(R.id.sub_rule_left_n).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_phrase).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_label).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_label2).setVisibility(View.VISIBLE);

                findViewById(R.id.sub_rule_right_n).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_phrase).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_label).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_label2).setVisibility(View.GONE);

                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.GONE);
                break;
            case WORD_BEFORE_PHRASE:
                findViewById(R.id.sub_rule_left_n).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_phrase).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_label).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_label2).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_phrase).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_label).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_label2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.GONE);
                break;
            case WORDS_BETWEEN_PHRASES:
                findViewById(R.id.sub_rule_left_n).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_phrase).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_label).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_left_n_label2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_phrase).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_label).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_right_n_label2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.GONE);
                break;
            case USE_CONSTANT:
                findViewById(R.id.sub_rule_left_n).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_phrase).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_label).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_left_n_label2).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_phrase).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_label).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_right_n_label2).setVisibility(View.GONE);
                findViewById(R.id.sub_rule_ignore).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_first).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore_n_last).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_ignore2).setVisibility(View.VISIBLE);
                findViewById(R.id.sub_rule_constant_value).setVisibility(View.VISIBLE);
                break;
        }

        Log.d(LOG,"refreshResult is called");
        switch (subRule.getExtractedParameter()){
            case CURRENCY:
                resultView.setText(subRule.applySubRule(rule.getSmsBody(), 1));
                break;
            case ACCOUNT_STATE_BEFORE:
            case ACCOUNT_STATE_AFTER:
            case ACCOUNT_DIFFERENCE:
            case COMMISSION:
                resultView.setText(subRule.applySubRule(rule.getSmsBody(), 0));
                break;
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
                DatabaseAccess db= DatabaseAccess.getInstance(getApplicationContext());
                db.open();
                db.deleteSubRule(subRule.getId());
                db.close();
                finish();
                break;
            default:
                Log.e(LOG, "Unsupported view!");
        }
    }

    @Override
    public void onBackPressed() {
        //Saving subrule changes to db and going back in stack
        DatabaseAccess db= DatabaseAccess.getInstance(getApplicationContext());
        db.open();
        db.addOrEditSubRule(subRule);
        db.close();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Saving subrule changes to db and going back in stack
                DatabaseAccess db= DatabaseAccess.getInstance(getApplicationContext());
                db.open();
                db.addOrEditSubRule(subRule);
                db.close();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
