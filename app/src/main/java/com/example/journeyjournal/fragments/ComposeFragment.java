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

import com.example.journeyjournal.Activities.ComposeJournal;
import com.example.journeyjournal.ParseConnectorFiles.Journals;
import com.example.journeyjournal.Adapters.JournalsAdapter;
import com.example.journeyjournal.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class ComposeFragment extends Fragment {

    private static final String TAG = "ComposeFragment";
    private SwipeRefreshLayout swipeContainer;
    ImageButton ibAddJournal;
    RecyclerView rvJournals;
    protected JournalsAdapter adapter;
    protected List<Journals> allJournals;

    public ComposeFragment() {
        // Required empty public constructor
    }


    @Override
    public void onResume() {
        super.onResume();
        ConnectivityManager connManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifi.isConnected()){
            // query posts from the database
            Log.i(TAG, "onResume");
            adapter.clear();
            queryJournals();
        } else {
            querySavedJournals();}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        ibAddJournal = view.findViewById(R.id.ibAddJournal);
        rvJournals = view.findViewById(R.id.rvJournals);

        allJournals = new ArrayList<>();
        adapter = new JournalsAdapter(getContext(), allJournals);
        // set the adapter on the recycler view
        rvJournals.setAdapter(adapter);
        // set the layout manager on the recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvJournals.setLayoutManager(linearLayoutManager);
        // query posts from Parse SDK if there is wifi
        if(wifi.isConnected()){
            queryJournals();
        } else {
            querySavedJournals();}

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if(wifi.isConnected()){
                    // query posts from the database
                    Log.i(TAG, "onResume");
                    adapter.clear();
                    queryJournals();
                } else {
                    querySavedJournals();}
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        ibAddJournal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifi.isConnected()) {
                    Intent intent = new Intent(getContext(), ComposeJournal.class);
                    startActivity(intent);
                } else{
                    Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
                }
            }});
    }


    private void queryJournals() {
        // specify what type of data we want to query - Journals.class
        ParseQuery<Journals> query = ParseQuery.getQuery(Journals.class);
        // include data referred by user key
        query.include(Journals.KEY_USER);
        // limit query to latest 20 items
        query.setLimit(20);
        query.whereEqualTo(Journals.KEY_USER, ParseUser.getCurrentUser());
        // order posts by creation date (newest first)
        query.addDescendingOrder("createdAt");
        // start an asynchronous call for posts
        query.findInBackground(new FindCallback<Journals>() {
            @Override
            public void done(List<Journals> journals, ParseException e) {
                // check for errors
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }

                // for debugging purposes let's print every post description to logcat
                for (Journals journal : journals) {
                    Log.i(TAG, "Post: " + journal.getTitle() + ", username: " + journal.getUser().getUsername());
                }

                // save received posts to list and notify adapter of new data
                allJournals.clear();
                adapter.notifyDataSetChanged();
                allJournals.addAll(journals);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);

            }
        });
    }

    private void querySavedJournals(){
        ParseQuery<Journals> query = ParseQuery.getQuery(Journals.class);
        query.include(Journals.KEY_USER);
        query.whereEqualTo(Journals.KEY_USER, ParseUser.getCurrentUser());
        query.fromLocalDatastore();
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback<Journals>() {
            @Override
            public void done(List<Journals> journal, ParseException e) {
                // check for failure
                if (e != null) {
                    Log.e(TAG, "Failure to load saved reminders", e);
                    return;
                }
                // prints every reminder description for debugging purposes
                for (Journals journals : journal){
                    Log.i(TAG, "Journal: " + journals.getEntry() + ", username: " + journals.getUser().getUsername());
                    Log.i(TAG, "Saved in database");
                }
                // save received comments to list and notify adapter of change
                allJournals.clear();
                allJournals.addAll(journal);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);

            }
    });}
}