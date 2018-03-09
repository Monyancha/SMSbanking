package com.khizhny.smsbanking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Rule;
import com.khizhny.smsbanking.model.SubRule;
import com.khizhny.smsbanking.model.Transaction;
import com.khizhny.smsbanking.model.Word;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class DatabaseAccess {
    private final SQLiteOpenHelper openHelper;
    private SQLiteDatabase db;
    private static DatabaseAccess instance;

    /**
     * Private constructor to aboid object creation from outside classes.
     *
     * @param context context
     */
    DatabaseAccess(Context context) {
        this.openHelper = new DbOpenHelper(context);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param context the Context
     * @return the instance of DabaseAccess
     */
    public static synchronized DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    /**
     * Open the database connection.
     */
    public void open() {
        try {
            this.db = openHelper.getWritableDatabase();
        } catch (Exception e){
            e.printStackTrace();
            Log.e(LOG,"Incompatible db version found");
        }
    }

      /**
     * Close the database connection.
     */
    public void close() {
        if (db != null) {
            this.db.close();
        }
    }

    /**
     * @return a List of Banks with Rules and Subrules.
     */
    public synchronized List<Bank> getBanks(@NonNull String country) {
        List<Bank> bankList = new ArrayList<>();
        String selectQuery = "SELECT _id FROM banks WHERE editable<>0 and country=?";
        Cursor cursor= db.rawQuery(selectQuery, new String[]{country});
            // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bank bank = getBank(cursor.getInt(0),true);
                bankList.add(bank);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bankList;
    }

    public synchronized void setDefaultCountry (String country) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("country", country);
        String nullSelection = "country" + " IS NULL";
        db.update("banks", contentValues, nullSelection, null);

        //db.rawQuery("UPDATE banks SET country=? WHERE country='null'", new String[]{country});
    }

    public synchronized void setActiveBank (int bankId) {
        db.execSQL("UPDATE banks SET active=0");
        db.execSQL("UPDATE banks SET active=1 WHERE _id=" + bankId + " and editable<>0");
    }

    /**
     * Reads BankV2 object from db with all Rules,Subrules,Words
     * @param bankId BankV2 id.
     * @return BankV2 object.
     */
    public synchronized Bank getBank (int bankId, boolean withRules) {
        Bank b = new Bank();
        Cursor cursor = db.rawQuery("SELECT _id, name, phone, active, default_currency,editable,current_account_state,country FROM banks WHERE _id="+bankId, null);
        if (cursor.moveToFirst()) {
            b.setId(cursor.getInt(0));
            b.setName(cursor.getString(1));
            b.setPhone(cursor.getString(2));
            b.setActive(cursor.getInt(3));
            b.setDefaultCurrency(cursor.getString(4));
            b.setEditable(cursor.getInt(5));
            b.setCurrentAccountState(cursor.getString(6));
            b.setCountry(cursor.getString(7));
            if (withRules) getRules(b);
            cursor.close();
            return b;
        }
        cursor.close();
        return null;
    }

    /**
     * Returns Active Bank object.
     * @return Bank object or null if not found.
     */
    public Bank getActiveBank () {
        Cursor c=db.rawQuery("SELECT _id FROM banks where active=1 and editable=1", null);
        int bankId=0;
        if (c.moveToFirst()) bankId=c.getInt(0);
        c.close();
        if (bankId>0) {
            return getBank(bankId,true);
        }
        return null;
    }

    /**
     * Sets any bank from myBanks as active. Used after bank is deleted.
     */
    public synchronized void setActiveAnyBank () {
        db.execSQL("UPDATE banks SET active=0");
        db.execSQL("UPDATE banks SET active=1 WHERE _id=(SELECT MAX(_id) FROM banks WHERE editable=1)");
    }

    /**
     * Deletes bank with bankId from database. Including all related Rules and Subrules.
     * @param bankId BankV2 id
     */
    public synchronized void deleteBank (int bankId) {
        // deleting all subrules and rules of active bank
        db.execSQL("DELETE FROM subrules WHERE rule_id IN (SELECT _id FROM rules WHERE bank_id="+bankId+")");
        db.execSQL("DELETE FROM rule_conflicts WHERE rule_id IN (SELECT _id FROM rules WHERE bank_id="+bankId+")");
        db.execSQL("DELETE FROM rules WHERE bank_id=" + bankId);
        db.execSQL("DELETE FROM transactions WHERE bank_id=" + bankId);
        db.execSQL("DELETE FROM banks WHERE _id=" + bankId);
    }

    public synchronized void deleteBankCache (int bankId) {
        // deleting all subrules and rules of active bank
        db.execSQL("DELETE FROM transactions WHERE bank_id=" + bankId);
    }

    /**
     * If bank ID<=0 then new bank will be added to db. Otherwise bank will be updated
     * @param bank - bank object
     * @param withRules - update rules as well.
     */
    public synchronized void addOrEditBank (Bank bank, boolean withRules, boolean withSubRules) {
        if (db.isReadOnly())
        {
            Log.d(LOG,"Cant open db with WR rights");
            return;
        }
        if (bank.getId()<=0){	// Adding new bank
            ContentValues v = new ContentValues();
            // making all banks not active
            v.put("active", 0);
            db.update("banks", v, "editable <> ?", new String[]{"0"});
            // making new bank active
            bank.setActive(1);
            bank.setEditable(1);
            // saving BankV2 info
            v = bank.getContentValues();
            db.insert("banks",null,v);

            // querying id
            Cursor c=db.rawQuery("SELECT MAX(_id) FROM banks",null);
            // returning new rule id
            int id=0;
            if (c.moveToFirst()) id=c.getInt(0);
            c.close();
            bank.setId(id);

        }else
        {	// Updating bank info
            ContentValues v =  bank.getContentValues();
            db.update("banks", v, "_id=? and editable<>?", new String[]{bank.getId()+"","0"});
        }
        if (withRules){
            for (Rule r: bank.ruleList){
                addOrEditRule(r,withSubRules);
            }
        }
    }

    /**
     * @param bank Bank.
     * @return list of rules for particular Bank (including subrules)
     */
    private synchronized void getRules(Bank bank){
        Cursor cursor = db.rawQuery("SELECT _id, name, sms_body, mask, selected_words, type, advanced FROM rules WHERE bank_id=" + bank.getId(), null);
        if (cursor.moveToFirst()) {
            do {
                Rule r = new Rule(bank,cursor.getString(1));
                r.setId(cursor.getInt(0));
                r.setSmsBody(cursor.getString(2));
                r.setMask(cursor.getString(3));
                r.setSelectedWords(cursor.getString(4));
                r.setRuleType(cursor.getInt(5));
                r.setAdvanced(cursor.getInt(6));
								getSubRules(r);
                getWords(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public void cacheTransaction  (ContentValues cv){
        db.insert("transactions", null, cv);
    }

    public synchronized List<Transaction> getTransactionCache(int bankId){
        List<Transaction> transactionList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT transaction_date,account_currency,sms_body,icon,transaction_currency,state_before,state_after,state_difference,commission,extra1,extra2,extra3,extra4,exchange_rate,sms_id FROM transactions WHERE bank_id=" + bankId, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Date transactionDate =  new Date (cursor.getLong(0));
                Transaction t = new Transaction(cursor.getString(2),cursor.getString(1),transactionDate); // transaction_date,account_currency, sms_body,
                t.icon=cursor.getInt(3); // icon,
                t.setTransactionCurrency(cursor.getString(4));// transaction_currency,
                if (cursor.getString(5)!=null) t.setStateBefore(cursor.getString(5));// state_before,
                if (cursor.getString(6)!=null)t.setStateAfter(cursor.getString(6));// state_after,
                if (cursor.getString(7)!=null)t.setDifference(cursor.getString(7));// state_difference,
                t.setComission(cursor.getString(8));// commission,
                t.setExtraParam1(cursor.getString(9));// extra1,
                t.setExtraParam2(cursor.getString(10));// extra2,
                t.setExtraParam3(cursor.getString(11));// extra3,
                t.setExtraParam4(cursor.getString(12));// extra4,
                t.setCurrencyRate(cursor.getString(13));// exchange_rate
                t.smsId=cursor.getLong(14);
                t.isCached=true;
                transactionList.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactionList;
    }

    /**
     * Saves or Updates Rule in database with subrules and words.
     * If Rule.id=-1 new record will be created. Otherwise Rule with such id is updated.
     * @param r Rule
     */
    public synchronized void addOrEditRule(Rule r, boolean withSubrules){
        int id = r.getId();
        if (id>=1) {
            //Updating existing rule with same ID
            db.update("rules", r.getContentValues(), "_id=?", new String[]{id + ""});
        }else{
            // adding new rule
            db.insert("rules", null, r.getContentValues());
            // querying Rule id to return
            Cursor c=db.rawQuery("SELECT MAX(_id) FROM rules",null);
            // returning new rule id
            if (c.moveToFirst()) id=c.getInt(0);
            c.close();
            r.setId(id);
        }

        // updating Words
        if (r.words.size()>0) addOrEditWords(r);

        if (withSubrules) {
            // updating Subrules
            for (SubRule subrule : r.subRuleList) {
                addOrEditSubRule(subrule);
            }
        }
    }

    /**
     * Gets Rule (with subrules) from DB by ID
     * @param ruleId Rule ID.
     * @return Rule object
     */
    synchronized Rule getRule(int ruleId){
				Log.v(LOG, "DatabaseAccess.getRule()");
				Cursor cursor = db.rawQuery("SELECT _id, name, sms_body, mask, selected_words, type, advanced, bank_id FROM rules WHERE _id=" + ruleId, null);
				if (cursor.moveToFirst()) {
						do {
								int bankId=cursor.getInt(7);
								Bank bank = getBank(bankId,false);
								Rule r = new Rule(bank, cursor.getString(1));
								r.setId(cursor.getInt(0));
								r.setSmsBody(cursor.getString(2));
								r.setMask(cursor.getString(3));
								r.setSelectedWords(cursor.getString(4));
								r.setRuleType(cursor.getInt(5));
								r.setAdvanced(cursor.getInt(6));
								getSubRules(r);
								getWords(r);
								cursor.close();
								return r;
						} while (cursor.moveToNext());
				}
				cursor.close();
				Log.e(LOG, "Requested rule not found.");
				return null;
    }

    /**
     * Deletes rule from DB with all subrules
     * @param ruleId ID of the rule to delete.
     */
    public synchronized void deleteRule (int ruleId) {
        // deleting all subrules and rule
        db.execSQL("DELETE FROM rule_conflicts WHERE rule_id="+ruleId);
        db.execSQL("DELETE FROM subrules WHERE rule_id="+ruleId);
        db.execSQL("DELETE FROM rules WHERE _id=" + ruleId);
    }

    /**
     * Gets All subrules for a Rule
     * @param rule -Rule object
     */
    private synchronized void getSubRules(Rule rule){
        String selectQuery = "SELECT \n" +
                "_id,\n" +
                "left_phrase,\n" +
                "right_phrase,\n" +
                "distance_to_left_phrase,\n" +
                "distance_to_right_phrase,\n" +
                "constant_value,\n" +
                "extracted_parameter,\n" +
                "extraction_method,\n" +
                "decimal_separator,\n" +
                "trim_left,\n" +
                "trim_right,\n" +
                "negate,\n" +
                "regex_phrase_index\n" +
                "FROM subrules \n" +
                "WHERE rule_id="+rule.getId();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                SubRule subRule = new SubRule(rule,Transaction.Parameters.values()[cursor.getInt(6)]);
                subRule.setId(Integer.parseInt(cursor.getString(0)));
                subRule.setLeftPhrase(cursor.getString(1));
                subRule.setRightPhrase(cursor.getString(2));
                subRule.setDistanceToLeftPhrase(cursor.getInt(3));
                subRule.setDistanceToRightPhrase(cursor.getInt(4));
                subRule.setConstantValue(cursor.getString(5));
                subRule.setExtractionMethod(cursor.getInt(7));
                subRule.setDecimalSeparator(SubRule.DECIMAL_SEPARATOR.values()[cursor.getInt(8)]);
                subRule.trimLeft=cursor.getInt(9);
                subRule.trimRight=cursor.getInt(10);
                subRule.negate=cursor.getInt(11)!=0;
                subRule.regexPhraseIndex=cursor.getInt(12);
                // Adding contact to list
                rule.subRuleList.add(subRule);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private synchronized void getWords(Rule rule){
        rule.words.clear();
        String selectQuery = "SELECT \n"+
                "first_letter_index, \n"+
                "last_letter_index, \n"+
                "word_type\n"+
                "FROM words\n"+
                "WHERE rule_id="+rule.getId()+"\n"+
                "ORDER BY first_letter_index";
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                int first_letter_index=cursor.getInt(0);
                int last_letter_index=cursor.getInt(1);
                Word.WORD_TYPES wordType= Word.WORD_TYPES.values()[cursor.getInt(2)];
                Word t = new Word(rule,first_letter_index,last_letter_index,wordType);
                rule.words.add(t);
            } while (cursor.moveToNext());
        }
        cursor.close();
        if (rule.words.size()==0) Rule.makeInitialWordSplitting(rule);
    }

    /**
     * Rewrites all Words for a Rule
     * @param rule Rule object.
     */
    private synchronized void addOrEditWords(Rule rule){
        if (rule.getId()>0) {
            // deleting all existing word from db if they exist for this rule
            db.delete("words","rule_id=?", new String[]{rule.getId() + ""});
            // saving all words in the list
            for (Word w:rule.words){
                db.insert("words",null, w.getContentValues());
            }
        }
    }

    /**
     * Adds or Edits Subrule to DB depending on SubRule ID.
     * if ID<=0 rule will be added and new ID will be returned.
     * Otherwise corrresponding record record will be updated.
     * @param sr Subrule object
     */
    synchronized void addOrEditSubRule(SubRule sr){
        if (sr.getId()<=0){// Adding new SubRule
            long rowId = db.insert("subrules",null,sr.getContentValues());
            if (rowId>=0) {
                Cursor c=db.rawQuery("SELECT _id " +
                        "FROM subrules " +
                        "WHERE ROWID="+rowId,null);
                if (c.moveToFirst()) {
                    sr.setId(c.getInt(0));
                }
                c.close();
            } else {
                Log.d(LOG,"Error while inserting new Subrule");
            }
        }else {	// Updating existing Rule
            db.update("subrules", sr.getContentValues(), "_id=?", new String[]{sr.getId() + ""});
        }
    }

    /**
     * Deletes subrule from the database.
     * @param subRuleId Subreule id to delete.
     */
    synchronized void deleteSubRule (int subRuleId) {
        db.delete("subrules","_id=?", new String[]{subRuleId + ""});
    }


    /**
     * Saves to db user choices for messages with conflictiong rules.
     * @param ruleID Rule ID that have to be used in case of several rules conflict.
     * @param date Time when message was recieved. Used to identify selected SMS.
     */
    public synchronized void saveRuleConflictChoice(int ruleID, Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        ContentValues v = new ContentValues();
        v.put("rule_id", ruleID);
        v.put("message_date", dateFormat.format(date));
        int rowsUpdated=db.update("rule_conflicts", v, "message_date='" + dateFormat.format(date)+"'",null);
        if (rowsUpdated==0){
            if (db.insert("rule_conflicts", null, v)==-1){
                Log.d(LOG,"Error updatind rule_conflicts");
            }
        }
    }

    /**
     * Returns rule ID choosed manualy by the user.
     * @param date Date on recieving of sms message.
     * @return Rule ID (if found in db) or -1.
     */
    public synchronized int getRuleIdFromConflictChoices(Date date){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Cursor c= db.rawQuery("SELECT rule_id FROM rule_conflicts WHERE message_date='" + dateFormat.format(date)+"'",null);
            if (!c.moveToFirst()) {
                return -1;
            } else {
                int id=c.getInt(0);
                c.close();
                return id;
            }
        } catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

}
