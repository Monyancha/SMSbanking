package com.khizhny.smsbanking.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.MyApplication;

public class MyCountryPostsFragment extends PostListFragment {

    public MyCountryPostsFragment() {

    }

    @Override
    public Query getQuery(DatabaseReference databaseReference) {

        Query recentPostsQuery = databaseReference.child("posts V"+ Bank.serialVersionUID)
                .child(country)
                .orderByChild("starCount");

        return recentPostsQuery;
    }
}
