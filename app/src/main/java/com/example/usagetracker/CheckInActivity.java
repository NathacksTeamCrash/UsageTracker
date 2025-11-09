package com.example.usagetracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.Calendar;

public class CheckInActivity extends AppCompatActivity {
    private TextView waterLastMonthTextView, waterCurrentMonthTextView, waterChangeTextView;
    private TextView electricityLastMonthTextView, electricityCurrentMonthTextView, electricityChangeTextView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        
        // Setup toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Check-In");
            }
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        waterLastMonthTextView = findViewById(R.id.waterLastMonthTextView);
        waterCurrentMonthTextView = findViewById(R.id.waterCurrentMonthTextView);
        waterChangeTextView = findViewById(R.id.waterChangeTextView);
        electricityLastMonthTextView = findViewById(R.id.electricityLastMonthTextView);
        electricityCurrentMonthTextView = findViewById(R.id.electricityCurrentMonthTextView);
        electricityChangeTextView = findViewById(R.id.electricityChangeTextView);

        loadUserData();
        loadCurrentMonthUsage();
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getUser(firebaseUser.getUid(), task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentUser = firebaseHelper.documentToUser(task.getResult());
                updateLastMonthData();
            }
        });
    }

    private void updateLastMonthData() {
        if (currentUser != null) {
            waterLastMonthTextView.setText(String.format("%.2f L", currentUser.getPreviousMonthWaterUsage()));
            electricityLastMonthTextView.setText(String.format("%.2f kWh", currentUser.getPreviousMonthElectricityUsage()));
        }
    }

    private void loadCurrentMonthUsage() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        // Get current month start and end timestamps
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTimestamp = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long endTimestamp = calendar.getTimeInMillis();

        firebaseHelper.getUsageLogsForMonth(firebaseUser.getUid(), startTimestamp, endTimestamp, task -> {
            if (task.isSuccessful()) {
                double waterUsage = 0.0;
                double electricityUsage = 0.0;

                for (QueryDocumentSnapshot document : task.getResult()) {
                    UsageLog log = firebaseHelper.documentToUsageLog(document);
                    if ("Water".equals(log.getType())) {
                        waterUsage += log.getUsageAmount();
                    } else if ("Electric".equals(log.getType())) {
                        electricityUsage += log.getUsageAmount();
                    }
                }

                updateCurrentMonthData(waterUsage, electricityUsage);
                calculateChanges(waterUsage, electricityUsage);
            }
        });
    }

    private void updateCurrentMonthData(double waterUsage, double electricityUsage) {
        waterCurrentMonthTextView.setText(String.format("%.2f L", waterUsage));
        electricityCurrentMonthTextView.setText(String.format("%.2f kWh", electricityUsage));
    }

    private void calculateChanges(double currentWater, double currentElectricity) {
        if (currentUser == null) return;

        double lastMonthWater = currentUser.getPreviousMonthWaterUsage();
        double lastMonthElectricity = currentUser.getPreviousMonthElectricityUsage();

        // Calculate percentage change
        double waterChange = 0;
        if (lastMonthWater > 0) {
            waterChange = ((currentWater - lastMonthWater) / lastMonthWater) * 100;
        }

        double electricityChange = 0;
        if (lastMonthElectricity > 0) {
            electricityChange = ((currentElectricity - lastMonthElectricity) / lastMonthElectricity) * 100;
        }

        // Update UI
        String waterChangeText = String.format("%.1f%%", waterChange);
        String electricityChangeText = String.format("%.1f%%", electricityChange);

        waterChangeTextView.setText(waterChangeText);
        electricityChangeTextView.setText(electricityChangeText);

        // Color coding: green for reduction, red for increase
        if (waterChange < 0) {
            waterChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            waterChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (electricityChange < 0) {
            electricityChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            electricityChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

