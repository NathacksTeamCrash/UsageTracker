package com.example.usagetracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeekStatsActivity extends AppCompatActivity {
    private TextView goalsMetTextView, totalPointsTextView, waterSavedTextView, electricitySavedTextView, progressTextView;
    private LeafProgressView leafProgressView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private boolean isTestMode = false;

    private int totalGoals = 0, goalsMet = 0, totalPoints = 0;
    private double waterSaved = 0, electricitySaved = 0;
    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();

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

    @Override
    protected void onResume() {
        super.onResume();
        loadWeekStats(); // Refresh data for the current week whenever this page becomes visible
    }

    private void loadWeekStats() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    // Get selectedGoals array and count
                    List<String> selectedGoals = (List<String>) document.get("selectedGoals");
                    if (selectedGoals != null) {
                        totalGoals = selectedGoals.size();
                    }

                    // Get ecoPoints
                    Long points = document.getLong("ecoPoints");
                    if (points != null) {
                        totalPoints = points.intValue();
                    }

                    updateUI(); // Update denominator and points display
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            });

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

        String weekId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // Fetch the current week's stats immediately on load
        db.collection("weeklyStats").document(userId)
            .collection("weeks").document(weekId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    Long met = snapshot.getLong("goalsMet");
                    Long total = snapshot.getLong("totalGoals");
                    Double water = snapshot.getDouble("waterSaved");
                    Double electric = snapshot.getDouble("electricitySaved");
                    Long points = snapshot.getLong("ecoPoints");

                    if (met != null) goalsMet = met.intValue();
                    if (total != null) totalGoals = total.intValue();
                    if (water != null) waterSaved = water;
                    if (electric != null) electricitySaved = electric;
                    if (points != null) totalPoints = points.intValue();

                    updateUI(); // Immediately reflect current Firestore data
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load weekly data", Toast.LENGTH_SHORT).show();
            });

        db.collection("weeklyStats").document(userId)
          .collection("weeks").document(weekId)
          .addSnapshotListener((snapshot, e) -> {
              if (e != null) {
                  Toast.makeText(this, "Failed to listen for weekly updates", Toast.LENGTH_SHORT).show();
                  return;
              }
              if (snapshot != null && snapshot.exists()) {
                  Long met = snapshot.getLong("goalsMet");
                  Long total = snapshot.getLong("totalGoals");
                  Double water = snapshot.getDouble("waterSaved");
                  Double electric = snapshot.getDouble("electricitySaved");
                  Long points = snapshot.getLong("ecoPoints");

                  if (met != null) goalsMet = met.intValue();
                  if (total != null) totalGoals = total.intValue();
                  if (water != null) waterSaved = water;
                  if (electric != null) electricitySaved = electric;
                  if (points != null) totalPoints = points.intValue();

                  updateUI();
              }
          });

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
                        setupClickListeners();
                    } else {
                        calculateAndDisplayStats(goals, new java.util.ArrayList<>());
                        setupClickListeners();
                    }
                });
            } else {
                calculateAndDisplayStats(new java.util.ArrayList<>(), new java.util.ArrayList<>());
                setupClickListeners();
            }
        });
    }

    private void calculateAndDisplayStats(List<Goal> goals, List<UsageLog> logs) {
        goalsMet = 0;
        totalPoints = 0;
        waterSaved = 0;
        electricitySaved = 0;
        totalGoals = goals.size();

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

        updateUI();
    }

    private void updateUI() {
        float progress = (totalGoals > 0) ? (goalsMet * 100.0f / totalGoals) : 0f;
        runOnUiThread(() -> {
            goalsMetTextView.setText(goalsMet + " / " + totalGoals);
            totalPointsTextView.setText(String.valueOf(totalPoints));
            waterSavedTextView.setText(String.format("%.2f L", waterSaved));
            electricitySavedTextView.setText(String.format("%.2f kWh", electricitySaved));
            leafProgressView.setProgress(progress);
            progressTextView.setText(String.format("%.0f%%", progress));
        });
    }

    private void setupClickListeners() {
        goalsMetTextView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("How many goals have you met so far?");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("0 - " + totalGoals);
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String valueStr = input.getText().toString();
                if (!valueStr.isEmpty()) {
                    int value = Integer.parseInt(valueStr);
                    if (value >= 0 && value <= totalGoals) {
                        goalsMet = value;
                        updateUI();
                        saveWeeklyStats();
                    } else {
                        Toast.makeText(this, "Please enter a value between 0 and " + totalGoals, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        waterSavedTextView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter water saved (liters)");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint(String.format("%.2f", waterSaved));
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String valueStr = input.getText().toString();
                if (!valueStr.isEmpty()) {
                    try {
                        double value = Double.parseDouble(valueStr);
                        if (value >= 0) {
                            waterSaved = value;
                            updateUI();
                            saveWeeklyStats();
                        } else {
                            Toast.makeText(this, "Please enter a non-negative value", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        electricitySavedTextView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter electricity saved (kWh)");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint(String.format("%.2f", electricitySaved));
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String valueStr = input.getText().toString();
                if (!valueStr.isEmpty()) {
                    try {
                        double value = Double.parseDouble(valueStr);
                        if (value >= 0) {
                            electricitySaved = value;
                            updateUI();
                            saveWeeklyStats();
                        } else {
                            Toast.makeText(this, "Please enter a non-negative value", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });
    }

    private void saveWeeklyStats() {
        if (userId == null) return;
        Calendar cal = Calendar.getInstance();
        String weekId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        Map<String, Object> weeklyData = new HashMap<>();
        weeklyData.put("goalsMet", goalsMet);
        weeklyData.put("totalGoals", totalGoals);
        weeklyData.put("progressPercent", (totalGoals > 0 ? (goalsMet * 100.0f / totalGoals) : 0f));
        weeklyData.put("waterSaved", waterSaved);
        weeklyData.put("electricitySaved", electricitySaved);
        weeklyData.put("ecoPoints", totalPoints);
        weeklyData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("weeklyStats").document(userId)
          .collection("weeks").document(weekId)
          .set(weeklyData, SetOptions.merge());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
