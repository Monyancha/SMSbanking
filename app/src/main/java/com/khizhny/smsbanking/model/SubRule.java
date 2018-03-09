package com.khizhny.smsbanking.model;

import android.content.ContentValues;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.khizhny.smsbanking.MyApplication.LOG;

public class SubRule implements java.io.Serializable {
		private final static long serialVersionUID = 3; // Is used to indicate class version during Import/Export
		private int id=-1;
		private final Rule rule; // back reference to rule
		private int distanceToLeftPhrase=1;
		private int distanceToRightPhrase=1;
		private String leftPhrase="";
		private String rightPhrase="";
		private String constantValue="";
		private final Transaction.Parameters extractedParameter;
		private Method extractionMethod = Method.USE_REGEX;
		private DECIMAL_SEPARATOR decimalSeparator=DECIMAL_SEPARATOR.SEPARATOR_AUTO;  // 0 - dot,  1-coma, 2 - auto
		public int trimLeft=0;
		public int trimRight=0;
		public int regexPhraseIndex=0;
		public boolean negate=false;

    public enum Method {
		WORD_AFTER_PHRASE,
		WORD_BEFORE_PHRASE,
		WORDS_BETWEEN_PHRASES,
		USE_CONSTANT,
		USE_REGEX
	}

	public enum DECIMAL_SEPARATOR {
		SEPARATOR_DOT,
		SEPARATOR_COMA,
		SEPARATOR_AUTO,
	}

		public SubRule(Rule rule, Transaction.Parameters extractedParameter) {
				this.rule = rule;
				rule.subRuleList.add(this);
				this.extractedParameter = extractedParameter;
		}

