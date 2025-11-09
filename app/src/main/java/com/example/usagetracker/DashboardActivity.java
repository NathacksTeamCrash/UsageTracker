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
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private TextView ecoPointsTextView, streakTextView, userNameTextView;
    private RecyclerView recentLogsRecyclerView;
    private FloatingActionButton fabLogUsage, fabMenu;
    private Button checkInButton, leaderboardButton, weekStatsButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private LogsAdapter logsAdapter;
    private List<UsageLog> recentLogsList;
    private boolean isTestMode = false;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Setup toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Dashboard is main screen
            }
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

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
        setupRecyclerView();
        loadUserData();
        loadRecentLogs();
    }

    private void initializeViews() {
        ecoPointsTextView = findViewById(R.id.ecoPointsTextView);
        streakTextView = findViewById(R.id.streakTextView);
        userNameTextView = findViewById(R.id.userNameTextView);
        recentLogsRecyclerView = findViewById(R.id.recentLogsRecyclerView);
        fabLogUsage = findViewById(R.id.fabLogUsage);
        fabMenu = findViewById(R.id.fabMenu);
        checkInButton = findViewById(R.id.checkInButton);
        leaderboardButton = findViewById(R.id.leaderboardButton);
        weekStatsButton = findViewById(R.id.weekStatsButton);

        // FAB for logging
        fabLogUsage.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LogUsageActivity.class);
            startActivity(intent);
        });

        // Menu FAB - show options
        fabMenu.setOnClickListener(v -> showMenuOptions());

        checkInButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CheckInActivity.class);
            startActivity(intent);
        });

        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LeaderboardActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("IS_TEST_MODE", isTestMode);
            startActivity(intent);
        });

        weekStatsButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, WeekStatsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("IS_TEST_MODE", isTestMode);
            startActivity(intent);
        });
    }

    private void showMenuOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Menu");
        String[] options = {"View All Logs", "Create Goal", "Settings"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    Intent logsIntent = new Intent(DashboardActivity.this, ViewLogsActivity.class);
                    logsIntent.putExtra("USER_ID", userId);
                    logsIntent.putExtra("IS_TEST_MODE", isTestMode);
                    startActivity(logsIntent);
                    break;
                case 1:
                    Intent goalIntent = new Intent(DashboardActivity.this, CreateGoalActivity.class);
                    startActivity(goalIntent);
                    break;
                case 2:
                    // Settings - could add later
                    Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        builder.show();
    }

    private void setupRecyclerView() {
        recentLogsList = new ArrayList<>();
        logsAdapter = new LogsAdapter(recentLogsList);
        recentLogsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentLogsRecyclerView.setAdapter(logsAdapter);
    }

    private void loadRecentLogs() {
        if (userId == null) return;

        firebaseHelper.getUsageLogs(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                recentLogsList.clear();
                int count = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (count >= 10) break; // Show only last 10 logs
                    UsageLog log = firebaseHelper.documentToUsageLog(document);
                    recentLogsList.add(log);
                    count++;
                }
                runOnUiThread(() -> logsAdapter.notifyDataSetChanged());
            }
        });
    }

    private void loadUserData() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // For test mode, create a dummy user immediately
        if (isTestMode) {
            createTestUser();
            return;
        }

        // Load from Firebase (async, non-blocking)
        firebaseHelper.getUser(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                currentUser = firebaseHelper.documentToUser(task.getResult());
                updateUI();
                loadRecentLogs();
            } else {
                // If user doesn't exist, create test user as fallback
                createTestUser();
            }
        });
    }

    private void createTestUser() {
        // Create test user data immediately (no network call)
        currentUser = new User(userId, "Test User", "test@example.com");
        currentUser.setHouseholdSize(2);
        currentUser.setEcoPoints(100);
        currentUser.setCurrentStreak(5);
        currentUser.setHasCompletedQuestionnaire(true);
        currentUser.setPreviousMonthWaterUsage(5000);
        currentUser.setPreviousMonthElectricityUsage(300);
        
        updateUI();
        loadRecentLogs();
    }

    private void updateUI() {
        if (currentUser != null) {
            userNameTextView.setText("Welcome, " + currentUser.getName() + "!");
            ecoPointsTextView.setText(String.valueOf(currentUser.getEcoPoints()));
            streakTextView.setText(currentUser.getCurrentStreak() + " " + getString(R.string.days));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if needed (not on every resume to reduce lag)
        if (currentUser == null) {
            loadUserData();
        } else {
            // Refresh user data and logs
            loadUserData();
            loadRecentLogs();
        }
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
