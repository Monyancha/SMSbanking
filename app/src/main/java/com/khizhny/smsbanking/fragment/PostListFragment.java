package com.khizhny.smsbanking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.khizhny.smsbanking.PostDetailActivity;
import com.khizhny.smsbanking.PostsActivity;
import com.khizhny.smsbanking.R;
import com.khizhny.smsbanking.gcm.MyDownloadService;
import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Post;
import com.khizhny.smsbanking.viewholder.PostViewHolder;

public abstract class PostListFragment extends Fragment {

    private static final String TAG = "PostListFragment";
    private static final int POST_DELETE_THRESHOLD=-10;

    public String country;
    private DatabaseReference mDatabase;

    private FirebaseRecyclerAdapter<Post, PostViewHolder> mAdapter;
    private RecyclerView mRecycler;
    private LinearLayoutManager mManager;

    public PostListFragment() {}

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_all_posts, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mRecycler = (RecyclerView) rootView.findViewById(R.id.messages_list);
        mRecycler.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up Layout Manager, reverse layout
        mManager = new LinearLayoutManager(getActivity());
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);

        //Reading country
         country=getArguments().get("country").toString();

        // Set up FirebaseRecyclerAdapter with the Query
        Query postsQuery = getQuery(mDatabase);
        mAdapter = new MyFirebaseRecyclerAdapter(Post.class, R.layout.item_post, PostViewHolder.class, postsQuery);
        mRecycler.setAdapter(mAdapter);
    }


    private void onLikeClicked(DatabaseReference postRef) {
        postRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Post post = mutableData.getValue(Post.class);
                if (post == null) {
                    return Transaction.success(mutableData);
                }
                String userId=getUid();

                if (post.stars.containsKey(userId)) {
                    post.starCount-=post.stars.get(userId);
                    post.stars.remove(userId);
                }else{
                    post.stars.put(userId,1);
                    post.starCount++;
                }
                // Set value and report transaction success
                mutableData.setValue(post);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
            }
        });
    }

     private void startDownload(String path) {
         // Kick off MyDownloadService to download the file
         Intent intent = new Intent(getActivity(), MyDownloadService.class)
                 .putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path)
                 .setAction(MyDownloadService.ACTION_DOWNLOAD);
         getActivity().startService(intent);
     }

     private void onDislikeClicked(DatabaseReference postRef) {
         postRef.runTransaction(new Transaction.Handler() {

             @Override
             public Transaction.Result doTransaction(MutableData mutableData) {
                 Post post = mutableData.getValue(Post.class);
                 if (post == null) {
                     return Transaction.success(mutableData);
                 }

                 String userId=getUid();
                 if (post.stars.containsKey(userId)) {
                     post.starCount-=post.stars.get(userId);
                     post.stars.remove(userId);
                 }else{
                     post.stars.put(userId,-1);
                     post.starCount--;
                 }

                 // Set value and report transaction success
                 mutableData.setValue(post);
                 return Transaction.success(mutableData);
             }

             @Override
             public void onComplete(DatabaseError databaseError, boolean b,
                                    DataSnapshot dataSnapshot) {
                 // Transaction completed
                 Log.d(TAG, "postTransaction:onComplete:" + databaseError);
             }
         });
     }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    public String getUid() {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null) {
            return user.getUid();
        }else{
            return null;
        }

    }

    public abstract Query getQuery(DatabaseReference databaseReference);

    private class MyFirebaseRecyclerAdapter extends FirebaseRecyclerAdapter<Post, PostViewHolder> {

        public MyFirebaseRecyclerAdapter(Class<Post> modelClass, int modelLayout, Class<PostViewHolder> viewHolderClass, Query ref) {
            super(modelClass, modelLayout, viewHolderClass, ref);
            ((PostsActivity)getActivity()).showProgress(true);
        }


        @Override
        protected void populateViewHolder(final PostViewHolder viewHolder, final Post post, final int position) {
            final DatabaseReference postRef = getRef(position);
            ((PostsActivity)getActivity()).showProgress(false);

            // Set click listener for the whole post view
            final String postKey = postRef.getKey();
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Launch PostDetailActivity
                    Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                    intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, postKey);
                    startActivity(intent);
                }
            });


            if (post.starCount<POST_DELETE_THRESHOLD || getUid().equals(post.uid)) {
                // only author can delete posts or anyone if post has bad reputation
                viewHolder.deleteView.setVisibility(View.VISIBLE);
            } else {
                // hiding delete button if post is not that bad
                viewHolder.deleteView.setVisibility(View.GONE);
            }

            // Determine if the current user has liked this post and set UI accordingly
            if (post.stars.containsKey(getUid())) {
                int score=post.stars.get(getUid());
                if (score>0) {
                       viewHolder.likeView.setImageResource(R.drawable.ic_thumb_up_green);
                       viewHolder.dislikeView.setImageResource(R.drawable.ic_thumb_down_grey);
                } else if (score <0){
                        viewHolder.likeView.setImageResource(R.drawable.ic_thumb_up_grey);
                        viewHolder.dislikeView.setImageResource(R.drawable.ic_thumb_down_red);
                } else { //=0
                        viewHolder.likeView.setImageResource(R.drawable.ic_thumb_up_grey);
                        viewHolder.dislikeView.setImageResource(R.drawable.ic_thumb_down_grey);
                }
            } else {
                viewHolder.likeView.setImageResource(R.drawable.ic_thumb_up_grey);
                viewHolder.dislikeView.setImageResource(R.drawable.ic_thumb_down_grey);
            }


            // Bind Post to ViewHolder, setting OnClickListener for the like/dislike buttons
            viewHolder.bindToPost(post, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Need to write to both places the post is stored
                    DatabaseReference countryPostRef = mDatabase
                            .child("posts V" + Bank.serialVersionUID)
                            .child(country)
                            .child(postRef.getKey());
                    DatabaseReference userPostRef = mDatabase.child("user-posts")
                            .child(post.uid)
                            .child(postRef.getKey());
                    DatabaseReference postComments = mDatabase.child("post-comments")
                            .child(postRef.getKey());

                    String[] arr=post.url.split("/");
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(arr[0])
                            .child(arr[1])
                            .child(arr[2]);

                    switch (view.getId()){
                        case R.id.like:// Run two transactions
                            onLikeClicked(countryPostRef);
                            onLikeClicked(userPostRef);
                            break;
                        case R.id.dislike: // Run two transactions
                            onDislikeClicked(countryPostRef);
                            onDislikeClicked(userPostRef);
                            break;
                        case R.id.delete_post:
                            storageRef.delete();// remove dat file
                            postComments.removeValue();  // remove comments
                            countryPostRef.removeValue(); // remove county-post
                            userPostRef.removeValue(); // remove user-post
                            break;
                        case R.id.download_post:
                            // Get path
                            startDownload(post.url);
                            break;
                    }


                }


            });

        }
    }

    public String getUserName() {
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo i : user.getProviderData()){
                if (i.getDisplayName()!=null) {
                    return i.getDisplayName();
                }
            }
        }
        return getString(R.string.anonymous);
    }

}
