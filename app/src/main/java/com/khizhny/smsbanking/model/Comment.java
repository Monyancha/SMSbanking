package com.khizhny.smsbanking.model;

public class Comment {

    public String uid;
    public String author;
    public String text;
    public long timestamp;

    public Comment() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Comment(String uid, String author, String text, long timestamp) {
        this.uid = uid;
        this.author = author;
        this.text = text;
        this.timestamp = timestamp;
    }

}