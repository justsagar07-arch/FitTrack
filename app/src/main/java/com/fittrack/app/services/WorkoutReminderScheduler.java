package com.fittrack.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.fittrack.app.models.WorkoutSchedule;

import java.util.Calendar;
import java.util.List;

public class WorkoutReminderScheduler {

    private static final String TAG = "WorkoutReminderScheduler";
    public static final String ACTION_WORKOUT_REMINDER = "com.fittrack.app.ACTION_WORKOUT_REMINDER";

    public static void scheduleWorkoutReminder(Context context, WorkoutSchedule schedule) {
        if (schedule == null || !schedule.isEnabled() || schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_WORKOUT_REMINDER);
        intent.putExtra("schedule_id", schedule.getId());
        intent.putExtra("schedule_title", schedule.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getStableRequestCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerMillis = calculateNextTriggerMillis(schedule);

        Log.d(TAG, "Scheduling workout reminder for '" + schedule.getTitle() + "' at millis: " + nextTriggerMillis);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent);
        }
    }

    public static void cancelWorkoutReminder(Context context, WorkoutSchedule schedule) {
        if (schedule == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_WORKOUT_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.getStableRequestCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled workout reminder for schedule: " + schedule.getId());
        }
    }

    public static long calculateNextTriggerMillis(WorkoutSchedule schedule) {
        Calendar now = Calendar.getInstance();
        List<Integer> targetDays = schedule.getDaysOfWeek();

        Calendar candidate = Calendar.getInstance();
        candidate.set(Calendar.HOUR_OF_DAY, schedule.getHour());
        candidate.set(Calendar.MINUTE, schedule.getMinute());
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
            if (dayOffset > 0) {
                candidate.add(Calendar.DAY_OF_YEAR, 1);
            }

            int dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK); // 1 = Sun, 2 = Mon ...

            if (targetDays.contains(dayOfWeek)) {
                if (candidate.after(now)) {
                    return candidate.getTimeInMillis();
                }
            }
        }

        // Fallback: 24 hours from now
        Calendar fallback = Calendar.getInstance();
        fallback.add(Calendar.DAY_OF_YEAR, 1);
        return fallback.getTimeInMillis();
    }
}
