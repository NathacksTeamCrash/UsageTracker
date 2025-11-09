package com.example.usagetracker.models;

import java.util.List;

public class User {
    private String userId;
    private String name;
    private String email;
    private int householdSize;
    private List<String> majorAppliances;
    private double previousMonthWaterUsage; // liters
    private double previousMonthElectricityUsage; // kWh
    private String sustainabilityGoal;
    private int ecoPoints;
    private int currentStreak;
    private boolean hasCompletedQuestionnaire;
    private long lastLoginDate;

    public User() {
        // Default constructor required for Firestore
    }

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.ecoPoints = 0;
        this.currentStreak = 0;
        this.hasCompletedQuestionnaire = false;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getHouseholdSize() {
        return householdSize;
    }

    public void setHouseholdSize(int householdSize) {
        this.householdSize = householdSize;
    }

    public List<String> getMajorAppliances() {
        return majorAppliances;
    }

    public void setMajorAppliances(List<String> majorAppliances) {
        this.majorAppliances = majorAppliances;
    }

    public double getPreviousMonthWaterUsage() {
        return previousMonthWaterUsage;
    }

    public void setPreviousMonthWaterUsage(double previousMonthWaterUsage) {
        this.previousMonthWaterUsage = previousMonthWaterUsage;
    }

    public double getPreviousMonthElectricityUsage() {
        return previousMonthElectricityUsage;
    }

    public void setPreviousMonthElectricityUsage(double previousMonthElectricityUsage) {
        this.previousMonthElectricityUsage = previousMonthElectricityUsage;
    }

    public String getSustainabilityGoal() {
        return sustainabilityGoal;
    }

    public void setSustainabilityGoal(String sustainabilityGoal) {
        this.sustainabilityGoal = sustainabilityGoal;
    }

    public int getEcoPoints() {
        return ecoPoints;
    }

    public void setEcoPoints(int ecoPoints) {
        this.ecoPoints = ecoPoints;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public boolean isHasCompletedQuestionnaire() {
        return hasCompletedQuestionnaire;
    }

    public void setHasCompletedQuestionnaire(boolean hasCompletedQuestionnaire) {
        this.hasCompletedQuestionnaire = hasCompletedQuestionnaire;
    }

    public long getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(long lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
}

