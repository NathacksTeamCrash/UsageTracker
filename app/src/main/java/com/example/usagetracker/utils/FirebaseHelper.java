package com.example.usagetracker.utils;

import android.util.Log;

import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;
import com.example.usagetracker.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // User operations
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        if (user == null || user.getUserId() == null) {
            Log.e(TAG, "Cannot save user: user or userId is null");
            return;
        }
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("name", user.getName() != null ? user.getName() : "");
        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("householdSize", user.getHouseholdSize());
        userMap.put("majorAppliances", user.getMajorAppliances() != null ? user.getMajorAppliances() : new ArrayList<String>());
        userMap.put("previousMonthWaterUsage", user.getPreviousMonthWaterUsage());
        userMap.put("previousMonthElectricityUsage", user.getPreviousMonthElectricityUsage());
        userMap.put("selectedGoals", user.getSelectedGoals() != null ? user.getSelectedGoals() : new ArrayList<String>());
        userMap.put("ecoPoints", user.getEcoPoints());
        userMap.put("currentStreak", user.getCurrentStreak());
        userMap.put("hasCompletedQuestionnaire", user.isHasCompletedQuestionnaire());
        userMap.put("lastLoginDate", user.getLastLoginDate());

        Log.d(TAG, "Saving user to Firestore: " + user.getUserId());
        db.collection("users").document(user.getUserId())
                .set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User saved successfully to Firestore");
                    } else {
                        Log.e(TAG, "Error saving user to Firestore", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    public void getUser(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void updateUserEcoPoints(String userId, int newPoints, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId)
                .update("ecoPoints", newPoints)
                .addOnCompleteListener(listener);
    }

    public void updateUserStreak(String userId, int newStreak, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId)
                .update("currentStreak", newStreak)
                .addOnCompleteListener(listener);
    }

    // Goal operations
    public void saveGoal(Goal goal, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> goalMap = new HashMap<>();
        goalMap.put("userId", goal.getUserId());
        goalMap.put("activityName", goal.getActivityName());
        goalMap.put("targetLimit", goal.getTargetLimit());
        goalMap.put("type", goal.getType());
        goalMap.put("frequency", goal.getFrequency());
        goalMap.put("unit", goal.getUnit());
        goalMap.put("createdAt", goal.getCreatedAt());

        db.collection("goals")
                .add(goalMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        goal.setGoalId(task.getResult().getId());
                    }
                    listener.onComplete(task);
                });
    }

    public void getGoals(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("goals")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    public void deleteGoal(String goalId, OnCompleteListener<Void> listener) {
        db.collection("goals").document(goalId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // Usage Log operations
    public void saveUsageLog(UsageLog log, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("userId", log.getUserId());
        logMap.put("goalId", log.getGoalId());
        logMap.put("activityName", log.getActivityName());
        logMap.put("usageAmount", log.getUsageAmount());
        logMap.put("type", log.getType());
        logMap.put("timestamp", log.getTimestamp());
        logMap.put("ecoPointsEarned", log.getEcoPointsEarned());
        logMap.put("metGoal", log.isMetGoal());

        db.collection("usageLogs")
                .add(logMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        log.setLogId(task.getResult().getId());
                    }
                    listener.onComplete(task);
                });
    }

    public void getUsageLogs(String userId, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("usageLogs")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    public void getUsageLogsForMonth(String userId, long startTimestamp, long endTimestamp, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("usageLogs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .get()
                .addOnCompleteListener(listener);
    }

    // Leaderboard operations
    public void getTopUsers(int limit, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("users")
                .orderBy("ecoPoints", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(listener);
    }

    // Helper method to convert DocumentSnapshot to User
    public User documentToUser(DocumentSnapshot document) {
        User user = new User();
        user.setUserId(document.getString("userId"));
        user.setName(document.getString("name"));
        user.setEmail(document.getString("email"));
        user.setHouseholdSize(document.getLong("householdSize") != null ? document.getLong("householdSize").intValue() : 0);
        
        // Safely handle List conversion
        Object appliancesObj = document.get("majorAppliances");
        if (appliancesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> appliances = (List<String>) appliancesObj;
            user.setMajorAppliances(appliances);
        } else {
            user.setMajorAppliances(new ArrayList<>());
        }
        
        user.setPreviousMonthWaterUsage(document.getDouble("previousMonthWaterUsage") != null ? document.getDouble("previousMonthWaterUsage") : 0.0);
        user.setPreviousMonthElectricityUsage(document.getDouble("previousMonthElectricityUsage") != null ? document.getDouble("previousMonthElectricityUsage") : 0.0);
        
        // Handle selectedGoals list
        Object goalsObj = document.get("selectedGoals");
        if (goalsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> goals = (List<String>) goalsObj;
            user.setSelectedGoals(goals);
        } else {
            user.setSelectedGoals(new ArrayList<>());
        }
        
        user.setEcoPoints(document.getLong("ecoPoints") != null ? document.getLong("ecoPoints").intValue() : 0);
        user.setCurrentStreak(document.getLong("currentStreak") != null ? document.getLong("currentStreak").intValue() : 0);
        user.setHasCompletedQuestionnaire(document.getBoolean("hasCompletedQuestionnaire") != null ? document.getBoolean("hasCompletedQuestionnaire") : false);
        user.setLastLoginDate(document.getLong("lastLoginDate") != null ? document.getLong("lastLoginDate") : 0);
        return user;
    }

    // Helper method to convert DocumentSnapshot to Goal
    public Goal documentToGoal(DocumentSnapshot document) {
        Goal goal = new Goal();
        goal.setGoalId(document.getId());
        goal.setUserId(document.getString("userId"));
        goal.setActivityName(document.getString("activityName"));
        goal.setTargetLimit(document.getDouble("targetLimit") != null ? document.getDouble("targetLimit") : 0.0);
        goal.setType(document.getString("type"));
        goal.setFrequency(document.getString("frequency"));
        goal.setUnit(document.getString("unit"));
        goal.setCreatedAt(document.getLong("createdAt") != null ? document.getLong("createdAt") : 0);
        return goal;
    }

    // Helper method to convert DocumentSnapshot to UsageLog
    public UsageLog documentToUsageLog(DocumentSnapshot document) {
        UsageLog log = new UsageLog();
        log.setLogId(document.getId());
        log.setUserId(document.getString("userId"));
        log.setGoalId(document.getString("goalId"));
        log.setActivityName(document.getString("activityName"));
        log.setUsageAmount(document.getDouble("usageAmount") != null ? document.getDouble("usageAmount") : 0.0);
        log.setType(document.getString("type"));
        log.setTimestamp(document.getLong("timestamp") != null ? document.getLong("timestamp") : 0);
        log.setEcoPointsEarned(document.getLong("ecoPointsEarned") != null ? document.getLong("ecoPointsEarned").intValue() : 0);
        log.setMetGoal(document.getBoolean("metGoal") != null ? document.getBoolean("metGoal") : false);
        return log;
    }
}

