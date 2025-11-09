package com.example.usagetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class QuestionnaireWelcomeActivity extends AppCompatActivity {
    private Button getStartedButton;
    private String userId;
    private boolean isNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire_welcome);

        userId = getIntent().getStringExtra("USER_ID");
        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);

        getStartedButton = findViewById(R.id.getStartedButton);

        getStartedButton.setOnClickListener(v -> {
            // Navigate to QuestionnaireActivity (not GoalSelectionActivity)
            Intent intent = new Intent(QuestionnaireWelcomeActivity.this, QuestionnaireActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("IS_NEW_USER", isNewUser);
            startActivity(intent);
        });
    }
}