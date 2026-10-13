package com.fittrack.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.fittrack.app.models.WorkoutSchedule;

import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "FitTrackBootReceiver";
    private static final String PREFS_NAME = "FitTrackReminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "Device rebooted. Re-scheduling active reminders...");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1. Water Reminder
        boolean waterEnabled = prefs.getBoolean("water_enabled", false);
        if (waterEnabled) {
            int startHour = prefs.getInt("water_start_hour", 8);
            int startMinute = prefs.getInt("water_start_minute", 0);
            int endHour = prefs.getInt("water_end_hour", 22);
            int endMinute = prefs.getInt("water_end_minute", 0);
            int intervalHours = prefs.getInt("water_interval_hours", 2);

            WaterReminderScheduler.scheduleWaterReminder(
                    context, startHour, startMinute, endHour, endMinute, intervalHours
            );
        }

        // 2. Workout Schedules
        int scheduleCount = prefs.getInt("workout_schedule_count", 0);
        for (int i = 0; i < scheduleCount; i++) {
            boolean enabled = prefs.getBoolean("ws_enabled_" + i, false);
            if (!enabled) continue;

            String id = prefs.getString("ws_id_" + i, "ws_" + i);
            String title = prefs.getString("ws_title_" + i, "Workout");
            int hour = prefs.getInt("ws_hour_" + i, 8);
            int minute = prefs.getInt("ws_minute_" + i, 0);

            String daysStr = prefs.getString("ws_days_" + i, "1,2,3,4,5,6,7");
            List<Integer> days = new ArrayList<>();
            for (String s : daysStr.split(",")) {
                try {
                    days.add(Integer.parseInt(s.trim()));
                } catch (Exception ignored) {
                }
            }

            WorkoutSchedule schedule = new WorkoutSchedule(id, title, hour, minute, days, enabled);
            WorkoutReminderScheduler.scheduleWorkoutReminder(context, schedule);
        }

        // 3. Move & Stretch
        boolean moveEnabled = prefs.getBoolean("move_enabled", false);
        if (moveEnabled) {
            int startHour = prefs.getInt("move_start_hour", 9);
            int startMinute = prefs.getInt("move_start_minute", 0);
            int endHour = prefs.getInt("move_end_hour", 18);
            int endMinute = prefs.getInt("move_end_minute", 0);
            int intervalMinutes = prefs.getInt("move_interval_minutes", 60);

            MoveReminderScheduler.scheduleMoveReminder(
                    context, startHour, startMinute, endHour, endMinute, intervalMinutes
            );
        }

        // 4. Sleep & Wind-Down
        boolean sleepEnabled = prefs.getBoolean("sleep_enabled", false);
        if (sleepEnabled) {
            int bedtimeHour = prefs.getInt("sleep_bedtime_hour", 22);
            int bedtimeMinute = prefs.getInt("sleep_bedtime_minute", 30);
            boolean windDownEnabled = prefs.getBoolean("sleep_winddown_enabled", true);
            int leadMinutes = prefs.getInt("sleep_winddown_lead_minutes", 30);

            SleepReminderScheduler.scheduleSleepReminders(
                    context, bedtimeHour, bedtimeMinute, windDownEnabled, leadMinutes
            );
        }

        // 5. Meals
        String[] mealTypes = {"breakfast", "lunch", "dinner"};
        int[] defaultHours = {8, 13, 19};
        int[] defaultMins = {0, 0, 30};

        for (int i = 0; i < mealTypes.length; i++) {
            String m = mealTypes[i];
            boolean enabled = prefs.getBoolean("meal_" + m + "_enabled", false);
            if (enabled) {
                int h = prefs.getInt("meal_" + m + "_hour", defaultHours[i]);
                int min = prefs.getInt("meal_" + m + "_min", defaultMins[i]);
                MealReminderScheduler.scheduleMealReminder(context, m, h, min, true);
            }
        }

        // 6. Daily Goals
        boolean goalEnabled = prefs.getBoolean("goal_reminder_enabled", false);
        if (goalEnabled) {
            int hour = prefs.getInt("goal_reminder_hour", 19);
            int min = prefs.getInt("goal_reminder_minute", 0);
            GoalReminderScheduler.scheduleGoalReminder(context, hour, min, true);
        }
    }
}
