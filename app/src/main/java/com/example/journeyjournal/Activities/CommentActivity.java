package com.example.journeyjournal.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.journeyjournal.Adapters.CommentsAdapter;
import com.example.journeyjournal.ParseConnectorFiles.Comment;
import com.example.journeyjournal.ParseConnectorFiles.Post;
import com.example.journeyjournal.ParseConnectorFiles.User;
import com.example.journeyjournal.R;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CommentActivity extends AppCompatActivity {

    public static final String TAG = "CommentActivity";
    RecyclerView rvComments;
    protected CommentsAdapter adapter;
    protected List<Comment> allComments;
    private SwipeRefreshLayout swipeContainer;

    public User user = (User) ParseUser.getCurrentUser();
    Post post;
    TextView tvPost;
    EditText etComment;
    ImageView ivProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        rvComments = findViewById(R.id.rvComments);

        allComments = new ArrayList<>();
        adapter = new CommentsAdapter(this, allComments);

        tvPost = findViewById(R.id.tvPost);
        etComment = findViewById(R.id.etComment);
        ivProfileImage = findViewById(R.id.ivProfileImage);

        // set the adapter on the RV
        rvComments.setAdapter(adapter);
        // set the layout manager on RV
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        //query comments
        queryNetworkOrLocal();

        swipeContainer = findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        // Your code to refresh the list here.
        // Make sure you call swipeContainer.setRefreshing(false)
        // once the network request has completed successfully.
        swipeContainer.setOnRefreshListener(this::queryNetworkOrLocal);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        ParseFile profileImage = user.getProfileImage();
        if (profileImage != null) {
            Glide.with(CommentActivity.this).load(profileImage.getUrl()).circleCrop().into(ivProfileImage);}

        // stores post comment was made on
        post = getIntent().getParcelableExtra("post");

        // click post button --> comment compose
        tvPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifi.isConnected()) {
                    saveComment();
                } else {
                    Toast.makeText(CommentActivity.this, "Can not add comment without internet", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void saveComment() {
        // constructing new Comment
        Comment comment = new Comment();
        comment.setComment(etComment.getText().toString());
        comment.setPost(post);
        comment.setCommenter(ParseUser.getCurrentUser());
        etComment.setText("");

        // saves data in data store with label
        comment.pinInBackground("Comments");
        // saves change to parse when there is internet
        comment.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(CommentActivity.this, "Error while saving", Toast.LENGTH_SHORT).show();
                }
            }
        });
        queryNetworkOrLocal();
    }

    private void queryNetwork() {
        // specify type of data to query - Comments.class
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        // include data referred by user key
        query.include(Comment.KEY_COMMENTER);
        // limit query to latest 20 comments
        query.setLimit(20);
        query.setSkip(0);
        // order comments by create (newest first)
        query.addDescendingOrder("createdAt");
        // start asynchronous call for comments
        query.findInBackground(new FindCallback<Comment>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void done(List<Comment> comments, ParseException e) {

                Comment.unpinAllInBackground(comments);

                // Remove the previously cached results.
                Comment.unpinAllInBackground("Comments", new DeleteCallback() {
                    public void done(ParseException e) {
                        // Cache the new results.
                        Comment.pinAllInBackground("Comments", comments);
                    }
                });

                // check for failure
                if (e != null) {
                    Log.e(TAG, "Failure to load comments", e);
                    return;
                }
                // prints every comment description for debugging purposes
                for (Comment comment : comments) {
                    Log.i(TAG, "Comment: " + comment.getComment() + ", username: " + comment.getCommenter().getUsername());
                }
                // save received comments to list and notify adapter of change
                allComments.clear();
                allComments.addAll(comments);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        });
    }

    protected void queryLocal() {
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        query.include(Post.KEY_USER);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.setSkip(0);
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<Comment>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void done(List<Comment> comments, ParseException e) {
                Log.i(TAG, comments.toString());

                // Remove the previously cached results.
                Comment.unpinAllInBackground("Comments", new DeleteCallback() {
                    public void done(ParseException e) {
                        // Cache the new results.
                        Comment.pinAllInBackground("Comments", comments);
                    }
                });

                if (e != null) {
                    Log.e(TAG, "Issue getting posts.", e);
                    return;
                }

                // at this point, we have gotten the posts successfully
                for (Comment comment : comments) {
                    Log.i(TAG, "Comment: " + comment.getComment() + ", username: " + comment.getCommenter().getUsername());
                }

                allComments.clear();
                allComments.addAll(comments);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        });
    }

    private void queryNetworkOrLocal() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifi.isConnected()){
            queryNetwork();
        } else {
            queryLocal();}
    }
}