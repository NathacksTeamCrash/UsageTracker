package com.example.usagetracker.models;

/**
 * EcoPointsCalculator tracks and calculates eco-friendly behavior scores over time.
 * This class maintains a cumulative total score based on various environmental actions
 * including shower usage, device usage, lights left on, windows left open, and utility bills.
 * Scores are weighted differently depending on whether bill data is available.
 */
public class EcoPointsCalculator {
    // Cumulative total score that accumulates over multiple calculations
    private double total;

    // Current shower duration in minutes
    private double shower;
    // Target/desired shower duration in minutes
    private double desiredShower;
    // Current device usage time in hours
    private double device;
    // Target/desired device usage time in hours
    private double desiredDevice;
    // Number of lights left on unnecessarily
    private int lightsLeftOn;
    // Number of windows left open (when heating/cooling is on)
    private int windowsLeftOpen;
    // Previous month's utility bill amount
    private double previousBill;
    // Current month's utility bill amount
    private double currentBill;

    /**
     * Constructs a new EcoPointsCalculator with initial environmental metrics.
     *
     * @param shower Current shower duration in minutes
     * @param desiredShower Target shower duration in minutes
     * @param device Current device usage time in hours
     * @param desiredDevice Target device usage time in hours
     * @param lightsLeftOn Number of lights left on unnecessarily
     * @param windowsLeftOpen Number of windows left open when heating/cooling is on
     * @param previousBill Previous month's utility bill amount
     * @param currentBill Current month's utility bill amount
     */
    public EcoPointsCalculator(double shower, double desiredShower,
                               double device, double desiredDevice,
                               int lightsLeftOn, int windowsLeftOpen,
                               double previousBill, double currentBill) {
        // Initialize shower metrics
        this.shower = shower;
        this.desiredShower = desiredShower;
        // Initialize device usage metrics
        this.device = device;
        this.desiredDevice = desiredDevice;
        // Initialize lights and windows metrics
        this.lightsLeftOn = lightsLeftOn;
        this.windowsLeftOpen = windowsLeftOpen;
        // Initialize bill comparison metrics
        this.previousBill = previousBill;
        this.currentBill = currentBill;
        // Initialize cumulative total to zero
        this.total = 0;
    }

    /**
     * Calculates points for shower usage behavior.
     *
     * @param current Current shower duration
     * @param desired Target shower duration
     * @return 10 points if at or below target, -5 points if over target
     */
    public static int showerPoints(double current, double desired) {
        // Award 10 points for meeting goal, penalize 5 for exceeding
        return (current <= desired) ? 10 : -5;
    }

    /**
     * Calculates points for device usage behavior.
     *
     * @param current Current device usage time
     * @param desired Target device usage time
     * @return 10 points if at or below target, -5 points if over target
     */
    public static int devicePoints(double current, double desired) {
        // Award 10 points for meeting goal, penalize 5 for exceeding
        return (current <= desired) ? 10 : -5;
    }

    /**
     * Calculates points based on number of lights left on.
     *
     * @param lightsLeftOn Number of lights left on unnecessarily
     * @return 10 points for 0 lights, 5 points for 1-2 lights, -5 points for 3+ lights
     */
    public static int lightsPoints(int lightsLeftOn) {
        // Perfect: no lights left on
        if (lightsLeftOn == 0) return 10;
            // Good: only 1-2 lights left on
        else if (lightsLeftOn <= 2) return 5;
            // Poor: 3 or more lights left on
        else return -5;
    }

    /**
     * Calculates points based on number of windows left open.
     *
     * @param windowsLeftOpen Number of windows left open when heating/cooling is on
     * @return 10 points for 0 windows, -5 points for any windows left open
     */
    public static int windowsPoints(int windowsLeftOpen) {
        // Award 10 points if all windows closed, penalize if any are open
        return (windowsLeftOpen == 0) ? 10 : -5;
    }

    /**
     * Calculates points based on utility bill comparison.
     *
     * @param previousBill Previous month's bill amount
     * @param currentBill Current month's bill amount
     * @return 15 points for decrease, 0 points for no change, -10 points for increase
     */
    public static int billPoints(double previousBill, double currentBill) {
        // Reward bill reduction with 15 points
        if (currentBill < previousBill) return 15;
            // No change in bill gets no points
        else if (currentBill == previousBill) return 0;
            // Penalize bill increase with -10 points
        else return -10;
    }

