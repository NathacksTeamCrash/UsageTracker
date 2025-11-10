package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private static final String PREFS_NAME = "EcoLogPrefs";
    private static final String KEY_TEST_MODE = "test_mode";
    private static final String KEY_TEST_USER_ID = "test_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        registerTextView = findViewById(R.id.registerTextView);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loginButton.setOnClickListener(v -> loginUser());


        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void useTestLogin() {
        // Create a test user without Firebase
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String testUserId = "test_user_" + System.currentTimeMillis();
        editor.putBoolean(KEY_TEST_MODE, true);
        editor.putString(KEY_TEST_USER_ID, testUserId);
        editor.apply();

        // Create test user in Firestore (async, don't wait)
        User testUser = new User(testUserId, "Test User", "test@example.com");
        testUser.setHouseholdSize(2);
        testUser.setEcoPoints(100);
        testUser.setCurrentStreak(5);
        testUser.setHasCompletedQuestionnaire(true);
        testUser.setPreviousMonthWaterUsage(5000);
        testUser.setPreviousMonthElectricityUsage(300);

        // Save to Firestore in background
        firebaseHelper.saveUser(testUser, task -> {
            // Don't wait for this to complete
        });

        Toast.makeText(this, "Test login activated!", Toast.LENGTH_SHORT).show();
        navigateToDashboard(testUserId, true);
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        // Clear test mode if using real login
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TEST_MODE, false).apply();

        loginButton.setEnabled(false);
        loginButton.setText(R.string.loading);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.login);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            navigateToDashboard(user.getUid(), false);
                        }
                    } else {
                        String errorMsg = "Login failed";
                        if (task.getException() != null) {
                            errorMsg = task.getException().getMessage();
                            if (errorMsg.contains("network")) {
                                errorMsg = "Network error. Check your connection.";
                            } else if (errorMsg.contains("password")) {
                                errorMsg = "Invalid email or password";
                            } else if (errorMsg.contains("user")) {
                                errorMsg = "User not found. Please register first.";
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard(String userId, boolean isTestMode) {
        Log.d("LoginActivity", "========================================");
        Log.d("LoginActivity", "navigateToDashboard called");
        Log.d("LoginActivity", "userId: " + userId);
        Log.d("LoginActivity", "isTestMode: " + isTestMode);
        Log.d("LoginActivity", "========================================");

        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.getUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Log.d("LoginActivity", "✓ User document EXISTS in Firestore");

                DocumentSnapshot document = task.getResult();

                // Log all fields in the document
                Log.d("LoginActivity", "Document data: " + document.getData());

                User user = firebaseHelper.documentToUser(document);
                boolean setupComplete = false;

                if (user != null) {
                    Log.d("LoginActivity", "✓ User object created successfully");
                    Log.d("LoginActivity", "User name: " + user.getName());
                    Log.d("LoginActivity", "User email: " + user.getEmail());

                    // Check directly from document first
                    if (document.contains("setupComplete")) {
                        setupComplete = document.getBoolean("setupComplete") != null && document.getBoolean("setupComplete");
                        Log.d("LoginActivity", "setupComplete from document: " + setupComplete);
                    } else {
                        Log.d("LoginActivity", "⚠ Document does NOT contain 'setupComplete' field");
                    }

                    // Also try reflection
                    try {
                        java.lang.reflect.Method getSetupCompleteMethod = user.getClass().getMethod("getSetupComplete");
                        Object setupCompleteObj = getSetupCompleteMethod.invoke(user);
                        if (setupCompleteObj instanceof Boolean) {
                            boolean fromGetter = (Boolean) setupCompleteObj;
                            Log.d("LoginActivity", "setupComplete from getSetupComplete(): " + fromGetter);
                            setupComplete = fromGetter; // Use this value
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "⚠ Reflection failed (method doesn't exist or error)", e);
                        // Fallback to hasCompletedQuestionnaire
                        setupComplete = user.isHasCompletedQuestionnaire();
                        Log.d("LoginActivity", "setupComplete from hasCompletedQuestionnaire (fallback): " + setupComplete);
                    }
                } else {
                    Log.e("LoginActivity", "✗ User object is NULL!");
                }

                Log.d("LoginActivity", "========================================");
                Log.d("LoginActivity", "FINAL setupComplete value: " + setupComplete);
                Log.d("LoginActivity", "========================================");

                if (!setupComplete) {
                    Log.d("LoginActivity", "→ Navigating to QuestionnaireWelcomeActivity");
                    Intent intent = new Intent(LoginActivity.this, QuestionnaireWelcomeActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("IS_NEW_USER", false);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("LoginActivity", "→ Navigating to DashboardActivity");
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("IS_TEST_MODE", isTestMode);
                    startActivity(intent);
                    finish();
                }
            } else {
                Log.d("LoginActivity", "✗ User document DOES NOT EXIST in Firestore");
                Log.d("LoginActivity", "→ Navigating to QuestionnaireWelcomeActivity");
                Intent intent = new Intent(LoginActivity.this, QuestionnaireWelcomeActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("IS_NEW_USER", false);
                startActivity(intent);
                finish();
            }
        });
    }
}