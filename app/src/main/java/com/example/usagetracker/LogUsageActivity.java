package com.example.usagetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AutoCompleteTextView;
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
    private AutoCompleteTextView activitySpinner;
    private EditText usageAmountEditText;
    private Button saveLogButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private List<Goal> goalsList;
    private List<String> activitiesList;
    private User currentUser;
    // List to hold activity IDs
    private List<String> activityIds = new ArrayList<>();
    // List to hold activity target limits
    private List<Double> activityTargetLimits = new ArrayList<>();

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

        activitySpinner = findViewById(R.id.activityDropdown);
        usageAmountEditText = findViewById(R.id.usageAmountEditText);
        saveLogButton = findViewById(R.id.saveLogButton);

        goalsList = new ArrayList<>();
        activitiesList = new ArrayList<>();
        loadActivities();
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

    // Loads activities from the "activities" collection for the current user and populates the spinner.
    private void loadActivities() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getFirestore().collection("activities")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        activitiesList.clear();
                        activityIds.clear();
                        activityTargetLimits.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String activityName = document.getString("activityName");
                            if (activityName != null) {
                                activitiesList.add(activityName);
                                // Save activity ID
                                activityIds.add(document.getId());
                                // Save targetLimit (may be null)
                                Double targetLimit = document.getDouble("targetLimit");
                                activityTargetLimits.add(targetLimit != null ? targetLimit : 0.0);
                            }
                        }

                        if (activitiesList.isEmpty()) {
                            Toast.makeText(LogUsageActivity.this, "Please create an activity first", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                LogUsageActivity.this,
                                android.R.layout.simple_spinner_item,
                                activitiesList
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        activitySpinner.setAdapter(adapter);
                    } else {
                        Toast.makeText(LogUsageActivity.this, "Failed to load activities", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void saveLog() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null || activitiesList.isEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedActivity = activitySpinner.getText().toString().trim();
        int selectedPosition = activitiesList.indexOf(selectedActivity);
        if (selectedPosition < 0 || selectedPosition >= activitiesList.size()) {
            Toast.makeText(this, "Please select an activity", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedActivityName = activitiesList.get(selectedPosition);
        String usageAmountStr = usageAmountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(usageAmountStr)) {
            usageAmountEditText.setError("Usage amount is required");
            return;
        }

        try {
            double usageAmount = Double.parseDouble(usageAmountStr);

            // Prepare log data
            java.util.Map<String, Object> logData = new java.util.HashMap<>();
            logData.put("userId", userId);
            logData.put("activityId", activityIds.get(selectedPosition));
            logData.put("activityName", selectedActivityName);
            logData.put("usageAmount", usageAmount);
            // Add targetLimit for the selected activity
            Double targetLimit = activityTargetLimits.get(selectedPosition);
            logData.put("targetLimit", targetLimit);

            java.util.Date currentDate = new java.util.Date();
            logData.put("timestamp", new com.google.firebase.Timestamp(currentDate));

            boolean metGoal = usageAmount <= targetLimit;
            logData.put("metGoal", metGoal);

            firebaseHelper.getFirestore().collection("logs")
                    .add(logData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LogUsageActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                            finish();
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
