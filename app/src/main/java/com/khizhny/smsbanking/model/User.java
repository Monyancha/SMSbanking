package com.khizhny.smsbanking.model;


import com.google.firebase.database.IgnoreExtraProperties;

@SuppressWarnings("unused")
@IgnoreExtraProperties
public class User {

		private String username;
		private String email;
    public String photoUri; // 96x96 Photo link

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

}