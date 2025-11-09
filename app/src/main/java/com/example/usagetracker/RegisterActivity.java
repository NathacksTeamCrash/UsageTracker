package com.example.usagetracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);

        registerButton.setOnClickListener(v -> registerUser());

        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText(R.string.loading);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true);
                    registerButton.setText(R.string.register);
                    
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d("RegisterActivity", "User registered successfully: " + firebaseUser.getUid());
                            
                            // User is automatically logged in after registration
                            // Create user document in Firestore with all initial data
                            User user = new User(firebaseUser.getUid(), name, email);
                            user.setLastLoginDate(System.currentTimeMillis());
                            user.setEcoPoints(0);
                            user.setCurrentStreak(0);
                            user.setHasCompletedQuestionnaire(false);
                            
                            // Save user to Firestore
                            firebaseHelper.saveUser(user, task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.d("RegisterActivity", "User data saved to Firestore successfully");
                                    // User data saved successfully, navigate to questionnaire
                                    Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                                    
                                    // Navigate to questionnaire welcome screen
                                    Intent intent = new Intent(RegisterActivity.this, QuestionnaireWelcomeActivity.class);
                                    intent.putExtra("USER_ID", firebaseUser.getUid());
                                    intent.putExtra("IS_NEW_USER", true);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Log the error
                                    if (task1.getException() != null) {
                                        Log.e("RegisterActivity", "Firestore save error: " + task1.getException().getMessage(), task1.getException());
                                    }
                                    
                                    // Even if Firestore save fails, user is logged in, so proceed
                                    // But show a warning
                                    Toast.makeText(RegisterActivity.this, "Account created but profile save failed. Please try again.", Toast.LENGTH_LONG).show();
                                    
                                    // Still navigate to questionnaire - user can complete it
                                    Intent intent = new Intent(RegisterActivity.this, QuestionnaireWelcomeActivity.class);
                                    intent.putExtra("USER_ID", firebaseUser.getUid());
                                    intent.putExtra("IS_NEW_USER", true);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } else {
                            Log.e("RegisterActivity", "Registration successful but FirebaseUser is null");
                            Toast.makeText(RegisterActivity.this, "Registration failed: User not created", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Registration failed - log and show detailed error
                        Exception exception = task.getException();
                        String errorMsg = "Registration failed";
                        
                        if (exception != null) {
                            Log.e("RegisterActivity", "Registration error: " + exception.getMessage(), exception);
                            
                            // Check if it's a FirebaseAuthException for specific error codes
                            if (exception instanceof FirebaseAuthException) {
                                FirebaseAuthException authException = (FirebaseAuthException) exception;
                                String errorCode = authException.getErrorCode();
                                
                                switch (errorCode) {
                                    case "ERROR_EMAIL_ALREADY_IN_USE":
                                        errorMsg = "This email is already registered. Please login instead.";
                                        break;
                                    case "ERROR_INVALID_EMAIL":
                                        errorMsg = "Invalid email address. Please check your email.";
                                        break;
                                    case "ERROR_WEAK_PASSWORD":
                                        errorMsg = "Password is too weak. Please use a stronger password (at least 6 characters).";
                                        break;
                                    case "ERROR_NETWORK_REQUEST_FAILED":
                                        errorMsg = "Network error. Please check your internet connection.";
                                        break;
                                    case "ERROR_INTERNAL_ERROR":
                                        errorMsg = "Internal error occurred. Please try again later.";
                                        break;
                                    case "ERROR_OPERATION_NOT_ALLOWED":
                                        errorMsg = "Email/password accounts are not enabled. Please contact support.";
                                        break;
                                    default:
                                        errorMsg = "Registration failed: " + errorCode + ". Please try again.";
                                        break;
                                }
                            } else {
                                // Generic exception handling
                                String exceptionMsg = exception.getMessage();
                                if (exceptionMsg != null) {
                                    if (exceptionMsg.contains("email-already-in-use") || exceptionMsg.contains("EMAIL_EXISTS")) {
                                        errorMsg = "This email is already registered. Please login instead.";
                                    } else if (exceptionMsg.contains("network") || exceptionMsg.contains("NETWORK")) {
                                        errorMsg = "Network error. Check your connection and try again.";
                                    } else if (exceptionMsg.contains("invalid-email") || exceptionMsg.contains("INVALID_EMAIL")) {
                                        errorMsg = "Invalid email address. Please check your email.";
                                    } else if (exceptionMsg.contains("weak-password") || exceptionMsg.contains("WEAK_PASSWORD")) {
                                        errorMsg = "Password is too weak. Please use a stronger password.";
                                    } else {
                                        errorMsg = "Registration failed: " + exceptionMsg;
                                    }
                                } else {
                                    errorMsg = "Registration failed: Unknown error occurred.";
                                }
                            }
                        } else {
                            errorMsg = "Registration failed: Unknown error occurred.";
                        }
                        
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}

