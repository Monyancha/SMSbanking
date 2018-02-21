package com.khizhny.smsbanking.model;

import android.content.ContentValues;

public class Word implements java.io.Serializable {

    public enum WORD_TYPES {
        WORD_CONST,
        WORD_VARIABLE,
        WORD_VARIABLE_FIXED_SIZE

    }
    private static final long serialVersionUID = 3; // Is used to indicate class version during Import/Export

    private int firstLetterIndex;    // index of word letter in Rule message
    private int lastLetterIndex;     // index of 1-st letter in Rule message
    private WORD_TYPES word_type;     // 0-const, 1- variable

    private String body;             // Word between [ firstLetterIndex,lastLetterIndex]

    public Rule rule;           // backRef to Rule

    public Word(Rule rule, int firstLetterIndex, int lastLetterIndex, WORD_TYPES word_type){
        this.rule=rule;
        this.word_type=word_type;
        this.firstLetterIndex=firstLetterIndex;
        this.lastLetterIndex=lastLetterIndex;
        try {
        		this.body=rule.getSmsBody().substring(firstLetterIndex,lastLetterIndex+1);
				}catch (Exception e) {
						this.body="";
				}
    }

    /**
     * Constructor for clonong Words from rule templates
     * @param originWord - word from template
     * @param rule - new Rule to bind word.
     */
    public Word(Word originWord, Rule rule){
        this.rule=rule;
        this.word_type=originWord.word_type;
        this.firstLetterIndex=originWord.firstLetterIndex;
        this.lastLetterIndex=originWord.lastLetterIndex;
        this.body=originWord.body;
    }

    public ContentValues getContentValues(){
        ContentValues v = new ContentValues();
        v.put("rule_id", rule.getId());
        v.put("first_letter_index", firstLetterIndex);
        v.put("last_letter_index",lastLetterIndex);
        v.put("word_type", word_type.ordinal());        return v;
    }

    public int getFirstLetterIndex(){
        return firstLetterIndex;
    }

    public int getLastLetterIndex(){
        return lastLetterIndex;
    }

		public String getBody(){
				return body;
		}

			public void setBody(String body){
				this.body=body;
		}

		public void changeWordType(){
        word_type=WORD_TYPES.values()[(word_type.ordinal()+1) % WORD_TYPES.values().length ];
    }

    public WORD_TYPES getWordType(){
        return word_type;
    }

    public void reAssign(int firstLetterIndex, int lastLetterIndex){
        this.firstLetterIndex=firstLetterIndex;
        this.lastLetterIndex=lastLetterIndex;
        this.body=rule.getSmsBody().substring(firstLetterIndex,lastLetterIndex+1);
    }

    public String getImpersonalizedBody(){
				if (word_type!= Word.WORD_TYPES.WORD_CONST) {
						String res = body;
						res = res.replaceAll("[0-9]", "0");
						res = res.replaceAll("[A-Z]", "A");
						res = res.replaceAll("[a-z]", "a");
						res = res.replaceAll("[а-я]", "А");
						res = res.replaceAll("[А-Я]", "Я");
						return res;
				} else{
						return body;
				}
		}
}
