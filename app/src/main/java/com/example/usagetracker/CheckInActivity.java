package com.example.usagetracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;
import android.widget.EditText;

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
    private TextView suggestionsTextView;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private Button checkInButton;

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

        checkInButton = findViewById(R.id.checkInButton);
        checkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCheckInDialog();
            }
        });

        loadUserData();
        loadCurrentMonthUsage();
    }

    private void showCheckInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Check-In");
        // Container for the EditTexts
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        // We'll use a vertical LinearLayout to hold the EditTexts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);
        final EditText waterEditText = new EditText(this);
        waterEditText.setHint("New Water Usage (L)");
        waterEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(waterEditText);
        final EditText electricityEditText = new EditText(this);
        electricityEditText.setHint("New Electricity Usage (kWh)");
        electricityEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(electricityEditText);
        builder.setView(layout);
        builder.setPositiveButton("Confirm", null); // We'll override this below
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String waterStr = waterEditText.getText().toString().trim();
                    String electricStr = electricityEditText.getText().toString().trim();
                    if (waterStr.isEmpty() || electricStr.isEmpty()) {
                        Toast.makeText(CheckInActivity.this, "Please enter both values", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newWater, newElectric;
                    try {
                        newWater = Double.parseDouble(waterStr);
                        newElectric = Double.parseDouble(electricStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(CheckInActivity.this, "Invalid input", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Retrieve household document again
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        Toast.makeText(CheckInActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }
                    firebaseHelper.getDb().collection("households")
                            .whereArrayContains("residents", firebaseUser.getUid())
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                    DocumentSnapshot householdDoc = task.getResult().getDocuments().get(0);
                                    String mood;
                                    if (householdDoc.contains("pastMonthWaterUsageMood") && householdDoc.get("pastMonthWaterUsageMood") != null)
                                        mood = String.valueOf(householdDoc.get("pastMonthWaterUsageMood"));
                                    else {
                                        mood = "";
                                    }
                                    double prevMonthWater = householdDoc.getDouble("previousMonthWaterUsage") != null ? householdDoc.getDouble("previousMonthWaterUsage") : 0.0;
                                    // Now, get the user document to update ecoPoints
                                    firebaseHelper.getDb().collection("users").document(firebaseUser.getUid())
                                            .get()
                                            .addOnSuccessListener(userDoc -> {
                                                double ecoPoints = 0.0;
                                                if (userDoc.contains("ecoPoints") && userDoc.get("ecoPoints") != null)
                                                    ecoPoints = userDoc.getDouble("ecoPoints");
                                                boolean addedPoints;
                                                boolean removedPoints;
                                                if ("Bad".equals(mood)) {
                                                    if (newWater <= prevMonthWater) {
                                                        removedPoints = false;
                                                        ecoPoints += 20.0;
                                                        addedPoints = true;
                                                    } else {
                                                        addedPoints = false;
                                                        ecoPoints -= 20.0;
                                                        removedPoints = true;
                                                    }
                                                } else {
                                                    removedPoints = false;
                                                    addedPoints = false;
                                                }
                                                double finalEcoPoints = ecoPoints;
                                                firebaseHelper.getDb().collection("users").document(firebaseUser.getUid())
                                                        .update("ecoPoints", finalEcoPoints)
                                                        .addOnSuccessListener(unused -> {
                                                            // Update household currentMonthWaterUsage and currentMonthElectricityUsage
                                                            householdDoc.getReference().update(
                                                                    "currentMonthWaterUsage", newWater,
                                                                    "currentMonthElectricityUsage", newElectric
                                                            ).addOnSuccessListener(householdUpdate -> {
                                                                // Now update UI and show toast on main thread
                                                                runOnUiThread(() -> {
                                                                    updateCurrentMonthData(newWater, newElectric);
                                                                    calculateChanges(newWater, newElectric);
                                                                    if (addedPoints) {
                                                                        Toast.makeText(CheckInActivity.this, "Usage updated and points adjusted!", Toast.LENGTH_LONG).show();
                                                                    } else if (removedPoints) {
                                                                        Toast.makeText(CheckInActivity.this, "Usage updated and points adjusted!", Toast.LENGTH_LONG).show();
                                                                    } else {
                                                                        Toast.makeText(CheckInActivity.this, "Usage updated and points adjusted!", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                                dialog.dismiss();
                                                            }).addOnFailureListener(e -> {
                                                                runOnUiThread(() -> Toast.makeText(CheckInActivity.this, "Failed to update usage in household.", Toast.LENGTH_SHORT).show());
                                                                dialog.dismiss();
                                                            });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(CheckInActivity.this, "Failed to update ecoPoints.", Toast.LENGTH_SHORT).show();
                                                            dialog.dismiss();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(CheckInActivity.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            });
                                } else {
                                    Toast.makeText(CheckInActivity.this, "No household found for this user", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            });
                }
            });
        });
        dialog.show();
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getDb().collection("households")
            .whereArrayContains("residents", firebaseUser.getUid())
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot householdDoc = task.getResult().getDocuments().get(0);
                    double prevWater = householdDoc.getDouble("previousMonthWaterUsage") != null ? householdDoc.getDouble("previousMonthWaterUsage") : 0.0;
                    double prevElectric = householdDoc.getDouble("previousMonthElectricityUsage") != null ? householdDoc.getDouble("previousMonthElectricityUsage") : 0.0;
                    double currWater = householdDoc.getDouble("currentMonthWaterUsage") != null ? householdDoc.getDouble("currentMonthWaterUsage") : 0.0;
                    double currElectric = householdDoc.getDouble("currentMonthElectricityUsage") != null ? householdDoc.getDouble("currentMonthElectricityUsage") : 0.0;

                    runOnUiThread(() -> {
                        waterLastMonthTextView.setText(String.format("%.2f L", prevWater));
                        electricityLastMonthTextView.setText(String.format("%.2f kWh", prevElectric));
                        waterCurrentMonthTextView.setText(String.format("%.2f L", currWater));
                        electricityCurrentMonthTextView.setText(String.format("%.2f kWh", currElectric));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No household found for this user", Toast.LENGTH_SHORT).show());
                }
            });

        // Old user retrieval code commented out since no longer needed
        /*
        firebaseHelper.getUser(firebaseUser.getUid(), task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                currentUser = firebaseHelper.documentToUser(task.getResult());
                runOnUiThread(() -> updateLastMonthData());
            }
        });
        */
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
            if (task.isSuccessful() && task.getResult() != null) {
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

                // Create final copies for use in lambda
                final double finalWaterUsage = waterUsage;
                final double finalElectricityUsage = electricityUsage;

                runOnUiThread(() -> {
                    updateCurrentMonthData(finalWaterUsage, finalElectricityUsage);
                    calculateChanges(finalWaterUsage, finalElectricityUsage);
                });
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
            waterChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            waterChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }

        if (electricityChange < 0) {
            electricityChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            electricityChangeTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }

        // Generate suggestions
        generateSuggestions(waterChange, electricityChange, currentWater, currentElectricity);
    }

    private void generateSuggestions(double waterChange, double electricityChange, double currentWater, double currentElectricity) {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("💡 Suggestions:\n\n");

        if (waterChange > 0) {
            // Water usage increased
            double reductionNeeded = (currentWater - currentUser.getPreviousMonthWaterUsage()) / 30.0; // Average per day
            suggestions.append("• Try reducing shower time by ").append(String.format("%.1f", reductionNeeded / 10)).append(" minutes\n");
            suggestions.append("• Fix any leaky faucets\n");
            suggestions.append("• Use a timer when washing dishes\n");
        } else if (waterChange < -5) {
            suggestions.append("• Great job reducing water usage! Keep it up!\n");
        }

        if (electricityChange > 0) {
            // Electricity usage increased
            double reductionNeeded = (currentElectricity - currentUser.getPreviousMonthElectricityUsage()) / 30.0; // Average per day
            suggestions.append("• Turn off lights when not in use\n");
            suggestions.append("• Unplug devices when not charging\n");
            suggestions.append("• Try reducing TV/computer time by ").append(String.format("%.1f", reductionNeeded / 2)).append(" hours per day\n");
        } else if (electricityChange < -5) {
            suggestions.append("• Excellent work on reducing electricity! Continue your efforts!\n");
        }

        if (waterChange <= 0 && electricityChange <= 0 && Math.abs(waterChange) < 5 && Math.abs(electricityChange) < 5) {
            suggestions.append("• You're doing well! Try to maintain or improve your current usage levels.\n");
        }

        if (suggestions.length() == "💡 Suggestions:\n\n".length()) {
            suggestions.append("• Keep tracking your usage to maintain your progress!\n");
        }

        suggestionsTextView.setText(suggestions.toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
