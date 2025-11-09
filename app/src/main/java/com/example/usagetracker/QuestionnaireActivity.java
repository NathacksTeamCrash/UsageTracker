package com.example.usagetracker;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.usagetracker.models.Household;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class QuestionnaireActivity extends AppCompatActivity {
    private static final String TAG = "QuestionnaireActivity";
    private EditText householdSizeEditText, appliancesEditText, waterUsageEditText,
            electricityUsageEditText;
    private Button submitButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private boolean isNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        // Get user ID and new user flag from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_ID")) {
            userId = intent.getStringExtra("USER_ID");
            isNewUser = intent.getBooleanExtra("IS_NEW_USER", false);
        }

        householdSizeEditText = findViewById(R.id.householdSizeEditText);
        appliancesEditText = findViewById(R.id.appliancesEditText);
        waterUsageEditText = findViewById(R.id.waterUsageEditText);
        electricityUsageEditText = findViewById(R.id.electricityUsageEditText);
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

            // Create list from appliances string
            final List<String> appliances = new ArrayList<>();
            if (!appliancesStr.isEmpty()) {
                String[] appliancesArray = appliancesStr.split(",");
                for (String appliance : appliancesArray) {
                    appliances.add(appliance.trim());
                }
            }

            Log.d(TAG, "Creating household with userId: " + finalUserId);

            // Create new Household object
            Household household = new Household(null, householdSize);
            household.setMajorAppliances(appliances);
            household.setPreviousMonthWaterUsage(waterUsage);
            household.setPreviousMonthElectricityUsage(electricityUsage);
            household.setCreatedAt(System.currentTimeMillis());
            household.setUpdatedAt(System.currentTimeMillis());

            // Add current user to residents array
            household.addResident(finalUserId);

            // Save household to Firestore
            firebaseHelper.saveHousehold(household, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String householdId = task.getResult().getId();
                    Log.d(TAG, "✓ Household saved successfully with ID: " + householdId);
                    Toast.makeText(QuestionnaireActivity.this,
                            "Household information saved!",
                            Toast.LENGTH_SHORT).show();

                    // Continue to next step (GoalSelectionActivity)
                    navigateToGoalSelection(finalUserId, householdId);
                } else {
                    Log.e(TAG, "✗ Failed to save household", task.getException());
                    Toast.makeText(QuestionnaireActivity.this,
                            "Failed to save household information",
                            Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Number format error", e);
        }
    }

    private void navigateToGoalSelection(String userId, String householdId) {
        Log.d(TAG, "Navigating to GoalSelectionActivity");
        Intent intent = new Intent(QuestionnaireActivity.this, GoalSelectionActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("HOUSEHOLD_ID", householdId);
        intent.putExtra("IS_NEW_USER", isNewUser);
        startActivity(intent);
        finish();
    }
}