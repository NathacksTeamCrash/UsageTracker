package com.example.usagetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;
import com.example.usagetracker.utils.GamificationHelper;

import java.util.ArrayList;
import java.util.List;

public class LogUsageActivity extends AppCompatActivity {
    private Spinner activitySpinner;
    private EditText usageAmountEditText;
    private Button saveLogButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private List<Goal> goalsList;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_usage);
        
        // Setup toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Log Usage");
            }
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        activitySpinner = findViewById(R.id.activitySpinner);
        usageAmountEditText = findViewById(R.id.usageAmountEditText);
        saveLogButton = findViewById(R.id.saveLogButton);

        goalsList = new ArrayList<>();
        loadGoals();
        loadUserData();

        saveLogButton.setOnClickListener(v -> saveLog());
    }

    private void loadUserData() {
        String userId = getIntent().getStringExtra("USER_ID");
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
        }
        
        if (userId == null) return;

        firebaseHelper.getUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentUser = firebaseHelper.documentToUser(task.getResult());
            }
        });
    }

    private void loadGoals() {
        String userId = getIntent().getStringExtra("USER_ID");
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
        }
        
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getGoals(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                goalsList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Goal goal = firebaseHelper.documentToGoal(document);
                    goalsList.add(goal);
                }

                if (goalsList.isEmpty()) {
                    Toast.makeText(LogUsageActivity.this, "Please create a goal first", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Populate spinner
                List<String> activityNames = new ArrayList<>();
                for (Goal goal : goalsList) {
                    activityNames.add(goal.getActivityName());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        LogUsageActivity.this,
                        android.R.layout.simple_spinner_item,
                        activityNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activitySpinner.setAdapter(adapter);
            }
        });
    }

    private void saveLog() {
        String userId = getIntent().getStringExtra("USER_ID");
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
        }
        
        if (userId == null || goalsList.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = activitySpinner.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= goalsList.size()) {
            Toast.makeText(this, "Please select an activity", Toast.LENGTH_SHORT).show();
            return;
        }

        Goal selectedGoal = goalsList.get(selectedPosition);
        String usageAmountStr = usageAmountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(usageAmountStr)) {
            usageAmountEditText.setError("Usage amount is required");
            return;
        }

        try {
            double usageAmount = Double.parseDouble(usageAmountStr);

            // Calculate if goal was met and eco-points
            boolean metGoal = GamificationHelper.metGoal(usageAmount, selectedGoal.getTargetLimit());
            int ecoPointsEarned = GamificationHelper.calculateEcoPoints(usageAmount, selectedGoal.getTargetLimit());

            // Create usage log
            UsageLog log = new UsageLog(
                    userId,
                    selectedGoal.getGoalId(),
                    selectedGoal.getActivityName(),
                    usageAmount,
                    selectedGoal.getType()
            );
            log.setMetGoal(metGoal);
            log.setEcoPointsEarned(ecoPointsEarned);

            // Save log
            firebaseHelper.saveUsageLog(log, task -> {
                if (task.isSuccessful()) {
                    // Update user's eco-points and streak
                    if (currentUser != null) {
                        int newEcoPoints = currentUser.getEcoPoints() + ecoPointsEarned;
                        int newStreak = GamificationHelper.updateStreak(currentUser.getCurrentStreak(), metGoal);

                        currentUser.setEcoPoints(newEcoPoints);
                        currentUser.setCurrentStreak(newStreak);

                        firebaseHelper.saveUser(currentUser, task1 -> {
                            if (task1.isSuccessful()) {
                                String message = metGoal
                                        ? "Great job! You earned " + ecoPointsEarned + " eco-points!"
                                        : "Goal not met. Keep trying!";
                                Toast.makeText(LogUsageActivity.this, message, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    } else {
                        Toast.makeText(LogUsageActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(LogUsageActivity.this, "Failed to save log", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            usageAmountEditText.setError("Please enter a valid number");
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

