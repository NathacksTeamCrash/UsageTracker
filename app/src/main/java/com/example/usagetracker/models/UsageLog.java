package com.example.usagetracker.models;

public class UsageLog {
    private String logId;
    private String userId;
    private String goalId;
    private String activityName;
    private double usageAmount;
    private String type; // "Electric" or "Water"
    private long timestamp;
    private int ecoPointsEarned;
    private boolean metGoal;

    public UsageLog() {
        // Default constructor required for Firestore
    }

    public UsageLog(String userId, String goalId, String activityName, double usageAmount, String type) {
        this.userId = userId;
        this.goalId = goalId;
        this.activityName = activityName;
        this.usageAmount = usageAmount;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.ecoPointsEarned = 0;
        this.metGoal = false;
    }

    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public double getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(double usageAmount) {
        this.usageAmount = usageAmount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getEcoPointsEarned() {
        return ecoPointsEarned;
    }

    public void setEcoPointsEarned(int ecoPointsEarned) {
        this.ecoPointsEarned = ecoPointsEarned;
    }

    public boolean isMetGoal() {
        return metGoal;
    }

    public void setMetGoal(boolean metGoal) {
        this.metGoal = metGoal;
    }
}

