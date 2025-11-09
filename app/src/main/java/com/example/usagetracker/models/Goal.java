package com.example.usagetracker.models;

public class Goal {
    private String goalId;
    private String userId;
    private String activityName;
    private double targetLimit;
    private String type; // "Electric" or "Water"
    private String frequency; // "Daily" or "Weekly"
    private String unit; // "minutes", "hours", "liters", "kWh"
    private long createdAt;

    public Goal() {
        // Default constructor required for Firestore
    }

    public Goal(String userId, String activityName, double targetLimit, String type, String frequency, String unit) {
        this.userId = userId;
        this.activityName = activityName;
        this.targetLimit = targetLimit;
        this.type = type;
        this.frequency = frequency;
        this.unit = unit;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public double getTargetLimit() {
        return targetLimit;
    }

    public void setTargetLimit(double targetLimit) {
        this.targetLimit = targetLimit;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

