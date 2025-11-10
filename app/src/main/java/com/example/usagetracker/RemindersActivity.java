package com.example.usagetracker;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import java.util.Map;

public class RemindersActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private RadioGroup frequencyGroup;
    private Button btnSet, btnTest;
    private EditText editTextReminder;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        timePicker = findViewById(R.id.timePicker);
        frequencyGroup = findViewById(R.id.radioGroupFrequency);
        btnSet = findViewById(R.id.btnSetNotification);
        editTextReminder = findViewById(R.id.editTextReminder);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                 ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                 : "anonymous";

        btnSet.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            int selectedId = frequencyGroup.getCheckedRadioButtonId();
            String frequency = (selectedId == R.id.radioDaily) ? "Daily" : "Weekly";
            String message = editTextReminder.getText().toString();

            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a reminder message", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> reminderData = new HashMap<>();
            reminderData.put("message", message);
            reminderData.put("frequency", frequency);
            reminderData.put("hour", hour);
            reminderData.put("minute", minute);
            reminderData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(reminderData)
                .addOnSuccessListener(documentReference ->
                    Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to save reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        });

    }
}