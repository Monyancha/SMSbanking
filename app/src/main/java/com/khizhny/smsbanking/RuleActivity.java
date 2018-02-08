package com.khizhny.smsbanking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.TextView;

import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;
import com.khizhny.smsbanking.model.Word;

import static com.khizhny.smsbanking.MainActivity.KEY_RULE_ID;
import static com.khizhny.smsbanking.MainActivity.KEY_SMS_BODY;
import static com.khizhny.smsbanking.MainActivity.KEY_TODO;
import static com.khizhny.smsbanking.MainActivity.KEY_TODO_ADD;
import static com.khizhny.smsbanking.MyApplication.db;
import static com.khizhny.smsbanking.MyApplication.forceRefresh;

public class RuleActivity extends AppCompatActivity implements View.OnClickListener{

    private List<Button> wordButtons= new ArrayList <Button>();
    private Rule rule;

    private TextView tvMessageBody;
	private TextView tvResults;
	private ImageView ivIcon;
    private AlertDialog alertDialog;
    private CheckBox cbAdvanced;
    private EditText etRegExp;
    private boolean weNeedToDeleteAllSubrules=false;  // if flag is set then old subrules will be deleted because regex mask is now changed.
    private OnSwipeTouchListener onSwipeTouchListener=new OnSwipeTouchListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);

        // getting rule object for editing
        Intent intent = getIntent();
        Bank bank = db.getActiveBank();
        Bundle bundle=intent.getExtras();
        if (bundle!=null) {
            String todo = bundle.getString(KEY_TODO);
            int rule_id = bundle.getInt(KEY_RULE_ID);
            if (todo != null) {
                if (todo.equals(KEY_TODO_ADD)) {
                    // adding new rule
                    rule = new Rule(bank, "");
                    rule.setSmsBody(intent.getExtras().getString(KEY_SMS_BODY));
                    rule.makeInitialWordSplitting();
                } else {
                    // picking existing rule for editing.
                    for (Rule r : bank.ruleList) {
                        if (r.getId() == rule_id)
                            rule = r;
                    }
                }
            }

            // restoring user changes
            if (savedInstanceState != null) {
                rule.setName(savedInstanceState.getString("rule_name"));
                rule.setRuleType(savedInstanceState.getInt("rule_type"));
                int wordsCount = savedInstanceState.getInt("words_count");
                rule.words.clear();
                for (int i = 0; i < wordsCount; i++) {
                    rule.words.add((Word) savedInstanceState.getSerializable("word" + i));
                }
                weNeedToDeleteAllSubrules = savedInstanceState.getBoolean("delete_rules");
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("rule_name", rule.getName());
        savedInstanceState.putInt("rule_type", rule.getRuleTypeInt());
        savedInstanceState.putInt("words_count", rule.words.size());
        for (int i = 0; i<rule.words.size(); i++) {
            savedInstanceState.putSerializable("word"+i,rule.words.get(i));
        }
        savedInstanceState.putBoolean("delete_rules", weNeedToDeleteAllSubrules);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_rule);

        // Coloring sample buttons
        changeColor(findViewById(R.id.btn_variable), Word.WORD_TYPES.WORD_VARIABLE);
        changeColor(findViewById(R.id.btn_fixed), Word.WORD_TYPES.WORD_CONST);
        changeColor(findViewById(R.id.btn_variable_fixed_size), Word.WORD_TYPES.WORD_VARIABLE_FIXED_SIZE);

        cbAdvanced=findViewById(R.id.cbAdvanced);
        cbAdvanced.setChecked(rule.isAdvanced());
        cbAdvanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rule.setAdvanced(isChecked?1:0);
                if (isChecked) {
                    etRegExp.setVisibility(View.VISIBLE);
                }else{
                    rule.updateMask();
                    etRegExp.setText(rule.getMask());
                    etRegExp.setVisibility(View.GONE);
                }
            }
        });

        etRegExp=findViewById(R.id.etRegex);
        etRegExp.setText(rule.getMask());
        etRegExp.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //KeyEvent: If triggered by an enter key, this is the event; otherwise, this is null.
                if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                } else if (actionId == EditorInfo.IME_ACTION_DONE
                        || event == null
                        || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // the user is done typing.
                    rule.setMask(v.getText().toString());
                    weNeedToDeleteAllSubrules=true;
                    tvResults.setText(rule.getValues());
                    return true;
                }
                return false; // pass on to other listeners.
            }
        });

        if (rule.isAdvanced()) {
            etRegExp.setVisibility(View.VISIBLE);
        }else{
            etRegExp.setVisibility(View.GONE);
        }

        tvResults=findViewById(R.id.tvResults);
        tvResults.setText(rule.getValues());

		tvMessageBody =  findViewById(R.id.rule_sms_body);
		tvMessageBody.setText(rule.getSmsBody());
        tvMessageBody.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //KeyEvent: If triggered by an enter key, this is the event; otherwise, this is null.
                if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                } else if (actionId == EditorInfo.IME_ACTION_DONE
                        || event == null
                        || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // the user is done typing.
                    rule.setSmsBody(v.getText().toString());
                    rule.makeInitialWordSplitting();
                    weNeedToDeleteAllSubrules=true;
                    updateWordsLayout();
                    return true;
                }
                return false; // pass on to other listeners.
            }
        });

		AppCompatSpinner ruleTypeView = this.findViewById(R.id.rule_type);
        ivIcon = this.findViewById(R.id.image);
        if (ruleTypeView != null) {
            ruleTypeView.setSelection(rule.getRuleTypeInt());
        }
        if (ruleTypeView != null) {
            ruleTypeView.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    ivIcon.setImageResource(Rule.ruleTypeIcons[position]);
                    rule.setRuleType(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    ivIcon.setImageResource(Rule.ruleTypeIcons[0]);
                    rule.setRuleType(0);/**/
                }
            });
        }
        ivIcon.setImageResource(rule.getRuleTypeDrawable());
        updateWordsLayout();


    }

    /**
     * Changes color of a word button
     * @param v - buttoon View
     * @param word_type - Selected word type
     */
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
            case R.id.rule_next:
                onNextBtnClick();
                break;
            default:
                break;

        }
    }

    /**
     * next button click handler
     */
    private void onNextBtnClick(){
        // Saving all changes to Rule object and saves them to db
        if (rule.isAdvanced()) {
            rule.setMask(etRegExp.getText().toString());
        }else {
            rule.updateMask();
        }

        rule.setName(rule.getRuleNameSuggestion());

        removeOldSubrules();
        //  If we changed mask or editing old rule entire rule must be redefined.
        if (weNeedToDeleteAllSubrules) {
            for (SubRule sr: rule.subRuleList)
                db.deleteSubRule(sr.getId());
            rule.subRuleList.clear();
        }

        // Saving Rule changes in DB.
        db.addOrEditRule(rule,true);

        if (rule.getRuleType()== Rule.transactionType.IGNORE) {
            // if subrules is not needed going back to MainActivity
            forceRefresh=true;
            super.onBackPressed();
        } else {
            Intent intent = new Intent(this, TransactionActivity.class);
            intent.putExtra(KEY_RULE_ID, rule.getId());
            startActivity(intent);
        }
    }

    /**
     * Removes subRules with old extraction methods to prevent their modify attempts
     */
    public void removeOldSubrules(){
        Iterator<SubRule> i = rule.subRuleList.iterator();
        while (i.hasNext()) {
            SubRule sr = i.next();
            switch (sr.getExtractionMethod())
            {
                case WORD_AFTER_PHRASE:
                case WORD_BEFORE_PHRASE:
                case WORDS_BETWEEN_PHRASES:
                    db.deleteSubRule(sr.getId());
                    i.remove();
                    break;
                case USE_CONSTANT:
                case USE_REGEX:
                    break;
            }
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
        builder.setNeutralButton("<<", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                w.rule.mergeLeft(w);
                dialog.dismiss();
                weNeedToDeleteAllSubrules=true;
                updateWordsLayout();
            }
        });

        builder.setPositiveButton(">>", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                w.rule.mergeRight(w);
                dialog.dismiss();
                weNeedToDeleteAllSubrules=true;
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
                    weNeedToDeleteAllSubrules=true;
                    dialog.dismiss();
                    updateWordsLayout();
                }
            });
        }
        alertDialog=builder.create();
        alertDialog.show();
    }

    @SuppressLint("ClickableViewAccessibility")
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
                wordButton.setTag(rule.words.get(i-1));
                wordButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        rule.setAdvanced(0);
                        cbAdvanced.setChecked(false);
                        Word word = (Word) v.getTag();
                        word.changeWordType();
                        changeColor(v, word.getWordType());
                        weNeedToDeleteAllSubrules = true;
                        refreshResults();
                    }
                });
                wordButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        rule.setAdvanced(0);
                        cbAdvanced.setChecked(false);
                        showDialogToChangeWord((Word) v.getTag());
                        return false;
                    }
                });
                wordButton.setOnTouchListener(onSwipeTouchListener);

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
        refreshResults();
    }
    private void refreshResults(){
        rule.updateMask();
        tvResults.setText(rule.getValues());
        etRegExp.setText(rule.getMask());
    }

    /**
     * Detects left and right swipes across a view.
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector = new GestureDetector(new GestureListener());

        Word word;


        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(final View v, final MotionEvent event) {
            word=(Word) v.getTag();
            return gestureDetector.onTouchEvent(event);

        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                result = onSwipeRight();
                            } else {
                                result = onSwipeLeft();
                            }
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                result = onSwipeBottom();
                            } else {
                                result = onSwipeTop();
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        boolean onSwipeRight() {
            word.rule.mergeRight(word);
            weNeedToDeleteAllSubrules=true;
            updateWordsLayout();
            return true;
        }

        boolean onSwipeLeft() {
            word.rule.mergeLeft(word);
            weNeedToDeleteAllSubrules=true;
            updateWordsLayout();
            return true;
        }

        boolean onSwipeTop() {
            return false;
        }

        boolean onSwipeBottom() {
            return false;
        }
    }
}