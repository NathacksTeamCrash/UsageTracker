package com.example.usagetracker;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ActivitySetupActivity extends AppCompatActivity {
    private LinearLayout activitiesContainer;
    private Button addActivityButton, finishButton;
    private String userId;
    private boolean isNewUser;
    private List<String> selectedGoals;
    private List<ActivityInput> activityInputs;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth auth;

    private static class ActivityInput {
        EditText activityNameEditText;
        EditText targetEditText;
        Spinner typeSpinner;
        Spinner frequencySpinner;
        Spinner unitSpinner;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_setup);

        // Try to get userId from Intent, otherwise use FirebaseAuth
        auth = FirebaseAuth.getInstance();
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null || userId.isEmpty()) {
            FirebaseUser firebaseUser = auth.getCurrentUser();
            if (firebaseUser != null) {
                userId = firebaseUser.getUid();
            }
        }
        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);
        selectedGoals = getIntent().getStringArrayListExtra("SELECTED_GOALS");
        if (selectedGoals == null) {
            selectedGoals = new ArrayList<>();
        }

        firebaseHelper = new FirebaseHelper();
        activityInputs = new ArrayList<>();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Set Your Targets");
            }
        }

        activitiesContainer = findViewById(R.id.activitiesContainer);
        addActivityButton = findViewById(R.id.addActivityButton);
        finishButton = findViewById(R.id.finishButton);

        addActivityButton.setOnClickListener(v -> addActivityInput());
        finishButton.setOnClickListener(v -> {
            Log.d("ActivitySetup", "Finish button clicked!");
            saveActivitiesAndFinish();
        });

        // Add first activity input by default
        addActivityInput();
    }

    private void addActivityInput() {
        View activityView = getLayoutInflater().inflate(R.layout.item_activity_input, activitiesContainer, false);
        ActivityInput input = new ActivityInput();
        input.activityNameEditText = activityView.findViewById(R.id.activityNameEditText);
        input.targetEditText = activityView.findViewById(R.id.targetEditText);
        input.typeSpinner = activityView.findViewById(R.id.typeSpinner);
        input.frequencySpinner = activityView.findViewById(R.id.frequencySpinner);
        input.unitSpinner = activityView.findViewById(R.id.unitSpinner);

        // Setup spinners with arrays
        android.widget.ArrayAdapter<CharSequence> typeAdapter = android.widget.ArrayAdapter.createFromResource(
                this, R.array.activity_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        input.typeSpinner.setAdapter(typeAdapter);

        android.widget.ArrayAdapter<CharSequence> frequencyAdapter = android.widget.ArrayAdapter.createFromResource(
                this, R.array.frequencies, android.R.layout.simple_spinner_item);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        input.frequencySpinner.setAdapter(frequencyAdapter);

        android.widget.ArrayAdapter<CharSequence> unitAdapter = android.widget.ArrayAdapter.createFromResource(
                this, R.array.units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        input.unitSpinner.setAdapter(unitAdapter);

        Button removeButton = activityView.findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> {
            activitiesContainer.removeView(activityView);
            activityInputs.remove(input);
        });

        activitiesContainer.addView(activityView);
        activityInputs.add(input);
    }

    private void saveActivitiesAndFinish() {
        Log.d("ActivitySetup", "saveActivitiesAndFinish called");

        if (activityInputs.isEmpty()) {
            Toast.makeText(this, "Please add at least one activity", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Goal> goals = new ArrayList<>();
        for (ActivityInput input : activityInputs) {
            String activityName = input.activityNameEditText.getText().toString().trim();
            String targetStr = input.targetEditText.getText().toString().trim();

            if (activityName.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double targetLimit = Double.parseDouble(targetStr);
                String type = input.typeSpinner.getSelectedItem().toString();
                String frequency = input.frequencySpinner.getSelectedItem().toString();
                String unit = input.unitSpinner.getSelectedItem().toString();

                Goal goal = new Goal(userId, activityName, targetLimit, type, frequency, unit);
                goals.add(goal);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for target", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Log.d("ActivitySetup", "Created " + goals.size() + " goals, calling saveGoalsToFirestore");
        // Save all goals to Firestore
        saveGoalsToFirestore(goals);
    }

    private void saveGoalsToFirestore(List<Goal> goals) {
        Log.d("ActivitySetup", "=== START saveGoalsToFirestore ===");
        Log.d("ActivitySetup", "Number of goals: " + goals.size());

        if (goals.isEmpty()) {
            Log.e("ActivitySetup", "No goals to save!");
            return;
        }

        final int[] savedCount = {0};
        final int[] failedCount = {0};
        final int totalGoals = goals.size();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        String uid = (firebaseUser != null) ? firebaseUser.getUid() : userId;

        Log.d("ActivitySetup", "Using userId: " + uid);

        Toast.makeText(this, "Saving activities...", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < goals.size(); i++) {
            Goal goal = goals.get(i);
            final int goalIndex = i;

            Log.d("ActivitySetup", "Preparing to save goal " + (i+1) + ": " + goal.getActivityName());

            java.util.HashMap<String, Object> activityData = new java.util.HashMap<>();
            activityData.put("activityName", goal.getActivityName());
            activityData.put("targetLimit", goal.getTargetLimit());
            activityData.put("type", goal.getType());
            activityData.put("frequency", goal.getFrequency());
            activityData.put("unit", goal.getUnit());
            activityData.put("userId", uid);

            Log.d("ActivitySetup", "Calling Firestore add() for goal " + (i+1));

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("activities")
                    .add(activityData)
                    .addOnSuccessListener(documentReference -> {
                        savedCount[0]++;
                        Log.d("ActivitySetup", "✓ SUCCESS for goal " + (goalIndex+1) + ": " + savedCount[0] + "/" + totalGoals + " saved");

                        if (savedCount[0] + failedCount[0] == totalGoals) {
                            Log.d("ActivitySetup", "!!! All goals processed! Calling updateUserProfile() !!!");
                            runOnUiThread(() -> updateUserProfile());
                        }
                    })
                    .addOnFailureListener(e -> {
                        failedCount[0]++;
                        Log.e("ActivitySetup", "✗ FAILURE for goal " + (goalIndex+1) + ": " + e.getMessage(), e);

                        if (savedCount[0] + failedCount[0] == totalGoals) {
                            Log.d("ActivitySetup", "All goals processed (with errors). Saved: " + savedCount[0] + ", Failed: " + failedCount[0]);
                            if (savedCount[0] > 0) {
                                runOnUiThread(() -> updateUserProfile());
                            } else {
                                Toast.makeText(ActivitySetupActivity.this,
                                        "All activities failed to save. Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        Log.d("ActivitySetup", "=== END saveGoalsToFirestore (all add() calls initiated) ===");
    }

    private void updateUserProfile() {
        Log.d("ActivitySetup", "!!! updateUserProfile called !!!");

        FirebaseUser firebaseUser = auth.getCurrentUser();
        String finalUserId = (firebaseUser != null) ? firebaseUser.getUid() : userId;
        Log.d("ActivitySetup", "Using userId for navigation: " + finalUserId);

        // Create a map with ALL the user data we want to save
        java.util.HashMap<String, Object> userData = new java.util.HashMap<>();
        userData.put("setupComplete", true);
        userData.put("hasCompletedQuestionnaire", true);
        if (selectedGoals != null && !selectedGoals.isEmpty()) {
            userData.put("selectedGoals", selectedGoals);
        }

        // Update or create user document with setupComplete flag
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(finalUserId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("ActivitySetup", "✓✓✓ Setup complete! setupComplete=true saved to Firestore");
                    Log.d("ActivitySetup", "Navigating to dashboard...");
                    Toast.makeText(ActivitySetupActivity.this, "Setup complete!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ActivitySetupActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", finalUserId);
                    intent.putExtra("IS_TEST_MODE", false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("ActivitySetup", "✗✗✗ Failed to update setupComplete flag", e);
                    // Navigate anyway since activities are saved
                    Toast.makeText(ActivitySetupActivity.this, "Activities saved! Continuing...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ActivitySetupActivity.this, DashboardActivity.class);
                    intent.putExtra("USER_ID", finalUserId);
                    intent.putExtra("IS_TEST_MODE", false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}