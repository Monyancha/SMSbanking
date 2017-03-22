package com.khizhny.smsbanking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import static com.khizhny.smsbanking.MyApplication.LOG;

public class DatabaseAccess {
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db;
    private static DatabaseAccess instance;

    /**
     * Private constructor to aboid object creation from outside classes.
     *
     * @param context context
     */
    private DatabaseAccess(Context context) {
        this.openHelper = new DbOpenHelper(context);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param context the Context
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Context context) {
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
     *
     * @return a list of read only banks. Can be used just as a tamplate.
     */
    public synchronized List<Bank> getBankTemplates () {
        List<Bank> bankList = new ArrayList<Bank>();
        String selectQuery = "SELECT _id, name, phone, active, default_currency FROM banks WHERE editable=0";
        if (!db.isOpen()) open();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Bank bank = new Bank();
                bank.setId(cursor.getInt(0));
                bank.setName(cursor.getString(1));
                bank.setPhone(cursor.getString(2));
                bank.setActive(cursor.getInt(3));
                bank.setDefaultCurrency(cursor.getString(4));

                // Adding Rules
                bank.ruleList=getAllRules(bank.getId());

                bankList.add(bank);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bankList;
    }

    /**
     * @return a List of Banks allowed to edit by the user.
     */
    public synchronized List<Bank> getMyBanks () {
        List<Bank> bankList = new ArrayList<Bank>();
        String selectQuery = "SELECT _id, name, phone, active, default_currency,current_account_state FROM banks WHERE editable<>0";
        Cursor cursor;
        if (!db.isOpen()) open();
        try {
            cursor = db.rawQuery(selectQuery, null);
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Bank bank = new Bank();
                    bank.setId(Integer.parseInt(cursor.getString(0)));
                    bank.setName(cursor.getString(1));
                    bank.setPhone(cursor.getString(2));
                    bank.setActive(Integer.parseInt(cursor.getString(3)));
                    bank.setDefaultCurrency(cursor.getString(4));
                    bank.setCurrentAccountState(cursor.getString(5));
                    // Adding Rules
                    bank.ruleList=getAllRules(bank.getId());
                    bankList.add(bank);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e){
            Log.e(LOG, "Error reading MyBanks from db. ");
            e.printStackTrace();
        }
        return bankList;
    }

    public synchronized void setActiveBank (int bankId) {
        if (!db.isOpen()) open();
        db.execSQL("UPDATE banks SET active=0");
        db.execSQL("UPDATE banks SET active=1 WHERE _id=" + bankId + " and editable<>0");
    }

    /**
     * Reads Bank object from db with all Rules and Subrules
     * @param bankId Bank id.
     * @return Bank object.
     */
    public synchronized Bank getBank (int bankId) {
        Bank b = new Bank();
        if (!db.isOpen()) open();
        Cursor cursor = db.rawQuery("SELECT _id, name, phone, active, default_currency,editable,current_account_state FROM banks WHERE _id="+bankId, null);
        if (cursor.moveToFirst()) {
            b.setId(cursor.getInt(0));
            b.setName(cursor.getString(1));
            b.setPhone(cursor.getString(2));
            b.setActive(cursor.getInt(3));
            b.setDefaultCurrency(cursor.getString(4));
            b.setEditable(cursor.getInt(5));
            b.setCurrentAccountState(cursor.getString(6));
            b.ruleList=getAllRules(cursor.getInt(0));
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
        if (db.isOpen()){
            Cursor c=db.rawQuery("SELECT _id FROM banks where active=1 and editable=1", null);
            int bankId=0;
            if (c.moveToFirst()) bankId=c.getInt(0);
            c.close();

            if (bankId>0) {
                return getBank(bankId);
            }
        } else {
            Log.e(LOG,"DB already closed.");
        }
        return null;
    }

    /**
     * Sets any bank from myBanks as active. Used after bank is deleted.
     */
    public synchronized void setActiveAnyBank () {
        if (!db.isOpen()) open();
        db.execSQL("UPDATE banks SET active=0");
        db.execSQL("UPDATE banks SET active=1 WHERE _id=(SELECT MAX(_id) FROM banks WHERE editable=1)");
    }

    /**
     * Deletes bank with bankId from database. Including all related Rules and Subrules.
     * @param bankId Bank id
     */
    public synchronized void deleteBank (int bankId) {
        // deleting all subrules and rules of active bank
        if (!db.isOpen()) open();
        db.execSQL("DELETE FROM subrules WHERE rule_id IN (SELECT _id FROM rules WHERE bank_id="+bankId+")");
        db.execSQL("DELETE FROM rule_conflicts WHERE rule_id IN (SELECT _id FROM rules WHERE bank_id="+bankId+")");
        db.execSQL("DELETE FROM rules WHERE bank_id=" + bankId);
        db.execSQL("DELETE FROM banks WHERE _id=" + bankId);
    }

    /**
     * If bank ID<=0 then new bank will be added to db.
     * Otherwise bank will be updated
     * @param b Bank Object to Add or Edit.
     */
    public synchronized void addOrEditBank (Bank b) {
        if (db.isReadOnly())
        {
            Log.d(LOG,"Cant open db with WR rights");
            return;
        }
        if (b.getId()<=0){	// Adding new bank
            ContentValues v = new ContentValues();
            // making all banks not active
            v.put("active", 0);
            db.update("banks", v, "editable <> ?", new String[]{"0"});
            // making new bank active
            b.setActive(1);
            b.setEditable(1);
            // saving Bank info
            v = b.getContentValues();
            db.insert("banks",null,v);
        }else
        {	// Updating bank info
            ContentValues v =  b.getContentValues();
            db.update("banks", v, "_id=? and editable<>?", new String[]{b.getId()+"","0"});
        }
    }

    /**
     * @param bankId Bank Id.
     * @return a list of rules for particulat Bank (including subrules)
     */
    public synchronized List<Rule> getAllRules(int bankId){
        List<Rule> ruleList = new ArrayList<Rule>();
        if (!db.isOpen()) open();
        Cursor cursor = db.rawQuery("SELECT _id, name, sms_body, mask, selected_words, bank_id, type FROM rules WHERE bank_id=" + bankId, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Rule r = new Rule(cursor.getInt(5),cursor.getString(1));
                r.setId(cursor.getInt(0));
                r.setSmsBody(cursor.getString(2));
                r.setMask(cursor.getString(3));
                r.setSelectedWords(cursor.getString(4));
                r.setRuleType(cursor.getInt(6));
                r.subRuleList=getSubRules(r.getId());
                ruleList.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ruleList;
    }

    /**
     * Saves or Updater Rule in database without subrules.
     * If Rule.id=-1 new record will be created.
     * Otherwise Rile with this id will be updated.
     * @param r Rule
     * @return Saved rule ID.
     */
    public synchronized int addOrEditRule(Rule r){
        if (!db.isOpen()) open();
        if (r.getId()>=1) { //Updating existing rule
            int id = r.getId();
            db.update("rules", r.getContentValues(), "_id=?", new String[]{id + ""});
            return id;
        }else{ // adding new rule
            db.insert("rules", null, r.getContentValues());
            // querying Rule id to return
            Cursor c=db.rawQuery("SELECT MAX(_id) FROM rules",null);
            int id=0;
            if (c.moveToFirst()) id=c.getInt(0);
            c.close();
            return id;
        }

    }

    /**
     * Gets Rule from DB by ID
     * @param ruleId Rule ID.
     * @return Rule object
     */
    public synchronized Rule getRule(int ruleId){
        String selectQuery = "SELECT _id, name, sms_body, mask, selected_words, bank_id, type FROM rules WHERE _id="+ruleId;
        if (!db.isOpen()) open();
        Cursor cursor = db.rawQuery(selectQuery, null);
        Rule r;
        if (cursor.moveToFirst()) {
            r = new Rule(cursor.getInt(5),cursor.getString(1));
            r.setId(cursor.getInt(0));
            r.setSmsBody(cursor.getString(2));
            r.setMask(cursor.getString(3));
            r.setSelectedWords(cursor.getString(4));
            r.setRuleType(cursor.getInt(6));
            r.subRuleList=getSubRules(cursor.getInt(0));
            cursor.close();
        }
        else {
            cursor.close();
            Log.e("DatabaseHelper.getRule", "rule id duplicated or not found.");
            return null;
        }
        return r;
    }
    /**
     * Deletes rule from DB with all subrules
     * @param ruleId ID of the rule to delete.
     */
    public synchronized void deleteRule (int ruleId) {
        // deleting all subrules and rule
        if (!db.isOpen()) open();
        db.execSQL("DELETE FROM rule_conflicts WHERE rule_id="+ruleId);
        db.execSQL("DELETE FROM subrules WHERE rule_id="+ruleId);
        db.execSQL("DELETE FROM rules WHERE _id=" + ruleId);
    }
    /**
     * Returns a list of subrules for particular Rule
     * @param ruleId ID of the rule.
     * @return a list of subrules for particular Rule
     */
    public synchronized List<SubRule> getSubRules(int ruleId){
        if (!db.isOpen()) open();
        List<SubRule> subRuleList = new ArrayList<SubRule>();
        String selectQuery = "SELECT _id, left_phrase,right_phrase, distance_to_left_phrase, distance_to_right_phrase, constant_value, extracted_parameter,extraction_method,decimal_separator,trim_left,trim_right,negate  FROM subrules WHERE rule_id="+ruleId;
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                SubRule r = new SubRule(ruleId);
                r.setId(Integer.parseInt(cursor.getString(0)));
                r.setLeftPhrase(cursor.getString(1));
                r.setRightPhrase(cursor.getString(2));
                r.setDistanceToLeftPhrase(cursor.getInt(3));
                r.setDistanceToRightPhrase(cursor.getInt(4));
                r.setConstantValue(cursor.getString(5));
                r.setExtractedParameter(cursor.getInt(6));
                r.setExtractionMethod(cursor.getInt(7));
                r.setDecimalSeparator(cursor.getInt(8));
                r.setTrimLeft(cursor.getInt(9));
                r.setTrimRight(cursor.getInt(10));
                if (cursor.getInt(11)==0) {
                    r.setNegate(false);
                } else {
                    r.setNegate(true);
                }
                // Adding contact to list
                subRuleList.add(r);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return subRuleList;
    }

    /**
     * Adds or Edits Subrule to DB depending on SubRule ID.
     * if ID<=0 rule will be added and new ID will be returned.
     * Otherwise corrresponding record record will be updated.
     * @param sr Subrule object
     * @return rule ID
     */
    public synchronized int addOrEditSubRule(SubRule sr){
        int id=sr.getId();
        if (id<=0){// Adding new SubRule
            long rowId = db.insert("subrules",null,sr.getContentValues());
            if (rowId>=0) {
                Cursor c=db.rawQuery("SELECT _id FROM subrules WHERE ROWID="+rowId,null);
                if (c.moveToFirst()) {
                    id= c.getInt(0);
                }
                c.close();
            } else {
                Log.d(LOG,"Error while inserting new Subrule");
            }
        }else {	// Updating existing Rule
            db.update("subrules", sr.getContentValues(), "_id=?", new String[]{id + ""});
        }
        return id;
    }

    /**
     * Deletes subrule from the database.
     * @param subRuleId Subreule id to delete.
     */
    public synchronized void deleteSubRule (int subRuleId) {
        db.delete("subrules","_id=?", new String[]{subRuleId + ""});
    }

    /**
     * Function will save Template Bank as My Bank with all Rules and SubRules.
     * @param b - Template Bank to be used as MyBank
     */
    public synchronized void useTemplate (Bank b) {
        if (db.isReadOnly())
        {
            Log.d(LOG,"Cant open db with WR rights");
            return;
        }
        ContentValues v = new ContentValues();

        // making all existing banks not active and making new bank active
        v.put("active", 0);
        db.update("banks", v, "editable <> ?", new String[]{"0"});
        b.setActive(1);
        b.setEditable(1);
        b.setId(-1);
        // saving Bank info
        v = b.getContentValues();
        long dbRowNum = db.insert("banks", null, v);
        if (dbRowNum==-1){
            Log.d(LOG,"Error inserting " +b+" to DBd ");
        }else
        {
            // getting new bank id assigned in db
            Cursor c=db.rawQuery("SELECT _id FROM banks WHERE rowid="+dbRowNum,null);
            int newBankId=0;
            if (c.moveToFirst()) newBankId=c.getInt(0);
            c.close();
            for (Rule r: b.ruleList) { //Saving rules
                r.setId(-1); // set to -1 will make new record instead updating
                r.changeBankId(newBankId);
                int newRuleId =addOrEditRule(r);
                for (SubRule sr:r.subRuleList){ //Saving SubRules
                    sr.setId(-1);  // set to -1 will make new record instead updating
                    sr.changeRuleId(newRuleId);
                    addOrEditSubRule(sr);
                }
            }
        }


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

    /**
     * Upgrades database to V6. After V6 upgardes will be made by update scripts in assets folder.
     */
    public int getDbOldVersion(){
        Cursor cursor = db.rawQuery("SELECT version FROM version", null);
        int oldVersion=0;
        if (cursor.moveToFirst()){
            oldVersion = cursor.getInt(0);
        }
        cursor.close();
        return oldVersion;
    }

    public void upgradeDbToV6() {
        Log.d("SMS_BANKING", "DATABASE UPGRADE to V6 STARTED...");
        switch (getDbOldVersion()){
            case 4:
                // these version is used only in 1.09
                db.execSQL("ALTER TABLE banks ADD COLUMN editable;");
                db.execSQL("ALTER TABLE banks ADD COLUMN current_account_state;");
                db.execSQL("UPDATE banks SET editable=1, current_account_state=0");
                db.execSQL("INSERT INTO sqlite_sequence(rowid,name,seq) VALUES(87,'banks',4);");
                db.execSQL("UPDATE version SET version=5");
                Log.v(LOG, "DB Updated from 4 to 5");
            case 5: // these version is used only  in 1.10
                // adding rule_conflicts table.
                db.execSQL(
                        "CREATE TABLE \"rule_conflicts\" (\n" +
                                "\t`rule_id`\tNUMERIC NOT NULL,\n" +
                                "\t`message_date`\tTEXT NOT NULL UNIQUE,\n" +
                                "\tPRIMARY KEY(message_date),\n" +
                                "\tFOREIGN KEY(`rule_id`) REFERENCES rules ( _id )\n" +
                                ");");
                db.execSQL("UPDATE version SET version=6");
                Log.v(LOG, "DB Updated from 5 to 6");
                // no return for further updates.
            case 6:
        }
        db.close();
        Log.d(LOG, "DATABASE UPGRADED to V6.");
    }/**/
}
