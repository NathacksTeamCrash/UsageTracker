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
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private TextView ecoPointsTextView, streakTextView, userNameTextView;
    private RecyclerView goalsRecyclerView;
    private Button addGoalButton, logUsageButton, checkInButton, leaderboardButton, viewLogsButton, weekStatsButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private GoalsAdapter goalsAdapter;
    private List<Goal> goalsList;
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
    }

    private void initializeViews() {
        ecoPointsTextView = findViewById(R.id.ecoPointsTextView);
        streakTextView = findViewById(R.id.streakTextView);
        userNameTextView = findViewById(R.id.userNameTextView);
        goalsRecyclerView = findViewById(R.id.goalsRecyclerView);
        addGoalButton = findViewById(R.id.addGoalButton);
        logUsageButton = findViewById(R.id.logUsageButton);
        checkInButton = findViewById(R.id.checkInButton);
        leaderboardButton = findViewById(R.id.leaderboardButton);
        viewLogsButton = findViewById(R.id.viewLogsButton);
        weekStatsButton = findViewById(R.id.weekStatsButton);

        addGoalButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreateGoalActivity.class);
            startActivity(intent);
        });

        logUsageButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LogUsageActivity.class);
            startActivity(intent);
        });

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

        viewLogsButton.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ViewLogsActivity.class);
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

    private void setupRecyclerView() {
        goalsList = new ArrayList<>();
        goalsAdapter = new GoalsAdapter(goalsList);
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalsRecyclerView.setAdapter(goalsAdapter);
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
                loadGoals();
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
        loadGoals();
    }

    private void updateUI() {
        if (currentUser != null) {
            userNameTextView.setText("Welcome, " + currentUser.getName() + "!");
            ecoPointsTextView.setText(String.valueOf(currentUser.getEcoPoints()));
            streakTextView.setText(currentUser.getCurrentStreak() + " " + getString(R.string.days));
        }
    }

    private void loadGoals() {
        if (userId == null) return;

        // Load goals asynchronously
        firebaseHelper.getGoals(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                goalsList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Goal goal = firebaseHelper.documentToGoal(document);
                    goalsList.add(goal);
                }
                // Update UI on main thread
                runOnUiThread(() -> goalsAdapter.notifyDataSetChanged());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if needed (not on every resume to reduce lag)
        if (currentUser == null) {
            loadUserData();
        } else {
            // Just refresh goals
            loadGoals();
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
