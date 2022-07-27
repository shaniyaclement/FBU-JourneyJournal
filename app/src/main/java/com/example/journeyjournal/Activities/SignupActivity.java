package com.example.journeyjournal.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.journeyjournal.ParseConnectorFiles.User;
import com.example.journeyjournal.R;
import com.parse.ParseException;
import com.parse.SignUpCallback;

@SuppressWarnings("deprecation")
public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private EditText etUsername;
    private EditText etPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                if (wifi.isConnected()) {
                    signupUser(username, password);
                } else {
                    Toast.makeText(SignupActivity.this, "Can not signup without internet", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void signupUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(SignupActivity.this, "Error while saving", Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "User signup was successful");
                Toast.makeText(SignupActivity.this, "User signup was successful", Toast.LENGTH_SHORT).show();
                etPassword.setText("");
                etUsername.setText("");
                goMainActivity();
            }
        });
    }

    private void goMainActivity() {
        //navigates to mainActivity and prevents from going back to login with back button
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}