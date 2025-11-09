package com.example.usagetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ViewLogsActivity extends AppCompatActivity {
    private RecyclerView logsRecyclerView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private LogsAdapter logsAdapter;
    private List<UsageLog> logsList;
    private String userId;
    private boolean isTestMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_logs);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Logs");
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

        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        logsList = new ArrayList<>();
        logsAdapter = new LogsAdapter(logsList);
        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logsRecyclerView.setAdapter(logsAdapter);

        loadLogs();
    }

    private void loadLogs() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.getUsageLogs(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                logsList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    UsageLog log = firebaseHelper.documentToUsageLog(document);
                    logsList.add(log);
                }
                runOnUiThread(() -> logsAdapter.notifyDataSetChanged());
            } else {
                Toast.makeText(ViewLogsActivity.this, "Failed to load logs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

