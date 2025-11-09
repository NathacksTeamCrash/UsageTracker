package com.example.usagetracker;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;


import java.util.Calendar;

public class ReminderActivity extends AppCompatActivity {

    private EditText editTextTime, editTextReminder;
    private int hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }


        setContentView(R.layout.activity_reminder);

        editTextTime = findViewById(R.id.editTextTime);
        editTextReminder = findViewById(R.id.editTextReminder);
        Button btnSetNotification = findViewById(R.id.btnSetNotification);

        editTextTime.setOnClickListener(v -> showTimePicker());

        btnSetNotification.setOnClickListener(v -> {
            String reminderText = editTextReminder.getText().toString();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            long triggerTime = calendar.getTimeInMillis();
            if (triggerTime < System.currentTimeMillis()) {
                // If the time is in the past, add one day
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                triggerTime = calendar.getTimeInMillis();
            }

            Intent intent = new Intent(ReminderActivity.this, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    ReminderActivity.this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );


            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        });

        Button btnTestNotification = findViewById(R.id.btnTestNotification);
        btnTestNotification.setOnClickListener(v -> {
            showInstantNotification();
        });

    }

    private void showInstantNotification() {
        String reminderText = "Is your fridge still running?";
        String channelId = "reminder_channel";

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android 8+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Reminder")
                .setContentText(reminderText)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }


    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
            hour = selectedHour;
            minute = selectedMinute;
            editTextTime.setText(String.format("%02d:%02d", hour, minute));
        }, currentHour, currentMinute, true);
        timePickerDialog.show();
    }
}
