package com.khizhny.smsbanking;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.internal.zzt.TAG;
import static com.khizhny.smsbanking.MyApplication.db;

public class Transaction implements Comparable<Transaction>, java.io.Serializable  {
    private final static long serialVersionUID = 1; // Is used to indicate class version during Import/Export
    public int icon;
    private String smsBody;
    public long smsId;
    private Date transactionDate;
    private String accountCurrency;
    private String transactionCurrency;
    private BigDecimal stateBefore;
    private BigDecimal stateAfter;
    private BigDecimal stateDifference;
    private BigDecimal currencyRate;
    private BigDecimal commission;

    private String extraParam1;
    private String extraParam2;
    private String extraParam3;
    private String extraParam4;

    int selectedRuleId;        // id if transactions forced to be used by the user for this transaction.
    List <Rule> applicableRules;  // list of rules that can be used for this transaction

    public enum Parameters {
        ACCOUNT_STATE_BEFORE,
        ACCOUNT_STATE_AFTER,
        ACCOUNT_DIFFERENCE,
        COMMISSION,
        CURRENCY,
        EXTRA_1,
        EXTRA_2,
        EXTRA_3,
        EXTRA_4
    }

    public boolean hasTransactionCurrency =false;
    public boolean hasStateBefore =false;
    public boolean hasStateAfter =false;
    public boolean hasStateDifference =false;
    public boolean isCached=false;
    public boolean hasCalculatedAccountStateBefore=false;
    public boolean hasCalculatedAccountStateAfter=false;
    public boolean hasCalculatedAccountDifference=false;
    public boolean hasCalculatedTransactionDate =false;

    @Override
    public int compareTo(@NonNull Transaction o) {
        try{
            return o.getTransactionDate().compareTo(getTransactionDate());
        } catch (Exception e)  {
            return 0;
        }
    }
    public  static String removeBadChars(String s){
        return s.replace("'", "").replace("\n", " ").trim();
    }

    Transaction(String smsBody, String accountCurrency, Date transactionDate){
        this.icon=R.drawable.ic_transaction_unknown;
        selectedRuleId=-1;
        this.smsBody =removeBadChars(smsBody);
        this.transactionDate=transactionDate;
        this.accountCurrency=accountCurrency;
        this.transactionCurrency=accountCurrency;
        this.currencyRate=new BigDecimal(1).setScale(3, RoundingMode.HALF_UP);
        this.setCommission(new BigDecimal(0).setScale(2, RoundingMode.HALF_UP));
        this.stateAfter=new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
        this.stateBefore=new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
        this.stateDifference=new BigDecimal(0).setScale(2, RoundingMode.HALF_UP);
        this.applicableRules = new ArrayList <Rule>();
        this.extraParam1="";
        this.extraParam2="";
        this.extraParam3="";
        this.extraParam4="";
    }

