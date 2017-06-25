package com.khizhny.smsbanking.viewholder;

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

    public TextView titleView;
    public TextView authorView;
    public TextView dateView;
    public ImageView likeView;
    public ImageView dislikeView;
    public TextView numStarsView;
    public ImageButton deleteView;
    public ImageButton downloadView;

    public PostViewHolder(View itemView) {
        super(itemView);
        dateView = (TextView) itemView.findViewById(R.id.post_date);
        titleView = (TextView) itemView.findViewById(R.id.post_title);
        authorView = (TextView) itemView.findViewById(R.id.post_author);
        likeView = (ImageView) itemView.findViewById(R.id.like);
        dislikeView = (ImageView) itemView.findViewById(R.id.dislike);
        numStarsView = (TextView) itemView.findViewById(R.id.post_num_stars);
        deleteView = (ImageButton) itemView.findViewById(R.id.delete_post);
        downloadView = (ImageButton) itemView.findViewById(R.id.download_post);
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

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(date);
    }
}
