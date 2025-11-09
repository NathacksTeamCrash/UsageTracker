package com.example.usagetracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.example.usagetracker.models.Goal;
import com.example.usagetracker.utils.FirebaseHelper;

public class CreateGoalActivity extends AppCompatActivity {
    private EditText activityNameEditText, targetLimitEditText;
    private Spinner typeSpinner, frequencySpinner, unitSpinner;
    private Button saveButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_goal);
        
        // Setup toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Create Goal");
            }
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        activityNameEditText = findViewById(R.id.activityNameEditText);
        targetLimitEditText = findViewById(R.id.targetLimitEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        frequencySpinner = findViewById(R.id.frequencySpinner);
        unitSpinner = findViewById(R.id.unitSpinner);
        saveButton = findViewById(R.id.saveButton);

        setupSpinners();
        saveButton.setOnClickListener(v -> saveGoal());
    }

    private void setupSpinners() {
        // Type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.activity_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        // Frequency spinner
        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(
                this, R.array.frequencies, android.R.layout.simple_spinner_item);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(frequencyAdapter);

        // Unit spinner
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
    }

    private void saveGoal() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String activityName = activityNameEditText.getText().toString().trim();
        String targetLimitStr = targetLimitEditText.getText().toString().trim();

        if (TextUtils.isEmpty(activityName)) {
            activityNameEditText.setError("Activity name is required");
            return;
        }

        if (TextUtils.isEmpty(targetLimitStr)) {
            targetLimitEditText.setError("Target limit is required");
            return;
        }

        try {
            double targetLimit = Double.parseDouble(targetLimitStr);
            String type = typeSpinner.getSelectedItem().toString();
            String frequency = frequencySpinner.getSelectedItem().toString();
            String unit = unitSpinner.getSelectedItem().toString();

            Goal goal = new Goal(firebaseUser.getUid(), activityName, targetLimit, type, frequency, unit);

            firebaseHelper.saveGoal(goal, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(CreateGoalActivity.this, "Goal created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateGoalActivity.this, "Failed to create goal", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            targetLimitEditText.setError("Please enter a valid number");
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

