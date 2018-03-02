package com.khizhny.smsbanking.viewholder;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.khizhny.smsbanking.R;
import com.khizhny.smsbanking.model.Post;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostViewHolder extends RecyclerView.ViewHolder {

    private final TextView titleView;
    private final TextView authorView;
    private final TextView dateView;
    public final ImageView likeView;
    public final ImageView dislikeView;
    private final TextView numStarsView;
    public final ImageButton deleteView;
    private final ImageButton downloadView;

    public PostViewHolder(View itemView) {
        super(itemView);
        dateView = itemView.findViewById(R.id.post_date);
        titleView = itemView.findViewById(R.id.post_title);
        authorView = itemView.findViewById(R.id.post_author);
        likeView = itemView.findViewById(R.id.like);
        dislikeView = itemView.findViewById(R.id.dislike);
        numStarsView = itemView.findViewById(R.id.post_num_stars);
        deleteView = itemView.findViewById(R.id.delete_post);
        downloadView = itemView.findViewById(R.id.download_post);
    }

    public void bindToPost(Post post, View.OnClickListener clickListener) {
        dateView.setText(convertTime(post.timestamp));
        titleView.setText(post.title);
        authorView.setText(post.author);
        numStarsView.setText(String.valueOf(post.starCount));
        likeView.setOnClickListener(clickListener);
        dislikeView.setOnClickListener(clickListener);
        deleteView.setOnClickListener(clickListener);
        downloadView.setOnClickListener(clickListener);
    }

    private static String convertTime(long time){
        Date date = new Date(time);
        @SuppressLint("SimpleDateFormat") Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(date);
    }
}