    //=======================================================GETs============================
    public String getSmsBody() {
        return smsBody;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public String getTransactionDateAsString(String transactionDateFormat) {
        DateFormat f = new SimpleDateFormat(transactionDateFormat, Locale.ENGLISH);
        return f.format(transactionDate);
    }

    public String getAccountCurrency(){
        return accountCurrency;
    }
    public String getTransactionCurrency(){
        return transactionCurrency;
    }

    public BigDecimal getStateBefore(){
        return stateBefore.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getStateAfter(){
        return stateAfter.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getStateDifference(){
        return stateDifference.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getStateDifferenceInNativeCurrency(){
        return currencyRate.multiply(stateDifference,  MathContext.UNLIMITED).setScale(2, RoundingMode.HALF_UP);
    }
    public String getDifferenceInNativeCurrencyAsString(){
        if (hasStateDifference) {
            return currencyRate.multiply(stateDifference, MathContext.UNLIMITED).setScale(2, RoundingMode.HALF_UP).toString();
        }else{
            return "N/A";
        }
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public String getCommissionAsString(boolean hideCurrency, boolean hideZero){
        if (commission.signum()!=0 || !hideZero)
        {
            if (!hideCurrency) {
                return commission.toString()+" " +accountCurrency;
            }else{
                return commission.toString();
            }
        }else{
            return "";
        }

    }

    public String getTransactionType()
    {
        switch (icon){
            case R.drawable.ic_transaction_unknown: return "UNKNOWN";
            case R.drawable.ic_transaction_income: return "INCOME";
            case R.drawable.ic_transaction_withdraw: return "WITHDRAW";
            case R.drawable.ic_transaction_transfer_out : return "TRANSFER_IN";
            case R.drawable.ic_transaction_transfer_in : return "TRANSFER_OUT";
            case R.drawable.ic_transaction_shopping: return "PURCHASE";
            case R.drawable.ic_transaction_failed : return "FAILED";
            case R.drawable.ic_transaction_calculated: return "CALCULATED";
            default : return "unknown";
        }

    }

    public String getExtraParam1() {
        return extraParam1;
    }

    public void setExtraParam1(String extraParam1) {
        this.extraParam1 = extraParam1;
    }

    public String getExtraParam2() {
        return extraParam2;
    }

    public void setExtraParam2(String extraParam2) {
        this.extraParam2 = extraParam2;
    }

    public String getExtraParam3() {
        return extraParam3;
    }

    public void setExtraParam3(String extraParam3) {
        this.extraParam3 = extraParam3;
    }

    public String getExtraParam4() {
        return extraParam4;
    }

    public void setExtraParam4(String extraParam4) {
        this.extraParam4 = extraParam4;
    }

    public String getAccountDifferenceAsString(boolean hideCurrency,boolean inverseRate){
        // function forms a string that will represent transaction difference on screen
        if (hasStateDifference) {
            String rez = "";
            if (accountCurrency.equals(transactionCurrency) || currencyRate.equals(new BigDecimal("1.000"))) {
                // if transaction has native currency
                if (stateDifference.subtract(commission).signum() == 1) {
                    rez += "+";
                }
                rez += stateDifference.subtract(commission).toString();
                if (!hideCurrency) {
                    rez += " " + transactionCurrency;
                }
            } else {   // if transaction has foreign currency
                if (stateDifference.signum() == 1) {
                    rez += "+";
                }
                rez += stateDifference.toString() + " " + transactionCurrency;
                rez += "\n(" + getStateDifferenceInNativeCurrency();
                if (!hideCurrency) {
                    rez += " " + accountCurrency;
                }
                rez += ")";
                if (!inverseRate) {
                    rez += "\n(" + "rate" + " " + currencyRate.toString() + ")";
                } else {
                    rez += "\n(" + "rate" + " " + (new BigDecimal(1).setScale(3, RoundingMode.HALF_UP)).divide(currencyRate, RoundingMode.HALF_UP).toString() + ")";
                }

            }
            return rez;
        }else{
            return "N/A";
        }

    }

    public String getAccountStateBeforeAsString(boolean hideCurrency){
        if (hasStateBefore) {
            if (!hideCurrency) {
                return stateBefore.toString() + " " + accountCurrency;
            } else {
                return stateBefore.toString();
            }
        }else{
            return "N/A";
        }
    }

    public String getAccountStateAfterAsString(boolean hideCurrency){
        if (hasStateAfter) {
            if (!hideCurrency) {
                return stateAfter.toString()+" "+accountCurrency;
            }else{
                return stateAfter.toString();
            }
        }else{
            return "N/A";
        }
    }

    public void setCurrencyRate(BigDecimal currencyRate){
        this.currencyRate=currencyRate;
    }

    public void setCurrencyRate(String currencyRate){
        try{
            this.setCurrencyRate(new BigDecimal(currencyRate.replace(",", ".")).setScale(3, BigDecimal.ROUND_HALF_UP));
        }catch (Exception e) {
            Log.e(TAG,"Setting rate error:" + currencyRate);
        }
    }

    public BigDecimal getCurrencyRate(){
        return currencyRate;
    }

    public ContentValues getContentValues() {
        ContentValues v = new ContentValues();
        v.put("transaction_date",transactionDate.getTime());
        v.put("account_currency",accountCurrency);
        v.put("sms_body",smsBody);
        v.put("sms_id",smsId);
        v.put("icon",icon);
        v.put("transaction_currency",transactionCurrency);
        if (hasStateBefore) v.put("state_before",stateBefore.toString());
        if (hasStateAfter)v.put("state_after",stateAfter.toString());
        if (hasStateDifference) v.put("state_difference",stateDifference.toString());
        v.put("commission",commission.toString());
        v.put("extra1",extraParam1);
        v.put("extra2",extraParam2);
        v.put("extra3",extraParam3);
        v.put("extra4",extraParam4);
        v.put("exchange_rate",currencyRate.toString());
        return v;
    }


    public void setAccountCurrency(String accountCurrency) {
        this.accountCurrency = accountCurrency;
    }

    public void setTransactionCurrency(String transactionCurrency) {
        this.transactionCurrency = transactionCurrency;
        //this.hasTransactionCurrency =true;

    }
    public void setStateBefore(BigDecimal stateBefore) {
        this.stateBefore = stateBefore.setScale(2, RoundingMode.HALF_UP);
        this.hasStateBefore =true;
    }
    public void setStateAfter(BigDecimal stateAfter) {
        this.stateAfter = stateAfter.setScale(2, RoundingMode.HALF_UP);
        this.hasStateAfter =true;
    }
    public void setDifference(BigDecimal stateDifference) {
        this.stateDifference = stateDifference.setScale(2, RoundingMode.HALF_UP);
        this.hasStateDifference =true;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission.setScale(2, RoundingMode.HALF_UP);
    }

    public int setStateBefore(String s){
        try {
            setStateBefore(new BigDecimal(s.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP));
            return 1;
        }catch (Exception e) {
            return 0;
        }
    }

    public int setStateAfter(String s){
        try{
            setStateAfter(new BigDecimal(s.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP));
            return 1;
        }catch (Exception e) {
            return 0;
        }
    }

    public int setDifference(String s){
        try{
            setDifference(new BigDecimal(s.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP));
            return 1;
        }catch (Exception e) {
            return 0;
        }
    }

    public int setComission(String comission) {
        try{
            this.setCommission(new BigDecimal(comission.replace(",", ".")).setScale(2, BigDecimal.ROUND_HALF_UP));
            return 1;
        }catch (Exception e) {
            return 0;
        }
    }

    /**
     * Function will try to calculate all dependent parameters of transaction.
     */
    public void calculateMissedData(){
        // Calculating stateAfter if possible
        if (!hasStateAfter && hasStateBefore && hasStateDifference) {
            if (transactionCurrency.equals(accountCurrency)) {
                stateAfter = stateBefore.add(stateDifference).subtract(commission);
                hasStateAfter =true;
                hasCalculatedAccountStateAfter=true;
            }
        }
        // Calculating stateBefore if possible
        if (!hasStateBefore && hasStateAfter && hasStateDifference) {
            if (transactionCurrency.equals(accountCurrency)) {
                stateBefore = stateAfter.subtract(stateDifference).add(commission);
                hasStateBefore =true;
                hasCalculatedAccountStateBefore=true;
            }
        }
        // Calculating account difference if possible
        if (!hasStateDifference && hasStateBefore && hasStateAfter) {
            if (transactionCurrency.equals(accountCurrency)) {
                stateDifference = stateAfter.subtract(stateBefore).add(commission);
                hasStateDifference =true;
                hasCalculatedAccountDifference=true;
            }
        }
        // if commission changed recalculate state before
        if (hasStateDifference && hasStateBefore && hasStateAfter) {
            if (transactionCurrency.equals(accountCurrency)) {
                stateBefore = stateAfter.subtract(stateDifference).add(commission);
            }
        }
        // calculating exchange rates if it is possible.
        if (    hasStateDifference &&
                //hasTransactionCurrency &&
                hasStateBefore &&
                hasStateAfter) {
            if (!transactionCurrency.equals(accountCurrency) &&
                    stateDifference.signum() != 0) {
                // calculating price in native currency
                BigDecimal uah_price = stateBefore.subtract(stateAfter);
                // minus commission if exists
                uah_price = uah_price.add(commission);
                // exchange rate
                BigDecimal rate = uah_price.divide(stateDifference.negate(), 3, RoundingMode.HALF_UP);
                setCurrencyRate(rate);
            }
        }
    }

    /**
     * Function will add extra transactions that is missing in the sequence. It also sorts transactions by time.
     * @param transactionList List of transactions.
     * @return Sorted list with recovered transactions.
     */
    public static List<Transaction> addMissingTransactions(List<Transaction> transactionList){
        // removing duplicates
        HashSet<Transaction> se = new HashSet<Transaction>(transactionList);
        transactionList.clear();
        transactionList = new ArrayList<Transaction>(se);
        //Sorting by date
        Collections.sort(transactionList);

        // Adding virtual transanctions instead of missing sms.
        Transaction curr;
        Transaction prev;
        Transaction next;
        int transactionsCount;
        transactionsCount = transactionList.size();
        if (transactionsCount > 1) {
            for (int i = transactionsCount - 1; i >= 1; i--) {
                curr = transactionList.get(i - 1);
                prev = transactionList.get(i);
                // if there is previous transaction we restore account state if needed from/to message
                if (!curr.hasStateBefore && prev.hasStateAfter) {
                    curr.setStateBefore(prev.getStateAfter());
                    curr.hasCalculatedAccountStateBefore = true;
                }
                if (curr.hasStateBefore && !prev.hasStateAfter) {
                    prev.setStateAfter(curr.getStateBefore());
                    prev.hasCalculatedAccountStateAfter = true;
                }
                // if there is next transaction we restore account state if needed from/to next message
                if (i > 2) {
                    next = transactionList.get(i - 2);
                    if (!curr.hasStateAfter && next.hasStateBefore) {
                        curr.setStateAfter(next.getStateBefore());
                        curr.hasCalculatedAccountStateAfter = true;
                    }
                    if (curr.hasStateAfter && !next.hasStateBefore) {
                        next.setStateBefore(curr.getStateAfter());
                        next.hasCalculatedAccountStateBefore = true;
                    }
                }
                // Adding info to transactions with foreign currency. (calculating exchange rates if it is possible).
                curr.calculateMissedData();
                // adding extra transactions if account state changed unexpectedly
                if (prev.hasStateAfter && curr.hasStateBefore) {
                    if (!prev.getStateAfter().equals(curr.getStateBefore())) {
                        Transaction new_transaction = new Transaction("", prev.getAccountCurrency(),new Date((curr.getTransactionDate().getTime() + prev.getTransactionDate().getTime()) / 2));
                        new_transaction.hasCalculatedTransactionDate = true;
                        new_transaction.setAccountCurrency(prev.getAccountCurrency());
                        new_transaction.icon = R.drawable.ic_transaction_calculated;
                        new_transaction.setStateBefore(prev.getStateAfter());
                        new_transaction.setStateAfter(curr.getStateBefore());
                        new_transaction.calculateMissedData();
                        transactionList.add(new_transaction);
                    }
                }
            }
            Collections.sort(transactionList);
        }
        return transactionList;
    }



    /**
     * Function loads a list of transactions from SMS to a List
     * @param activeBank Bank object with all extraction settings and rules.
     * @param context Context
     * @return List of Transaction objects. (sorted)
     */
    public synchronized static List<Transaction> loadTransactions(Bank activeBank, Context context){
        //reading preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean hideMatchedMessages = settings.getBoolean("hide_matched_messages", false);
        Boolean hideNotMatchedMessages = settings.getBoolean("hide_not_matched_messages", false);
        Boolean ignoreClones = settings.getBoolean("ignore_clones", false);

        // Loading transactions from cache.
        List<Transaction> transactionList;
        Date lastCachedTransactionDate;
        transactionList = db.getTransactionCache(activeBank.getId());
        if (transactionList.size()>0) {
            lastCachedTransactionDate = transactionList.get(0).getTransactionDate();
        }else{
            lastCachedTransactionDate=new Date(0);
        }

        // Loading transactions from SMS.
        String smsBody="";
        String prevSmsBody="";
        String phoneNumbers = activeBank.getPhone().replace(";", "','");
        Cursor c;
        if (MyApplication.hasReadSmsPermission) {
            Uri uri = Uri.parse("content://sms/inbox");
            c = context.getContentResolver().query(uri, null, "address IN ('" + phoneNumbers + "')", null, "date DESC");
        }else {
            return transactionList;
        }
        if (c != null) {
            int msgCount = c.getCount();
            if (c.moveToFirst()) {
                for (int ii = 0; ii < msgCount; ii++) {
                    boolean skipMessage=false;
                    Date transactionDate = new Date(c.getLong(c.getColumnIndexOrThrow("date")));
                    if (!transactionDate.after(lastCachedTransactionDate)) skipMessage=true;

                    smsBody = Transaction.removeBadChars(c.getString(c.getColumnIndexOrThrow("body")));

                    if (ignoreClones && prevSmsBody.equals(smsBody)) skipMessage=true;

                    if (!skipMessage) {
                        // if sms body is duplicating previous one and ignoreClones flag is set - just skip message
                        Transaction transaction = new Transaction(smsBody,activeBank.getDefaultCurrency(),transactionDate);
                        transaction.smsId = c.getLong(c.getColumnIndexOrThrow("_id"));
                        Boolean messageHasIgnoreTypeRule = false;

                        for (Rule rule : activeBank.ruleList) {
                            if (smsBody.matches(rule.getMask())) {
                                if (rule.hasIgnoreType()) {
                                    messageHasIgnoreTypeRule = true;
                                } else {
                                    transaction.applicableRules.add(rule);
                                }
                            }
                        }

                        if (!messageHasIgnoreTypeRule) {
                            // adding transaction to the list only is it is not ignoreg by any rule
                            if ((!hideNotMatchedMessages && transaction.applicableRules.size() == 0)) {
                                // adding to list only is user set "show non matched" option in parameters
                                transaction.calculateMissedData();
                                transactionList.add(transaction);
                            }
                            if (!hideMatchedMessages && transaction.applicableRules.size() == 1) {
                                // adding to list only is user set "show matched"  option in parameters
                                transaction.applicableRules.get(0).applyRule(transaction);
                                transaction.calculateMissedData();
                                transactionList.add(transaction);
                            }
                            if (!hideMatchedMessages && transaction.applicableRules.size() >= 2) {
                                transaction.selectedRuleId= db.getRuleIdFromConflictChoices(transactionDate);
                                if (transaction.selectedRuleId >= 0) { // if user already picked rule using his choice
                                    transaction.getSelectedRule().applyRule(transaction);
                                } else { // if user did not picked rule choose any first.
                                    transaction.applicableRules.get(0).applyRule(transaction);
                                }
                                transaction.calculateMissedData();
                                transactionList.add(transaction);
                            }
                        }
                    }
                    prevSmsBody=smsBody;
                    c.moveToNext();
                }

            }
            c.close();
        }
        transactionList=Transaction.addMissingTransactions(transactionList);
        // saving last bank account state to db for later usage
        if (transactionList.size()>0){
            activeBank.setCurrentAccountState(transactionList.get(0).getStateAfter());
            db.addOrEditBank(activeBank);
        }
        return transactionList;
    }/**/

    public Rule getSelectedRule(){
        for (Rule r : applicableRules) {
            if (r.getId()==selectedRuleId) return r;
        }
        return null;
    }

    public int ruleOptionsCount(){
        return applicableRules.size();
    }

    /**
     * Changes selected rule ID by the user to next possible value.
     * Seves changes to db.
     */
    public void switchToNextRule(){
        if (applicableRules.size()<2) return;
        int originSelectedRuleId=selectedRuleId;
        for (int i = 0; i< applicableRules.size(); i++) {
            if (applicableRules.get(i).getId()==originSelectedRuleId || selectedRuleId==-1) {
                if (i+1== applicableRules.size()){
                    switchToRule(applicableRules.get(0));
                }else{
                    switchToRule(applicableRules.get(i+1));
                    break;
                }
            }
        }
    }

    public void switchToRule(Rule rule){
        selectedRuleId= rule.getId();
        db.saveRuleConflictChoice(selectedRuleId, transactionDate);
    }

    /**
     * True if Transaction has at least one extra parameter set.
     * @return true if at least one extra parameter set.
     */
    public Boolean hasExtras(){
        return (String.format("%s%s%s%s", extraParam1, extraParam2, extraParam3, extraParam4)).length()>0;
    }

    /**
     * Returns the index of transaction date in the bar chart.
     * @param startDate reference date (with index value=0)
     * @param step 1-Year,2-Quarter,3-MONTH,4-WEEK, 5-DAY
     * @return index of transaction date.
     */
    public int getDateIndex(Date startDate,int step){
        Calendar cal = Calendar.getInstance();
        cal.setMinimalDaysInFirstWeek(7);
        cal.setTime(startDate);

        int startYear=cal.get(Calendar.YEAR);
        int startWeek=cal.get(Calendar.WEEK_OF_YEAR);
        int startMonth=cal.get(Calendar.MONTH);
        cal.setTime(transactionDate);
        int diffYear=cal.get(Calendar.YEAR)-startYear;
        int diffMonth=diffYear*12+cal.get(Calendar.MONTH)-startMonth;
        long diffDay=TimeUnit.DAYS.convert(transactionDate.getTime()-startDate.getTime(), TimeUnit.MILLISECONDS);
        int diffWeek=diffYear*52+cal.get(Calendar.WEEK_OF_YEAR)-startWeek;
        //int diffWeek=(int) diffDay/7;
        int diffQuater = diffMonth/3;
        switch (step){
            case 1: //Year
                return diffYear;
            //break;
            case 2: //Quater
                return diffQuater;
            //break;
            case 3: //Month
                return diffMonth;
            //break;
            case 4: //Week
                return diffWeek;
            //break;
            case 5: //Day
                //Log.d("DateCheck","getDateIndex startDate="+startDate+"  transactionDate="+transactionDate+ " index="+diffDay);
                return (int) diffDay;
            //break;
            default:
                return 0;
        }
    }
    /**
     * @param transactionList List of transactions sorted desc by time.
     * @return Last known account state.
     */
    public static BigDecimal getLastAccountState(List<Transaction> transactionList) {
        for (Transaction t : transactionList) {
            if (t.hasStateAfter) return t.getStateAfter();
            if (t.hasStateBefore) return t.getStateBefore();
        }
        return new BigDecimal("0.00");
    }
}
