package com.khizhny.smsbanking.model;

import android.content.ContentValues;
import android.graphics.Color;

public class Word implements java.io.Serializable {

    public enum WORD_TYPES {
        WORD_CONST,
        WORD_VARIABLE
    }
    private static final long serialVersionUID = 1; // Is used to indicate class version during Import/Export

    private int firstLetterIndex;    // index of word letter in Rule message
    private int lastLetterIndex;     // index of 1-st letter in Rule message
    private WORD_TYPES word_type;     // 0-const, 1- variable

    private String body;             // Word between [ firstLetterIndex,lastLetterIndex]

    public Rule rule;           // backref to Rule

    public Word(Rule rule, int firstLetterIndex, int lastLetterIndex, WORD_TYPES word_type){
        this.rule=rule;
        this.word_type=word_type;
        this.firstLetterIndex=firstLetterIndex;
        this.lastLetterIndex=lastLetterIndex;
        this.body=rule.getSmsBody().substring(firstLetterIndex,lastLetterIndex+1);
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
        return firstLetterIndex;
    }

    public String getBody(){
        return body;
    }

    public void changeWordType(){
        word_type=WORD_TYPES.values()[(word_type.ordinal()+1) % WORD_TYPES.values().length ];
    }

    public WORD_TYPES getWordType(){
        return word_type;
    }
    public int getWordColor(){

        switch (word_type){
            case WORD_CONST:
                return Color.LTGRAY;
            case WORD_VARIABLE:
                return Color.GRAY;
            default:
                return Color.BLACK;
        }
    }

}
