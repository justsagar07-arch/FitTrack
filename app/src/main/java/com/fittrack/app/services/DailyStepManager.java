package com.fittrack.app.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DailyStepManager {

    private static final String PREF_NAME =
            "FitTrackDailySteps";

    private static final String KEY_DATE =
            "step_date";

    private static final String KEY_BASELINE =
            "step_baseline";

    private final SharedPreferences preferences;

    public DailyStepManager(Context context) {

        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    // =========================================================
    // GET TODAY DATE
    // =========================================================

    private String getTodayDate() {

        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());
    }

    // =========================================================
    // CALCULATE TODAY'S STEPS
    // =========================================================

    public synchronized long getTodaySteps(
            long sensorTotalSteps
    ) {

        String today =
                getTodayDate();

        String savedDate =
                preferences.getString(
                        KEY_DATE,
                        ""
                );

        long baseline =
                preferences.getLong(
                        KEY_BASELINE,
                        -1
                );

        // -----------------------------------------------------
        // FIRST TIME TODAY
        // -----------------------------------------------------

        if (!today.equals(savedDate)
                || baseline < 0) {

            preferences.edit()
                    .putString(
                            KEY_DATE,
                            today
                    )
                    .putLong(
                            KEY_BASELINE,
                            sensorTotalSteps
                    )
                    .apply();

            return 0;
        }

        // -----------------------------------------------------
        // PHONE RESTART / SENSOR RESET
        // -----------------------------------------------------

        if (sensorTotalSteps < baseline) {

            preferences.edit()
                    .putString(
                            KEY_DATE,
                            today
                    )
                    .putLong(
                            KEY_BASELINE,
                            sensorTotalSteps
                    )
                    .apply();

            return 0;
        }

        // -----------------------------------------------------
        // TODAY'S STEPS
        // -----------------------------------------------------

        return sensorTotalSteps - baseline;
    }

    // =========================================================
    // GET SAVED BASELINE
    // =========================================================

    public long getBaseline() {

        return preferences.getLong(
                KEY_BASELINE,
                -1
        );
    }

    // =========================================================
    // RESET
    // =========================================================

    public void reset() {

        preferences.edit()
                .remove(KEY_DATE)
                .remove(KEY_BASELINE)
                .apply();
    }
}