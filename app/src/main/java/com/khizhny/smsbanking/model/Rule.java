package com.khizhny.smsbanking.model;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.khizhny.smsbanking.R;

import java.util.ArrayList;
import java.util.List;

import static com.khizhny.smsbanking.MyApplication.db;

public class Rule implements java.io.Serializable {

	private static final long serialVersionUID = 1; // Is used to indicate class version during Import/Export
	private final static String LOG ="SMS_BANKING";

	private int id;
	private int bankId;
	private String name;
	private String smsBody;
	private String mask;
    private String nameSuggestion;
	private transactionType ruleType;
	public int wordsCount;
	public boolean[] wordIsSelected;
	public List<SubRule> subRuleList;
	public List<Word> words=new ArrayList<Word>();

	/**
	 * Transaction type icons array.
	 */
	public static int[] ruleTypeIcons ={
			R.drawable.ic_transaction_unknown,
			R.drawable.ic_transaction_income,
			R.drawable.ic_transaction_withdraw,
			R.drawable.ic_transaction_transfer_out,
			R.drawable.ic_transaction_transfer_in,
			R.drawable.ic_transaction_shopping,
			R.drawable.ic_transaction_failed,
			R.drawable.ic_transaction_calculated, // Calculated
			R.drawable.ic_transaction_ignore // ignore
	};
	public enum transactionType {
		UNKNOWN,
		INCOME,
		WITHDRAW,
		TRANSFER_IN,
		TRANSFER_OUT,
		PURCHASE,
		FAILED,
		CALCULATED,
		IGNORE
	}

	/**
	 * New rule constructor.
	 * @param bankId Bank ID.
	 * @param name Name of the rule
	 */
    public Rule(int bankId, String name){
		this.id=-1;
		this.bankId=bankId;
		this.name=name;
        this.nameSuggestion=name;
		this.smsBody="";
		this.mask="";
        this.ruleType=transactionType.UNKNOWN;
        this.wordsCount=0;
        this.wordIsSelected=null;
        this.subRuleList=null;
        this.subRuleList=new ArrayList<SubRule>();
	}

	/**
	 * Constructor is used for cloning bank object with all Rules and subrules
	 * @param bankId Bank ID
	 * @param rule Rule
	 */
    public Rule(Rule rule, int bankId){
		this.id=-1;
		this.bankId=bankId;
		this.name=rule.name;
        this.nameSuggestion=rule.name;
		this.smsBody=rule.smsBody;
		this.mask=rule.mask;
		this.ruleType=rule.ruleType;
		this.wordsCount=rule.wordsCount;
		this.wordIsSelected=null;
		for (SubRule sr : rule.subRuleList) {
			this.subRuleList.add(new SubRule(sr, -1));
		}
		Log.d(LOG, "Rule " +rule + " was cloned");
	}

	public String toString(){
		return name;
	}

    public int getId() {
        return id;
    }
    public int getBankId() {
        return bankId;
    }

	public void setId(int id) {
		this.id = id;
	}

	public String getSmsBody() {
		return smsBody;
	}

	public void setSmsBody(String smsBody) {
		this.smsBody = smsBody;
		wordsCount=smsBody.split(" ").length;
		wordIsSelected =new boolean[wordsCount+2]; // adding 2 words for <BEGIN> and <END>
		for (int i=1; i<=wordsCount;i++) {
			wordIsSelected[i]=false;
		}
		wordIsSelected[0]=true; // words <BEGIN> and <END> is always selected
		wordIsSelected[wordsCount+1]=true;
	}

	public String getMask() {
		return mask;
	}

