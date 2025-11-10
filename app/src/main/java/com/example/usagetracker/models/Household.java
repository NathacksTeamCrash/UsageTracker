package com.example.usagetracker.models;

import java.util.ArrayList;
import java.util.List;

public class Household {
    private String householdId;
    private int householdSize;
    private List<String> majorAppliances;
    private double previousMonthWaterUsage; // liters
    private double previousMonthElectricityUsage; // kWh
    private double currentMonthWaterUsage;
    private double currentMonthElectricityUsage;
    private List<String> residents; // Array of user IDs
    private long createdAt;
    private long updatedAt;

    public Household() {
        // Default constructor required for Firestore
        this.residents = new ArrayList<>();
        this.majorAppliances = new ArrayList<>();
        this.currentMonthWaterUsage = 0.0;
        this.currentMonthElectricityUsage = 0.0;
    }

    public Household(String householdId, int householdSize) {
        this.householdId = householdId;
        this.householdSize = householdSize;
        this.residents = new ArrayList<>();
        this.majorAppliances = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.currentMonthWaterUsage = 0.0;
        this.currentMonthElectricityUsage = 0.0;
    }

    // Getters and Setters
    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
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

    public double getCurrentMonthWaterUsage() {
        return currentMonthWaterUsage;
    }

    public void setCurrentMonthWaterUsage(double currentMonthWaterUsage) {
        this.currentMonthWaterUsage = currentMonthWaterUsage;
    }

    public double getCurrentMonthElectricityUsage() {
        return currentMonthElectricityUsage;
    }

    public void setCurrentMonthElectricityUsage(double currentMonthElectricityUsage) {
        this.currentMonthElectricityUsage = currentMonthElectricityUsage;
    }

    public List<String> getResidents() {
        return residents;
    }

    public void setResidents(List<String> residents) {
        this.residents = residents;
    }

    public void addResident(String userId) {
        if (this.residents == null) {
            this.residents = new ArrayList<>();
        }
        if (!this.residents.contains(userId)) {
            this.residents.add(userId);
        }
    }

    public void removeResident(String userId) {
        if (this.residents != null) {
            this.residents.remove(userId);
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}