package com.example.journeyjournal.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.example.journeyjournal.ParseConnectorFiles.User;
import com.example.journeyjournal.R;
import com.example.journeyjournal.fragments.ComposeFragment;
import com.example.journeyjournal.fragments.FeedFragment;
import com.example.journeyjournal.fragments.HomeFragment;
import com.example.journeyjournal.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.ParseUser;

// activity that holds bottom navigation bar and allows it ot be accessible
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_buttons, menu);
        return true;
    }

    public static final String TAG = "MainActivity";

    HomeFragment homeFragment = new HomeFragment();
    FeedFragment feedFragment = new FeedFragment();
    ComposeFragment composeFragment = new ComposeFragment();
    ProfileFragment profileFragment = new ProfileFragment();

    // declaring items in layout
    public BottomNavigationView bottomNavigationView;

    // setting up fragments
    final FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.themeTan));
        }


        // bottom navigation bar functionality
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener((BottomNavigationView.OnNavigationItemSelectedListener) item -> {
            Fragment fragmentToShow = null;
            switch (item.getItemId()) {
                case R.id.action_home:
                    fragmentToShow = homeFragment;
                    break;
                case R.id.action_compose:
                    fragmentToShow = composeFragment;
                    break;
                case R.id.action_feed:
                    fragmentToShow = feedFragment;
                    break;
                case R.id.action_profile:
                    fragmentToShow = profileFragment;
                    break;
                default: break;
            }
            if (fragmentToShow != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flContainer, fragmentToShow).commit();
            }
            return true;
        });
        bottomNavigationView.setSelectedItemId(R.id.action_profile);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_post:
                goComposePost();
                return true;
            case R.id.new_journal:
                goComposeJournal();
                return true;
            case R.id.new_reminder:
                goComposeReminder();
                return true;
            case R.id.logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void goToProfileFragment(ParseUser user) {
        // makes sure the profile navigation leads to the user whose image was selected
        bottomNavigationView.setSelectedItemId(R.id.action_profile);
        profileFragment.user = (User) user;
    }

    public void goComposePost(){
        Intent intent = new Intent(this, ComposePostActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.left_out, R.anim.right_in);
    }

    public void goComposeJournal(){
        Intent intent = new Intent(this, ComposeJournal.class);
        startActivity(intent);
        overridePendingTransition(R.anim.left_out, R.anim.right_in);
    }

    public void logout(){
        ParseUser.logOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        this.finish();
    }
    public void goComposeReminder(){
        Intent intent = new Intent(this, ComposeReminder.class);
        startActivity(intent);
        overridePendingTransition(R.anim.left_out, R.anim.right_in);
    }

}