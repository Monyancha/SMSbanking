package com.khizhny.smsbanking.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.khizhny.smsbanking.model.Bank;

public class MyCountryPostsFragment extends PostListFragment {

    public MyCountryPostsFragment() {

    }

    @Override
    public Query getQuery(DatabaseReference databaseReference) {

        return databaseReference.child("posts V"+ Bank.serialVersionUID)
                .child(country)
                .orderByChild("starCount");
    }
}
