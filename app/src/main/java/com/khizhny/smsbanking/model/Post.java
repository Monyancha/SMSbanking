package com.khizhny.smsbanking.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Post {

    public String uid;
    public String author;
    public String title;
    public String currency;
    public String url;
    public long timestamp;
    public int starCount = 0;
    public Map<String, Integer> stars = new HashMap<String,Integer>();

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String uid,
                String author,
                String title,
                String url,
                String currency,
                long timestamp) {
        this.uid = uid;
        this.author = author;
        this.title = title;
        this.url = url;
        this.currency=currency;
        this.timestamp=timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("uid", uid);
        result.put("author", author);
        result.put("title", title);
        result.put("url", url);
        result.put("starCount", starCount);
        result.put("stars", stars);
        result.put("currency", currency);
        result.put("timestamp", timestamp);
        return result;
    }

}