		SubRule(SubRule origin, Rule rule) {
				this.rule = rule;
				rule.subRuleList.add(this);
				this.distanceToLeftPhrase = origin.distanceToLeftPhrase;
				this.distanceToRightPhrase = origin.distanceToRightPhrase;
				this.leftPhrase=origin.leftPhrase;
				this.rightPhrase=origin.rightPhrase;
				this.constantValue=origin.constantValue;
				this.extractedParameter = origin.extractedParameter;
				this.extractionMethod = origin.extractionMethod;
				this.decimalSeparator = origin.decimalSeparator;
				this.trimLeft = origin.trimLeft;
				this.trimRight = origin.trimRight;
				regexPhraseIndex = origin.regexPhraseIndex;
				this.negate = origin.negate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    public void setDistanceToLeftPhrase(int distanceToLeftPhrase) {
		this.distanceToLeftPhrase = distanceToLeftPhrase;
	}

    public void setDistanceToRightPhrase(int distanceToRightPhrase) {
		this.distanceToRightPhrase = distanceToRightPhrase;
	}

	public void setLeftPhrase(String leftPhrase) {
		this.leftPhrase = leftPhrase;
	}

    public  void setRightPhrase(String rightPhrase) {
		this.rightPhrase = rightPhrase;
	}

    private int getExtractionMethodInt() {
		return extractionMethod.ordinal();
	}

    public Method getExtractionMethod() {
		return extractionMethod;
	}

    public void setExtractionMethod(int extractionMethod) {
		this.extractionMethod = Method.values()[extractionMethod];
	}
	public void setExtractionMethod(Method method) {
		this.extractionMethod = method;
	}

    public String getConstantValue() {
		return constantValue;
	}

    public void setConstantValue(String constantValue) {
		this.constantValue = constantValue;
	}

    private int getExtractedParameterInt() {
		return extractedParameter.ordinal();
	}

    public Transaction.Parameters getExtractedParameter() {
		return extractedParameter;
	}

	public DECIMAL_SEPARATOR getDecimalSeparator() {
		return decimalSeparator;
	}

	private String getDecimalSeparatorString(String word) {
    	switch (decimalSeparator){
			case SEPARATOR_DOT:
				return ".";
			case SEPARATOR_COMA:
				return ",";
			case SEPARATOR_AUTO: // first dot or coma from right
				for (int i=word.length()-1; i>=0; i--){
					if (word.charAt(i)=='.')return ".";
					if (word.charAt(i)==',')return ",";
					}
				return ".";
			default:
				return ".";
		}
	}

	public void setDecimalSeparator(DECIMAL_SEPARATOR decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	/**
	 * These Function will try to apply rule to SMS message.
	 * If returnedType=0 result will be Numeric.
	 * If returnedType=1 result will be Alphabetical.
	 * If returnedType>1 result will be String AS-IS. Unchanged after extraction.
	 *
	 * @param smsMsg          SMS message text
	 * @param returnedType Zero for decimal, One for Alphabetical, Other for unchanged string.
	 * @return Parameter value
	 */
    public static String applySubRule(SubRule sr, String smsMsg, int returnedType) {
		String msg = "<BEGIN> " + smsMsg + " <END>";
		StringBuilder temp = new StringBuilder();
		try {
            String[] arr;
            int wordsCount;
			switch (sr.extractionMethod) {
				case WORD_AFTER_PHRASE:
					temp = new StringBuilder(msg.split(String.format("\\Q%s\\E", sr.leftPhrase))[1].split(" ")[sr.distanceToLeftPhrase]);
					break;
				case WORD_BEFORE_PHRASE:
					temp = new StringBuilder(msg.split(String.format("\\Q%s\\E", sr.rightPhrase))[0].trim());
					arr = temp.toString().split(" ");
					wordsCount = arr.length;
					temp = new StringBuilder((arr[wordsCount - sr.distanceToRightPhrase]));
					break;
				case WORDS_BETWEEN_PHRASES:
					// temp will store all words between phrases "leftPhrase" and "rightPhrase"
                    arr=msg.split(String.format("\\Q%s\\E", sr.leftPhrase));
                    if (arr.length>1) {
                        temp = new StringBuilder(arr[1]);
                        arr = temp.toString().split(String.format("\\Q%s\\E", sr.rightPhrase));
                        if (arr.length>=1) {
                            temp = new StringBuilder(arr[0].trim());
                        }else{
                            temp = new StringBuilder();
                        }
                    }else{
                        temp = new StringBuilder();
                    }
                    // Now we will just keep phrase starting from N-th word from the left till M-th word wrom the right.
                    // where N - distanceToLeftPhrase
                    // M - distanceToRightPhrase
                    arr = temp.toString().split(" ");
                    temp = new StringBuilder();
                    for (int j=sr.distanceToLeftPhrase-1; j<arr.length-sr.distanceToRightPhrase+1;j++) {
                        if (j > sr.distanceToLeftPhrase - 1) temp.append(" ");
                        temp.append(arr[j]);
                    }
                    break;
				case USE_CONSTANT:
					temp = new StringBuilder(sr.constantValue);
					break;
				case USE_REGEX:
                    Pattern pattern = Pattern.compile(sr.rule.getMask());
                    Matcher matcher = pattern.matcher(smsMsg);
                    if (matcher.matches()) {
                        if (matcher.groupCount() > sr.regexPhraseIndex) {
                        	temp = new StringBuilder(matcher.group(sr.regexPhraseIndex + 1));
						}
                    }
					break;
			}
		} catch (Exception e) {
			return "";
		}
		// trimming characters if needed
		if (sr.trimRight > 0 || sr.trimLeft > 0) {
			int temp_len = temp.length();
			try {
				temp = new StringBuilder(temp.substring(sr.trimLeft, temp_len - sr.trimRight));
			} catch (Exception e) {
				return "";
			}
		}

		switch (returnedType) {
			case 0: // return only Numbers
				try {
					// changing sign if needed
					if (sr.negate) {
						if (!temp.toString().contains("-")) {
							temp.insert(0, "-");
						} else {
							temp = new StringBuilder(temp.toString().replace("-", ""));
						}
					}
					temp = new StringBuilder(temp.toString().replaceAll("[^0-9" + sr.getDecimalSeparatorString(temp.toString()) + "-]", ""));
				} catch (Exception e) {
                    temp = new StringBuilder("0");
				}
                break;
			case 1: // return only text
                temp = new StringBuilder(temp.toString().replaceAll("[^A-Za-z]", ""));
                break;
			default: // return as-is.
                break;
		}
        return temp.toString();
	}

	/**
	 * This function will apply extraction subrule on SMS message and save extracted value to Transaction object
	 *
	 * @param msg         SMS Message
	 * @param transaction Transaction
	 */
    static void applySubRule(SubRule sr, String msg, Transaction transaction) {
		switch (sr.extractedParameter) {
			case ACCOUNT_STATE_BEFORE: //Account state before transaction
				transaction.setStateBefore(applySubRule(sr, msg, 0));
					return;
			case ACCOUNT_STATE_AFTER: //Account state after transaction
				transaction.setStateAfter(applySubRule(sr, msg, 0));
					return;
			case ACCOUNT_DIFFERENCE: //Account difference
				transaction.setDifference(applySubRule(sr, msg, 0));
					return;
			case COMMISSION: //Transaction commission
				transaction.setComission(applySubRule(sr, msg, 0));
					return;
			case CURRENCY: //Transaction currency
				transaction.setTransactionCurrency(applySubRule(sr, msg, 1)); // return only text)
					return;
			case EXTRA_1:
				transaction.setExtraParam1(applySubRule(sr, msg, 2));
					return;
			case EXTRA_2:
				transaction.setExtraParam2(applySubRule(sr, msg, 2));
					return;
			case EXTRA_3:
				transaction.setExtraParam3(applySubRule(sr, msg, 2));
					return;
			case EXTRA_4:
				transaction.setExtraParam4(applySubRule(sr, msg, 2));
					return;
			default:
				Log.d(LOG, "Unexpected parameter number " + sr.extractedParameter + " in Rule ID=" + sr.rule.getId());
		}

	}

	/**
	 * @return Content values used for database insert or update function.
	 */
    public ContentValues getContentValues() {
		ContentValues v = new ContentValues();
		if (id >= 1) v.put("_id", id);
		v.put("rule_id", rule.getId());
		v.put("left_phrase", leftPhrase);
		v.put("right_phrase", rightPhrase);
		v.put("distance_to_left_phrase", distanceToLeftPhrase);
		v.put("distance_to_right_phrase", distanceToRightPhrase);
		v.put("constant_value", constantValue);
		v.put("extracted_parameter", getExtractedParameterInt());
		v.put("extraction_method", getExtractionMethodInt());
		v.put("decimal_separator", decimalSeparator.ordinal());
		v.put("trim_left", trimLeft);
		v.put("trim_right", trimRight);
		v.put("regex_phrase_index", regexPhraseIndex);
		v.put("negate", negate ? -1 : 0);
		return v;
	}
}