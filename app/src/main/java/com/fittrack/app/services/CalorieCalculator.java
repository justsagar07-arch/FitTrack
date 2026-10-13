package com.fittrack.app.services;

public class CalorieCalculator {

    private static final double CALORIES_PER_STEP = 0.04;

    private CalorieCalculator() {
        // Utility class
    }

    public static int calculateCalories(long steps) {

        if (steps <= 0) {
            return 0;
        }

        double calories =
                steps * CALORIES_PER_STEP;

        return (int) Math.round(calories);
    }
}