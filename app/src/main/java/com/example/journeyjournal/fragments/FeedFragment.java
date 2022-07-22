package com.example.journeyjournal.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.journeyjournal.Activities.ComposePostActivity;
import com.example.journeyjournal.ParseConnectorFiles.Post;
import com.example.journeyjournal.Adapters.PostsAdapter;
import com.example.journeyjournal.Activities.LoginActivity;
import com.example.journeyjournal.ParseConnectorFiles.User;
import com.example.journeyjournal.R;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.SettingsClient;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    private SwipeRefreshLayout swipeContainer;
    ImageButton ibLogout;
    ImageButton ibNewPost;
    RecyclerView rvPosts;
    protected PostsAdapter adapter;
    protected List<Post> allPosts;

    double longitude;
    double latitude;
    private final User currentUser = (User) ParseUser.getCurrentUser();
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult.getLastLocation() != null) {
                onLocationChanged(locationResult.getLastLocation());
            }
        }
    };

    long MIN_DISTANCE = 2 * 1609;
    long FASTEST_INTERVAL = 600000;

    public FeedFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        ConnectivityManager connManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.i(TAG, "onResume");
        adapter.clear();
        whichQuery();
        if (wifi.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        swipeContainer = view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        // Your code to refresh the list here.
        // Make sure you call swipeContainer.setRefreshing(false)
        // once the network request has completed successfully.
        swipeContainer.setOnRefreshListener(this::whichQuery);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        ibLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logOut();
                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        ibNewPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goNewPost();
            }
        });
    }


    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        /* 2 Mile */
        mLocationRequest.setInterval(MIN_DISTANCE);
        /* 60 sec */
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(requireActivity());
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_DENIED && ActivityCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
            Log.i(TAG, "permission denied ask");
            return;
        }
        LocationServices.getFusedLocationProviderClient(requireActivity()).requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        Toast.makeText(getActivity(), "Permission ok", Toast.LENGTH_SHORT).show();

    }

    public void onLocationChanged(Location location) {
        // New location has now been determined
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        ParseGeoPoint location1 = new ParseGeoPoint(latitude, longitude);
        currentUser.setLocation(location1);
        String msg = "Updated Location: " + latitude + "," + longitude;
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
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

        // TODO - add more scoping constraints (followings, user interest)

        // query posts that were created within 25 miles of the user location
        query.whereWithinMiles(Post.KEY_LOCATION, currentUser.getLocation(), 25.0);
        // order posts by creation date (newest first)
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Post>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void done(List<Post> posts, ParseException e) {
                Log.i(TAG, posts.toString());

                // Remove the previously cached results.
                Post.unpinAllInBackground("Posts", new DeleteCallback() {
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.e(TAG, "Issue with unpinning posts", e);
                            return;
                        }
                        // Cache the new results.
                        Log.i(TAG, "posts deleted and added");
                        Post.pinAllInBackground("Posts", posts);
                        Log.i(TAG, posts.toString());
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
            @SuppressLint("NotifyDataSetChanged")
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

    private void whichQuery() {
        ConnectivityManager connManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi.isConnected()) {
            queryPosts();
            startLocationUpdates();
        } else {
            querySavedPosts();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocationServices.getFusedLocationProviderClient(requireActivity()).removeLocationUpdates(mLocationCallback);
    }

}

