package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private TextView ecoPointsTextView, streakTextView, userNameTextView;
    private RecyclerView activitiesRecyclerView;
    private RecyclerView logsRecyclerView;
    private FloatingActionButton fabLogUsage;
    private FloatingActionButton fabView;
    private Button checkInButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private GoalsAdapter goalsAdapter;
    private List<String> goalStringsList;
    private boolean isTestMode = false;
    private String userId;
    private LogsAdapter logsAdapter;
    private List<UsageLog> logsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Dashboard is main screen
                getSupportActionBar().setTitle("EcoLog");
            }
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        goalStringsList = new ArrayList<>();

        // Get user ID and test mode from intent
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("USER_ID");
            isTestMode = intent.getBooleanExtra("IS_TEST_MODE", false);
        }

        // If no userId from intent, try to get from Firebase or SharedPreferences
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

        initializeViews();
        setupRecyclerViews();
        loadUserData();
        loadActivities();
    }

    private void initializeViews() {
        ecoPointsTextView = findViewById(R.id.ecoPointsTextView);
        streakTextView = findViewById(R.id.streakTextView);
        userNameTextView = findViewById(R.id.userNameTextView);
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        fabLogUsage = findViewById(R.id.fabLogUsage);
        fabView = findViewById(R.id.fabView);
        checkInButton = findViewById(R.id.checkInButton);

        // FAB for logging
        fabLogUsage.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LogUsageActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        // FAB for viewing stats
        fabView.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, WeekStatsActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        FloatingActionButton fabLeaderboard = findViewById(R.id.fabLeaderboard);
        fabLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LeaderboardActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        checkInButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CheckInActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        // FAB for notifications/reminders
        FloatingActionButton fabNotifications = findViewById(R.id.fabNotification);
        fabNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, RemindersActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });
    }

    private void setupRecyclerViews() {
        // Setup Activities RecyclerView (for goals)
        goalsAdapter = new GoalsAdapter(goalStringsList);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activitiesRecyclerView.setAdapter(goalsAdapter);

        // Setup Logs RecyclerView
        logsAdapter = new LogsAdapter(logsList);
        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logsRecyclerView.setAdapter(logsAdapter);
    }

    private void loadActivities() {
        if (userId == null) return;

        firebaseHelper.getFirestore().collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        List<String> selectedGoals = (List<String>) task.getResult().get("selectedGoals");
                        goalStringsList.clear();
                        if (selectedGoals != null) {
                            goalStringsList.addAll(selectedGoals);
                        }
                        runOnUiThread(() -> goalsAdapter.notifyDataSetChanged());
                    }
                });
    }

    // Loads the user's activity logs from the logs collection and displays them in the logsAdapter
    private void loadUserLogs() {
        if (userId == null) return;

        firebaseHelper.getFirestore().collection("logs")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        logsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UsageLog log = firebaseHelper.documentToUsageLog(document);
                            logsList.add(log);
                        }
                        runOnUiThread(() -> {
                            logsAdapter.setLogs(logsList);
                            logsAdapter.notifyDataSetChanged();
                        });
                    }
                });
    }

    private void loadCurrentUsage() {
        if (userId == null || goalStringsList.isEmpty()) return;

        // Get current period (today for daily, this week for weekly)
        Calendar calendar = Calendar.getInstance();
        long startTimestamp;

        // For now, calculate for today (daily) or this week (weekly)
        // We'll aggregate usage for each goal
        Map<String, Double> usageMap = new HashMap<>();

        firebaseHelper.getUsageLogs(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    UsageLog log = firebaseHelper.documentToUsageLog(document);
                    String goalId = log.getGoalId();

                    // Check if log is within current period
                    // Since we no longer have Goal objects, skip this part or adjust accordingly
                    // Here we skip period check because we lack goal frequency info
                    double current = usageMap.getOrDefault(goalId, 0.0);
                    usageMap.put(goalId, current + log.getUsageAmount());
                }

                runOnUiThread(() -> {
                    // No progressAdapter to update now
                });
            }
        });
    }

    private Goal findGoalById(String goalId) {
        return null; // Removed as goalsList no longer exists
    }

    private boolean isLogInCurrentPeriod(UsageLog log, Goal goal) {
        return true; // Removed as goalsList and Goal objects no longer used
    }

    private void loadUserData() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set userId for logsAdapter and load logs immediately after userId assignment
        logsAdapter.setUserId(userId);
        loadUserLogs();

        // For test mode, create a dummy user immediately
        if (isTestMode) {
            createTestUser();
            return;
        }

        // Load from Firebase
        firebaseHelper.getUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                currentUser = firebaseHelper.documentToUser(task.getResult());
                Long ecoPointsLong = task.getResult().getLong("ecoPoints");
                int ecoPoints = ecoPointsLong != null ? ecoPointsLong.intValue() : 0;
                runOnUiThread(() -> {
                    ecoPointsTextView.setText(String.valueOf(ecoPoints));
                    updateUI();
                });
            } else {
                // If user doesn't exist, create test user as fallback
                createTestUser();
            }
        });
    }

    private void createTestUser() {
        currentUser = new User(userId, "Test User", "test@example.com");
        currentUser.setHouseholdSize(2);
        currentUser.setEcoPoints(100);
        currentUser.setCurrentStreak(5);
        currentUser.setHasCompletedQuestionnaire(true);
        currentUser.setPreviousMonthWaterUsage(5000);
        currentUser.setPreviousMonthElectricityUsage(300);

        updateUI();
    }

    private void updateUI() {
        if (currentUser != null) {
            userNameTextView.setText("Welcome, " + currentUser.getName() + "!");
            ecoPointsTextView.setText(String.valueOf(currentUser.getEcoPoints()));
            streakTextView.setText(currentUser.getCurrentStreak() + " days");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadActivities();
        loadUserLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            // Clear test mode
            SharedPreferences prefs = getSharedPreferences("EcoLogPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("test_mode", false).apply();

            // Sign out from Firebase
            auth.signOut();

            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}