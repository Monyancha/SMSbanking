package com.khizhny.smsbanking.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class MyPostsFragment extends PostListFragment {

    public MyPostsFragment() {}

    @Override
    public Query getQuery(DatabaseReference databaseReference) {
        // todo  All my posts
        return databaseReference.child("user-posts")
                .child(getUid())
                .orderByChild("starCount");
    }
}
