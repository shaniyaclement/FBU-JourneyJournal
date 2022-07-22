package com.example.journeyjournal.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.journeyjournal.Adapters.PostsAdapter;
import com.example.journeyjournal.Adapters.ReminderAdapter;
import com.example.journeyjournal.ParseConnectorFiles.Post;
import com.example.journeyjournal.ParseConnectorFiles.Reminder;
import com.example.journeyjournal.ParseConnectorFiles.User;
import com.example.journeyjournal.R;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ComposePostActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 42;
    private static final int PICK_PHOTO_CODE = 1046;
    EditText etDescription;
    Button btnImage;
    Button btnUpload;
    Button btnPost;
    ImageView ivImage;
    protected PostsAdapter adapter;
    protected List<Post> allPosts;

    File photoFile;
    public String photoFileName = "photo.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_post);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


        etDescription = findViewById(R.id.etDescription);
        btnImage = findViewById(R.id.btnImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnPost = findViewById(R.id.btnPost);
        ivImage = findViewById(R.id.ivImage);

        allPosts = new ArrayList<>();
        adapter = new PostsAdapter(this, allPosts);

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description = etDescription.getText().toString();
                if (description.isEmpty()) {
                    Toast.makeText(ComposePostActivity.this, "Description cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (photoFile == null || ivImage.getDrawable() == null) {
                    Toast.makeText(ComposePostActivity.this, "No image", Toast.LENGTH_SHORT).show();
                    return;
                }
                User currentUser = (User) ParseUser.getCurrentUser();
                if (wifi.isConnected()) {
                    savePost(description, currentUser, photoFile);
                } else {
                    Toast.makeText(ComposePostActivity.this, "Can not add post without internet", Toast.LENGTH_LONG).show();
                }
            }
        });
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });
    }


    // launches implicit intent to open the phone camera and take the photo for the post
    private void launchCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName);

        // wrap File object into a content provider, needed for URI >= 24
        Uri fileProvider = FileProvider.getUriForFile(this, "com.codepath.fileprovider", photoFile);
        //make app a file provider
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Start the image capture intent to take photo
            //noinspection deprecation
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }


    // Returns the File for a photo stored on disk given the fileName
    private File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos using `getExternalFilesDir` on Context to access package-specific directories
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + fileName);
    }

    // create a new post and saves to app
    private void savePost(String description, User currentUser, File photoFile) {
        Post post = new Post();
        post.setDescription(description);
        post.setImage(new ParseFile(this.photoFile));
        post.setUser(currentUser);
        Log.i(TAG, "Post was successful");
        etDescription.setText("");
        ivImage.setImageResource(0);
        // saves data in data store with label
        post.pinInBackground("Posts");
        // saves change to parse when there is internet
        post.saveEventually();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // switch case changes how the method is used based on how it is triggered
        switch (requestCode) {
            //  adds taken image to image preview
            case (CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE):
                if (resultCode == RESULT_OK) {
                    // by this point we have the camera photo on disk
                    Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                    // RESIZE BITMAP, see section below
                    // compresses image for successful Parse upload
                    // Load the taken image into a preview
                    ivImage.setImageBitmap(takenImage);
                } else { // Result was a failure
                    Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
                }
            default:
                Log.i(TAG, "default case");
                break;
        }

    }
}