    /**
     * Calculates the weighted eco score based on all environmental metrics
     * and adds it to the cumulative total. Uses different weighting depending
     * on whether bill data has changed (indicating a new billing period).
     *
     * @return The rounded cumulative total score after adding the current period's score
     */
    public int totalEcoScore() {
        // Calculate individual component scores
        int showerScore = showerPoints(shower, desiredShower);
        int deviceScore = devicePoints(device, desiredDevice);
        int lightScore = lightsPoints(lightsLeftOn);
        int windowScore = windowsPoints(windowsLeftOpen);
        int billScore = billPoints(previousBill, currentBill);

        // Variable to hold the weighted score for this calculation
        double scoreToAdd;

        // If bills differ, include bill score with 30% weight
        if (previousBill != currentBill) {
            scoreToAdd = showerScore * 0.2 +    // 20% weight for shower
                    deviceScore * 0.2 +      // 20% weight for device
                    lightScore * 0.2 +       // 20% weight for lights
                    windowScore * 0.10 +     // 10% weight for windows
                    billScore * 0.3;         // 30% weight for bill reduction
        } else {
            // If no bill change, redistribute weight among other metrics
            scoreToAdd = showerScore * 0.3 +    // 30% weight for shower
                    deviceScore * 0.3 +      // 30% weight for device
                    lightScore * 0.3 +       // 30% weight for lights
                    windowScore * 0.1;       // 10% weight for windows
        }

        // Add the calculated score to the running total
        this.total += scoreToAdd;
        // Return the rounded cumulative total as an integer
        return (int) Math.round(this.total);
    }

    /**
     * Updates the shower usage metrics.
     *
     * @param shower New current shower duration
     * @param desiredShower New target shower duration
     */
    public void updateShower(double shower, double desiredShower) {
        // Update shower duration values
        this.shower = shower;
        this.desiredShower = desiredShower;
    }

    /**
     * Updates the device usage metrics.
     *
     * @param device New current device usage time
     * @param desiredDevice New target device usage time
     */
    public void updateDevice(double device, double desiredDevice) {
        // Update device usage values
        this.device = device;
        this.desiredDevice = desiredDevice;
    }

    /**
     * Updates the number of lights left on.
     *
     * @param lightsLeftOn New number of lights left on
     */
    public void updateLights(int lightsLeftOn) {
        // Update lights left on count
        this.lightsLeftOn = lightsLeftOn;
    }

    /**
     * Updates the number of windows left open.
     *
     *
     */
    public void updateWindows(int windowsLeftOpen) {
        // Update windows left open count
        this.windowsLeftOpen = windowsLeftOpen;
    }

    /**
     * Updates the utility bill comparison values.
     *
     * @param previousBill New previous bill amount
     * @param currentBill New current bill amount
     */
    public void updateBills(double previousBill, double currentBill) {
        // Update bill comparison values
        this.previousBill = previousBill;
        this.currentBill = currentBill;
    }

    /**
     * Updates all environmental metrics at once.
     *
     * @param shower New current shower duration
     * @param desiredShower New target shower duration
     * @param device New current device usage time
     * @param desiredDevice New target device usage time
     * @param lightsLeftOn New number of lights left on
     * @param windowsLeftOpen New number of windows left open
     * @param previousBill New previous bill amount
     * @param currentBill New current bill amount
     */
    public void updateAll(double shower, double desiredShower,
                          double device, double desiredDevice,
                          int lightsLeftOn, int windowsLeftOpen,
                          double previousBill, double currentBill) {
        // Update all shower metrics
        this.shower = shower;
        this.desiredShower = desiredShower;
        // Update all device metrics
        this.device = device;
        this.desiredDevice = desiredDevice;
        // Update all lights and windows metrics
        this.lightsLeftOn = lightsLeftOn;
        this.windowsLeftOpen = windowsLeftOpen;
        // Update all bill metrics
        this.previousBill = previousBill;
        this.currentBill = currentBill;
    }

    /**
     * Gets the current cumulative total score.
     *
     * @return The current total eco score
     */
    public double getTotal() {
        // Return the cumulative total score
        return total;
    }

    /**
     * Resets the cumulative total score to zero.
     */
    public void resetTotal() {
        // Reset cumulative total back to zero
        this.total = 0;
    }
}