	public void setMask(String mask) {
		this.mask = mask;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSelectedWords() {
		String r="";
		String delimiter="";
		for (int i=0; i<=wordsCount+1;i++) {
			if (wordIsSelected[i]) {
				r+=delimiter+i;
				delimiter=",";
			}
		}
		return r;
	}

    public String getRuleNameSuggestion() {
        return nameSuggestion;
    }

	public void setSelectedWords(String selectedWords) {
		// function sets selected words flags from string.
		String[] a = selectedWords.split(",");
		// setting all to false
		for (int i=0; i<=wordsCount+1;i++) {
			wordIsSelected[i]=false;
		}
		// setting selected to true
		int k;
		for (String w : a) {
			k = Integer.parseInt(w);
			wordIsSelected[k] = true;
		}
		updateMask();
	}

    /**
     * Returns a Transaction example based on origin sms used to create rule and
     * all of the subrules within the rule.
     * @return Transaction object
     */
	public Transaction getSampleTransaction(Context ctx){
        Bank bank=db.getBank(bankId);
        Transaction transaction = new Transaction(smsBody,bank.getDefaultCurrency(),null);
        applyRule(transaction);
        transaction.calculateMissedData();
        return  transaction;
    }

	public void selectWord(int WordIndex){
		wordIsSelected[WordIndex]=true;
		updateMask();
	}

	public void deSelectWord(int WordIndex){
		wordIsSelected[WordIndex]=false;
		updateMask();
	}

	/**
	 * Function updates sms mask (used to match SMS and Rules) after user change constant words.
	 */
	private void updateMask(){
		mask="";
        nameSuggestion="";
		String delimiter="";
		String[] words=smsBody.split(" ");
		if (!smsBody.trim().equals(smsBody)) mask= ".*";
        boolean skip_wildcard=false;
		for (int i=1; i<=wordsCount; i++){
			if (wordIsSelected[i]){
				mask+=delimiter+"\\Q"+words[i-1]+"\\E";
                nameSuggestion+=delimiter+words[i-1];
				skip_wildcard=false;
			}else{
				if (!skip_wildcard) {
					mask+=delimiter+".*";
					skip_wildcard=true;
				}				
			}
			delimiter=" ";
		}
        nameSuggestion.trim();
        if (!smsBody.trim().equals(smsBody)) mask+= ".*";
	}


	public int getRuleTypeInt() {
		return ruleType.ordinal();
	}

    public transactionType getRuleType() {
        return ruleType;
    }

    /**
	 *
	 * @return Drawable ID of the icon to be shown in transaction list.
	 */
	public int getRuleTypeDrawable() {
		return ruleTypeIcons[ruleType.ordinal()];
	}

	public void setRuleType(int ruleType) {
		this.ruleType = transactionType.values()[ruleType];
	}

	public List<String> getConstantPhrases(){
		List<String> out = new ArrayList<String>();
		out.add("<BEGIN>");
		int phraseCount=1;
		boolean startNewPhrase=true;
		String[] words=smsBody.split(" ");
		for (int i=1; i<=wordsCount; i++){
			if (wordIsSelected[i]){
				if (startNewPhrase) {
					phraseCount+=1;
					out.add(words[i-1]);
					startNewPhrase=false;
				} else
				{
					out.set(phraseCount-1, out.get(phraseCount-1)+" "+(words[i-1]));
				}
				
			}else{
				startNewPhrase=true;
			}
		}
		out.add("<END>");
		return out;
	}

	/**
	 *
	 * @return Content values used for database insert or update function.
	 */
	public ContentValues getContentValues(){
		ContentValues v = new ContentValues();
		if (id>=1) v.put("_id",id);
		v.put("name", name);
		v.put("sms_body", smsBody);
		v.put("mask", mask);
		v.put("selected_words", getSelectedWords());
		v.put("bank_id", bankId);
		v.put("type", ruleType.ordinal());
		return v;
	}

	/**
	 * Function is used to copy bank templates to MyBanks.
	 * @param bankId Db id of the Bank record.
	 */
	public void changeBankId(int bankId){
		this.bankId=bankId;
	}

    /**
     * Apply current Rule to Transaction.
     * @param transaction Transaction on which rule will be applied.
     * @return TRUE if Rule was applied. False if rule can not be applied.
     */
	public Boolean applyRule(Transaction transaction ){
		if (transaction!=null) {
            String sms_body = transaction.getSmsBody();
            if (ruleType == Rule.transactionType.IGNORE || !sms_body.matches(mask)) {
                return false;  //  rule is not for this SMS.
            } else {
                transaction.icon = getRuleTypeDrawable();
                for (SubRule subRule : subRuleList) subRule.applySubRule(sms_body, transaction);
                return true;
            }
        } else {
            return false;
        }
	}
	public Boolean hasIgnoreType(){
		return ruleType == Rule.transactionType.IGNORE;
	}

    /**
     * Function gets existing subrule for parameter or creates a new one for it.
     * @param transactionParameter
     * @return
     */
	public SubRule getOrCreateSubRule(Transaction.Parameters transactionParameter){
        // looking for existing subrule.
        for (SubRule sr: subRuleList) {
            if (sr.getExtractedParameter()==transactionParameter){
                return sr;
            }
        }
        // if no luck - creating it.
        SubRule sr = new SubRule(id,transactionParameter);
        subRuleList.add(sr);
        return sr;
    }

	public void makeInitialWordSplitting(){

	}
}

