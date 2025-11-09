package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        Intent intent;
        if (currentUser != null) {
            // User is logged in via Firebase
            intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("USER_ID", currentUser.getUid());
            intent.putExtra("IS_TEST_MODE", false);
        } else {
            // Check for test mode
            SharedPreferences prefs = getSharedPreferences("EcoLogPrefs", MODE_PRIVATE);
            if (prefs.getBoolean("test_mode", false)) {
                String testUserId = prefs.getString("test_user_id", null);
                if (testUserId != null) {
                    intent = new Intent(MainActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", testUserId);
                    intent.putExtra("IS_TEST_MODE", true);
                } else {
                    intent = new Intent(MainActivity.this, LoginActivity.class);
                }
            } else {
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
        }
        
        startActivity(intent);
        finish();
    }
}