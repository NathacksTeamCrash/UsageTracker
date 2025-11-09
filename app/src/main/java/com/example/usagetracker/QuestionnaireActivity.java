package com.example.usagetracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class QuestionnaireActivity extends AppCompatActivity {
    private EditText householdSizeEditText, appliancesEditText, waterUsageEditText, 
                     electricityUsageEditText, sustainabilityGoalEditText;
    private Button submitButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        // Get user ID from intent (if coming from registration)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_ID")) {
            userId = intent.getStringExtra("USER_ID");
        }

        householdSizeEditText = findViewById(R.id.householdSizeEditText);
        appliancesEditText = findViewById(R.id.appliancesEditText);
        waterUsageEditText = findViewById(R.id.waterUsageEditText);
        electricityUsageEditText = findViewById(R.id.electricityUsageEditText);
        sustainabilityGoalEditText = findViewById(R.id.sustainabilityGoalEditText);
        submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> submitQuestionnaire());
    }

    private void submitQuestionnaire() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        
        // Use userId from intent if available, otherwise use Firebase user
        String targetUserId = userId;
        if (targetUserId == null && firebaseUser != null) {
            targetUserId = firebaseUser.getUid();
        }
        
        if (targetUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Make final for lambda
        final String finalUserId = targetUserId;

        String householdSizeStr = householdSizeEditText.getText().toString().trim();
        String appliancesStr = appliancesEditText.getText().toString().trim();
        String waterUsageStr = waterUsageEditText.getText().toString().trim();
        String electricityUsageStr = electricityUsageEditText.getText().toString().trim();
        String sustainabilityGoal = sustainabilityGoalEditText.getText().toString().trim();

        if (TextUtils.isEmpty(householdSizeStr)) {
            householdSizeEditText.setError("Household size is required");
            return;
        }

        if (TextUtils.isEmpty(waterUsageStr)) {
            waterUsageEditText.setError("Previous month water usage is required");
            return;
        }

        if (TextUtils.isEmpty(electricityUsageStr)) {
            electricityUsageEditText.setError("Previous month electricity usage is required");
            return;
        }

        try {
            final int householdSize = Integer.parseInt(householdSizeStr);
            final double waterUsage = Double.parseDouble(waterUsageStr);
            final double electricityUsage = Double.parseDouble(electricityUsageStr);

            // Create mutable list from appliances string
            final List<String> appliances = new ArrayList<>();
            if (!appliancesStr.isEmpty()) {
                String[] appliancesArray = appliancesStr.split(",");
                for (String appliance : appliancesArray) {
                    appliances.add(appliance.trim());
                }
            }

            // Get current user data and update it
            firebaseHelper.getUser(finalUserId, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    // User document exists, update it
                    User user = firebaseHelper.documentToUser(task.getResult());
                    user.setHouseholdSize(householdSize);
                    user.setMajorAppliances(appliances);
                    user.setPreviousMonthWaterUsage(waterUsage);
                    user.setPreviousMonthElectricityUsage(electricityUsage);
                    // Note: selectedGoals is now set in the new questionnaire flow (GoalSelectionActivity)
                    user.setHasCompletedQuestionnaire(true);

                    firebaseHelper.saveUser(user, task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(QuestionnaireActivity.this, "Questionnaire submitted successfully!", Toast.LENGTH_SHORT).show();
                            navigateToDashboard(finalUserId);
                        } else {
                            Toast.makeText(QuestionnaireActivity.this, "Failed to save questionnaire", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Create new user if document doesn't exist
                    String userName = "User";
                    String userEmail = "";
                    if (firebaseUser != null) {
                        userName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User";
                        userEmail = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    }
                    
                    User user = new User(finalUserId, userName, userEmail);
                    user.setHouseholdSize(householdSize);
                    user.setMajorAppliances(appliances);
                    user.setPreviousMonthWaterUsage(waterUsage);
                    user.setPreviousMonthElectricityUsage(electricityUsage);
                    // Note: selectedGoals is now set in the new questionnaire flow (GoalSelectionActivity)
                    user.setHasCompletedQuestionnaire(true);
                    user.setEcoPoints(0);
                    user.setCurrentStreak(0);
                    user.setLastLoginDate(System.currentTimeMillis());

                    firebaseHelper.saveUser(user, task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(QuestionnaireActivity.this, "Questionnaire submitted successfully!", Toast.LENGTH_SHORT).show();
                            navigateToDashboard(finalUserId);
                        } else {
                            Toast.makeText(QuestionnaireActivity.this, "Failed to save questionnaire", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDashboard(String userId) {
        Intent intent = new Intent(QuestionnaireActivity.this, DashboardActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("IS_TEST_MODE", false);
        startActivity(intent);
        finish();
    }
}

