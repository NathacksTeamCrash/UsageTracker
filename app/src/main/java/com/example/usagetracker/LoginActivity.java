package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
    private Button loginButton, testLoginButton;
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
        testLoginButton = findViewById(R.id.testLoginButton);
        registerTextView = findViewById(R.id.registerTextView);

        // Check if user is already logged in (only check Firebase, not test mode)
        // FirebaseUser currentUser = auth.getCurrentUser();
        // if (currentUser != null) {
        //     navigateToDashboard(currentUser.getUid(), false);
        //     return;
        // }

        // Check for test mode
        // SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // if (prefs.getBoolean(KEY_TEST_MODE, false)) {
        //     String testUserId = prefs.getString(KEY_TEST_USER_ID, null);
        //     if (testUserId != null) {
        //         navigateToDashboard(testUserId, true);
        //         return;
        //     }
        // }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loginButton.setOnClickListener(v -> loginUser());
        
        testLoginButton.setOnClickListener(v -> useTestLogin());

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
        // Check if user has completed setup (setupComplete flag)
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.getUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                com.example.usagetracker.models.User user = firebaseHelper.documentToUser(task.getResult());
                boolean setupComplete = false;
                if (user != null) {
                    // Try to get setupComplete from user model, fallback to hasCompletedQuestionnaire if not present
                    try {
                        // If User has getSetupComplete(), use it; else fallback
                        java.lang.reflect.Method getSetupCompleteMethod = user.getClass().getMethod("getSetupComplete");
                        Object setupCompleteObj = getSetupCompleteMethod.invoke(user);
                        if (setupCompleteObj instanceof Boolean) {
                            setupComplete = (Boolean) setupCompleteObj;
                        }
                    } catch (Exception e) {
                        // Fallback for legacy users
                        setupComplete = user.isHasCompletedQuestionnaire();
                    }
                }
                if (!setupComplete) {
                    // Navigate to questionnaire/setup flow if not complete
                    Intent intent = new Intent(LoginActivity.this, QuestionnaireWelcomeActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("IS_NEW_USER", false);
                    startActivity(intent);
                    finish();
                } else {
                    // Navigate to dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("IS_TEST_MODE", isTestMode);
                    startActivity(intent);
                    finish();
                }
            } else {
                // User doesn't exist, navigate to questionnaire/setup flow
                Intent intent = new Intent(LoginActivity.this, QuestionnaireWelcomeActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("IS_NEW_USER", false);
                startActivity(intent);
                finish();
            }
        });
    }
}

