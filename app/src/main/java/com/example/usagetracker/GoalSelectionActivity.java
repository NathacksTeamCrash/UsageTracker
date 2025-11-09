package com.example.usagetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class GoalSelectionActivity extends AppCompatActivity {
    private CheckBox saveMoneyCheckBox, reduceWaterCheckBox, lowerCarbonCheckBox,
                     buildHabitsCheckBox, trackUsageCheckBox;
    private Button continueButton;
    private String userId;
    private boolean isNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_selection);

        userId = getIntent().getStringExtra("USER_ID");
        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Choose Your Goals");
            }
        }

        saveMoneyCheckBox = findViewById(R.id.saveMoneyCheckBox);
        reduceWaterCheckBox = findViewById(R.id.reduceWaterCheckBox);
        lowerCarbonCheckBox = findViewById(R.id.lowerCarbonCheckBox);
        buildHabitsCheckBox = findViewById(R.id.buildHabitsCheckBox);
        trackUsageCheckBox = findViewById(R.id.trackUsageCheckBox);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            List<String> selectedGoals = new ArrayList<>();
            if (saveMoneyCheckBox.isChecked()) {
                selectedGoals.add("Save money on energy bills");
            }
            if (reduceWaterCheckBox.isChecked()) {
                selectedGoals.add("Reduce water consumption");
            }
            if (lowerCarbonCheckBox.isChecked()) {
                selectedGoals.add("Lower carbon footprint");
            }
            if (buildHabitsCheckBox.isChecked()) {
                selectedGoals.add("Build sustainable daily habits");
            }
            if (trackUsageCheckBox.isChecked()) {
                selectedGoals.add("Track and visualize utility usage");
            }

            if (selectedGoals.isEmpty()) {
                android.widget.Toast.makeText(this, "Please select at least one goal", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(GoalSelectionActivity.this, ActivitySetupActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("IS_NEW_USER", isNewUser);
            intent.putStringArrayListExtra("SELECTED_GOALS", new ArrayList<>(selectedGoals));
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

