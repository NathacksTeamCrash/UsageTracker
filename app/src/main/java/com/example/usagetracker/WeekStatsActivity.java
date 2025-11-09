package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.Calendar;
import java.util.List;

public class WeekStatsActivity extends AppCompatActivity {
    private TextView goalsMetTextView, totalPointsTextView, waterSavedTextView, electricitySavedTextView, progressTextView;
    private LeafProgressView leafProgressView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private boolean isTestMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week_stats);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Weekly Stats");
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        // Get user ID from intent or Firebase
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("USER_ID");
            isTestMode = intent.getBooleanExtra("IS_TEST_MODE", false);
        }

        if (userId == null) {
            FirebaseUser firebaseUser = auth.getCurrentUser();
            if (firebaseUser != null) {
                userId = firebaseUser.getUid();
            } else {
                SharedPreferences prefs = getSharedPreferences("EcoLogPrefs", MODE_PRIVATE);
                if (prefs.getBoolean("test_mode", false)) {
                    userId = prefs.getString("test_user_id", null);
                    isTestMode = true;
                }
            }
        }

        goalsMetTextView = findViewById(R.id.goalsMetTextView);
        totalPointsTextView = findViewById(R.id.totalPointsTextView);
        waterSavedTextView = findViewById(R.id.waterSavedTextView);
        electricitySavedTextView = findViewById(R.id.electricitySavedTextView);
        leafProgressView = findViewById(R.id.leafProgressView);
        progressTextView = findViewById(R.id.progressTextView);

        loadWeekStats();
    }

    private void loadWeekStats() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current week start and end timestamps
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long weekStart = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        long weekEnd = calendar.getTimeInMillis();

        // Load goals and logs for the week
        firebaseHelper.getGoals(userId, goalsTask -> {
            if (goalsTask.isSuccessful() && goalsTask.getResult() != null) {
                List<Goal> goals = new java.util.ArrayList<>();
                for (QueryDocumentSnapshot doc : goalsTask.getResult()) {
                    goals.add(firebaseHelper.documentToGoal(doc));
                }

                firebaseHelper.getUsageLogsForMonth(userId, weekStart, weekEnd, logsTask -> {
                    if (logsTask.isSuccessful() && logsTask.getResult() != null) {
                        List<UsageLog> logs = new java.util.ArrayList<>();
                        for (QueryDocumentSnapshot doc : logsTask.getResult()) {
                            logs.add(firebaseHelper.documentToUsageLog(doc));
                        }

                        calculateAndDisplayStats(goals, logs);
                    } else {
                        calculateAndDisplayStats(goals, new java.util.ArrayList<>());
                    }
                });
            } else {
                calculateAndDisplayStats(new java.util.ArrayList<>(), new java.util.ArrayList<>());
            }
        });
    }

    private void calculateAndDisplayStats(List<Goal> goals, List<UsageLog> logs) {
        int goalsMet = 0;
        int totalPoints = 0;
        double waterSaved = 0;
        double electricitySaved = 0;
        final int totalGoals = goals.size();

        // Calculate stats
        for (UsageLog log : logs) {
            if (log.isMetGoal()) {
                goalsMet++;
            }
            totalPoints += log.getEcoPointsEarned();

            // Find corresponding goal to calculate savings
            for (Goal goal : goals) {
                if (goal.getGoalId().equals(log.getGoalId())) {
                    if (log.isMetGoal() && log.getUsageAmount() < goal.getTargetLimit()) {
                        double saved = goal.getTargetLimit() - log.getUsageAmount();
                        if ("Water".equals(goal.getType())) {
                            waterSaved += saved;
                        } else if ("Electric".equals(goal.getType())) {
                            electricitySaved += saved;
                        }
                    }
                }
            }
        }

        // Make variables final for lambda
        final int finalGoalsMet = goalsMet;
        final int finalTotalPoints = totalPoints;
        final double finalWaterSaved = waterSaved;
        final double finalElectricitySaved = electricitySaved;

        // Calculate progress percentage (0-100)
        float progress = 0;
        if (totalGoals > 0) {
            progress = (finalGoalsMet * 100.0f) / totalGoals;
        } else if (logs.size() > 0) {
            // If no goals but have logs, use points as indicator
            progress = Math.min(finalTotalPoints / 10.0f, 100.0f);
        }
        final float finalProgress = progress;

        // Update UI
        runOnUiThread(() -> {
            goalsMetTextView.setText(finalGoalsMet + " / " + totalGoals);
            totalPointsTextView.setText(String.valueOf(finalTotalPoints));
            waterSavedTextView.setText(String.format("%.2f L", finalWaterSaved));
            electricitySavedTextView.setText(String.format("%.2f kWh", finalElectricitySaved));
            
            // Update leaf progress
            leafProgressView.setProgress(finalProgress);
            progressTextView.setText(String.format("%.0f%%", finalProgress));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

