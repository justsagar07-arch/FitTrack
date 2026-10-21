package com.fittrack.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class UnitUtils {

    public static final String PREFS_UNITS = "FitTrackUnitPrefs";
    public static final String KEY_WEIGHT_UNIT = "weight_unit";
    public static final String KEY_HEIGHT_UNIT = "height_unit";

    public static final String UNIT_KG = "kg";
    public static final String UNIT_LBS = "lbs";
    public static final String UNIT_CM = "cm";
    public static final String UNIT_FT = "ft";

    public static String getWeightUnit(Context context) {
        if (context == null) return UNIT_KG;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_UNITS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WEIGHT_UNIT, UNIT_KG);
    }

    public static String getHeightUnit(Context context) {
        if (context == null) return UNIT_CM;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_UNITS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_HEIGHT_UNIT, UNIT_CM);
    }

    public static boolean isWeightLbs(Context context) {
        return UNIT_LBS.equalsIgnoreCase(getWeightUnit(context));
    }

    public static boolean isHeightFt(Context context) {
        return UNIT_FT.equalsIgnoreCase(getHeightUnit(context));
    }

    // Weight conversions (stored as kg in Firestore)
    public static double kgToLbs(double kg) {
        return kg * 2.20462;
    }

    public static double lbsToKg(double lbs) {
        return lbs / 2.20462;
    }

    public static String formatWeight(Context context, Double weightKg) {
        if (weightKg == null || weightKg <= 0) {
            return isWeightLbs(context) ? "-- lbs" : "-- kg";
        }
        if (isWeightLbs(context)) {
            double lbs = kgToLbs(weightKg);
            return String.format(Locale.getDefault(), "%.1f lbs", lbs);
        } else {
            return String.format(Locale.getDefault(), "%.1f kg", weightKg);
        }
    }

    // Height conversions (stored as cm in Firestore)
    public static double cmToInches(double cm) {
        return cm / 2.54;
    }

    public static double cmToFeet(double cm) {
        return cm / 30.48;
    }

    public static double feetToCm(double feet) {
        return feet * 30.48;
    }

    public static String formatHeight(Context context, Double heightCm) {
        if (heightCm == null || heightCm <= 0) {
            return isHeightFt(context) ? "-- ft" : "-- cm";
        }
        if (isHeightFt(context)) {
            double totalInches = cmToInches(heightCm);
            int feet = (int) (totalInches / 12);
            int inches = (int) Math.round(totalInches % 12);
            if (inches >= 12) {
                feet += 1;
                inches = 0;
            }
            if (feet <= 0 && inches > 0) {
                return String.format(Locale.getDefault(), "%d in", inches);
            }
            return String.format(Locale.getDefault(), "%d ft %d in", feet, inches);
        } else {
            return String.format(Locale.getDefault(), "%.0f cm", heightCm);
        }
    }
}
