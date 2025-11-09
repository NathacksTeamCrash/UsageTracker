package com.example.usagetracker.utils;

import com.example.usagetracker.models.Goal;
import com.example.usagetracker.models.UsageLog;

public class GamificationHelper {
    private static final int BASE_POINTS = 10;
    private static final int STREAK_BONUS = 5;
    private static final int PERFECT_BONUS = 20;

    /**
     * Calculate eco-points based on goal achievement
     * @param usageAmount The actual usage amount
     * @param targetLimit The target limit from the goal
     * @return Points earned (0 if over target, base points if met, bonus if perfect)
     */
    public static int calculateEcoPoints(double usageAmount, double targetLimit) {
        if (usageAmount > targetLimit) {
            return 0; // No points if over target
        } else if (usageAmount == targetLimit) {
            return BASE_POINTS + PERFECT_BONUS; // Perfect match bonus
        } else {
            // Calculate points based on how much under target (more under = more points)
            double percentageUnder = ((targetLimit - usageAmount) / targetLimit) * 100;
            int points = BASE_POINTS;
            
            if (percentageUnder >= 20) {
                points += PERFECT_BONUS; // 20% or more under target
            } else if (percentageUnder >= 10) {
                points += 10; // 10-19% under target
            }
            
            return points;
        }
    }

    /**
     * Check if usage met the goal
     */
    public static boolean metGoal(double usageAmount, double targetLimit) {
        return usageAmount <= targetLimit;
    }

    /**
     * Calculate streak bonus points
     */
    public static int calculateStreakBonus(int currentStreak) {
        if (currentStreak >= 7) {
            return STREAK_BONUS * 3; // Week streak
        } else if (currentStreak >= 3) {
            return STREAK_BONUS * 2; // 3-day streak
        } else if (currentStreak > 0) {
            return STREAK_BONUS; // Daily streak
        }
        return 0;
    }

    /**
     * Update streak based on goal achievement
     */
    public static int updateStreak(int currentStreak, boolean metGoal) {
        if (metGoal) {
            return currentStreak + 1;
        } else {
            return 0; // Reset streak if goal not met
        }
    }
}

