package com.example.journeyjournal.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.journeyjournal.Activities.ComposePostActivity;
import com.example.journeyjournal.Activities.RemindersActivity;
import com.example.journeyjournal.ParseConnectorFiles.Post;
import com.example.journeyjournal.Adapters.PostsAdapter;
import com.example.journeyjournal.Activities.LoginActivity;
import com.example.journeyjournal.ParseConnectorFiles.Reminder;
import com.example.journeyjournal.R;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private SwipeRefreshLayout swipeContainer;
    ImageButton ibLogout;
    ImageButton ibNewPost;
    RecyclerView rvPosts;
    protected PostsAdapter adapter;
    protected List<Post> allPosts;

    public FeedFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
            // query posts from the database
            Log.i(TAG, "onResume");
            adapter.clear();
            querySavedPosts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        ibLogout = view.findViewById(R.id.ibLogout);
        ibNewPost = view.findViewById(R.id.ibAddJournal);
        rvPosts = view.findViewById(R.id.rvReminders);
        // initialize the array that will hold posts and create a PostsAdapter
        allPosts = new ArrayList<>();
        adapter = new PostsAdapter(getContext(), allPosts);
        // set the adapter on the recycler view
        rvPosts.setAdapter(adapter);
        // set the layout manager on the recycler view
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        querySavedPosts();

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                if(wifi.isConnected()){
                    queryPosts();
                } else {
                    querySavedPosts();}
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        ibLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logOut();
                ParseUser currentUser = ParseUser.getCurrentUser();
                Intent i = new Intent(getContext(), LoginActivity.class);
                startActivity(i);
                getActivity().finish();
            }
        });

        ibNewPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goNewPost();
            }
        });
    }

    private void goNewPost() {
            Intent intent = new Intent(getActivity(), ComposePostActivity.class);
            startActivity(intent);
    }


    private void queryPosts() {
        // specify what type of data we want to query - Post.class
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        // include data referred by user key
        query.include(Post.KEY_USER);
        query.setLimit(20);
        query.setSkip(0);
        // order posts by creation date (newest first)
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {

                // Remove the previously cached results.
                Post.unpinAllInBackground("Posts", new DeleteCallback() {
                    public void done(ParseException e) {
                        // Cache the new results.
                        Post.pinAllInBackground("Posts", posts);
                    }
                });

                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }
                for (Post post : posts) {
                    Log.i(TAG, "Post: " + post.getDescription() + ", username: " + post.getUser().getUsername());
                }
                // save received posts to list and notify adapter of new data
                allPosts.clear();
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        });
    }

    protected void querySavedPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.include(Post.KEY_USER);
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                Log.i(TAG, posts.toString());

                if (e != null) {
                    Log.e(TAG, "Issue getting posts.", e);
                    return;
                }

                // at this point, we have gotten the posts successfully
                for (Post post : posts) {
                    Log.i(TAG, "Post: " + post.getDescription() + ", username: " + post.getUser().getUsername());
                }

                allPosts.clear();
                allPosts.addAll(posts);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        });
    }
}
