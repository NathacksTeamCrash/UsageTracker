package com.example.usagetracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.usagetracker.models.User;
import com.example.usagetracker.utils.FirebaseHelper;

import java.util.Arrays;
import java.util.List;

public class QuestionnaireActivity extends AppCompatActivity {
    private EditText householdSizeEditText, appliancesEditText, waterUsageEditText, 
                     electricityUsageEditText, sustainabilityGoalEditText;
    private Button submitButton;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        householdSizeEditText = findViewById(R.id.householdSizeEditText);
        appliancesEditText = findViewById(R.id.appliancesEditText);
        waterUsageEditText = findViewById(R.id.waterUsageEditText);
        electricityUsageEditText = findViewById(R.id.electricityUsageEditText);
        sustainabilityGoalEditText = findViewById(R.id.sustainabilityGoalEditText);
        submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(v -> submitQuestionnaire());
    }

    private void submitQuestionnaire() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String householdSizeStr = householdSizeEditText.getText().toString().trim();
        String appliancesStr = appliancesEditText.getText().toString().trim();
        String waterUsageStr = waterUsageEditText.getText().toString().trim();
        String electricityUsageStr = electricityUsageEditText.getText().toString().trim();
        String sustainabilityGoal = sustainabilityGoalEditText.getText().toString().trim();

        if (TextUtils.isEmpty(householdSizeStr)) {
            householdSizeEditText.setError("Household size is required");
            return;
        }

        if (TextUtils.isEmpty(waterUsageStr)) {
            waterUsageEditText.setError("Previous month water usage is required");
            return;
        }

        if (TextUtils.isEmpty(electricityUsageStr)) {
            electricityUsageEditText.setError("Previous month electricity usage is required");
            return;
        }

        try {
            int householdSize = Integer.parseInt(householdSizeStr);
            double waterUsage = Double.parseDouble(waterUsageStr);
            double electricityUsage = Double.parseDouble(electricityUsageStr);

            List<String> appliances = Arrays.asList(appliancesStr.split(","));
            for (int i = 0; i < appliances.size(); i++) {
                appliances.set(i, appliances.get(i).trim());
            }

            // Get current user data and update it
            firebaseHelper.getUser(firebaseUser.getUid(), task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    User user = firebaseHelper.documentToUser(task.getResult());
                    user.setHouseholdSize(householdSize);
                    user.setMajorAppliances(appliances);
                    user.setPreviousMonthWaterUsage(waterUsage);
                    user.setPreviousMonthElectricityUsage(electricityUsage);
                    user.setSustainabilityGoal(sustainabilityGoal);
                    user.setHasCompletedQuestionnaire(true);

                    firebaseHelper.saveUser(user, task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(QuestionnaireActivity.this, "Questionnaire submitted successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(QuestionnaireActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(QuestionnaireActivity.this, "Failed to save questionnaire", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Create new user if document doesn't exist
                    User user = new User(firebaseUser.getUid(), firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User", firebaseUser.getEmail());
                    user.setHouseholdSize(householdSize);
                    user.setMajorAppliances(appliances);
                    user.setPreviousMonthWaterUsage(waterUsage);
                    user.setPreviousMonthElectricityUsage(electricityUsage);
                    user.setSustainabilityGoal(sustainabilityGoal);
                    user.setHasCompletedQuestionnaire(true);

                    firebaseHelper.saveUser(user, task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(QuestionnaireActivity.this, "Questionnaire submitted successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(QuestionnaireActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(QuestionnaireActivity.this, "Failed to save questionnaire", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}

