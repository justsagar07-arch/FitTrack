package com.fittrack.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class WaterReminderScheduler {

    private static final String TAG = "WaterReminderScheduler";
    public static final int WATER_REMINDER_REQUEST_CODE = 2001;
    public static final String ACTION_WATER_REMINDER = "com.fittrack.app.ACTION_WATER_REMINDER";

    public static void scheduleWaterReminder(
            Context context,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int intervalHours
    ) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_WATER_REMINDER);
        intent.putExtra("startHour", startHour);
        intent.putExtra("startMinute", startMinute);
        intent.putExtra("endHour", endHour);
        intent.putExtra("endMinute", endMinute);
        intent.putExtra("intervalHours", intervalHours);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                WATER_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerMillis = calculateNextTriggerMillis(
                startHour, startMinute, endHour, endMinute, intervalHours
        );

        Log.d(TAG, "Scheduling water reminder for millis: " + nextTriggerMillis);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                );
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis,
                    pendingIntent
            );
        }
    }

    public static void cancelWaterReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_WATER_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                WATER_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled water reminder");
        }
    }

    public static long calculateNextTriggerMillis(
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int intervalHours
    ) {
        Calendar now = Calendar.getInstance();

        Calendar startToday = Calendar.getInstance();
        startToday.set(Calendar.HOUR_OF_DAY, startHour);
        startToday.set(Calendar.MINUTE, startMinute);
        startToday.set(Calendar.SECOND, 0);
        startToday.set(Calendar.MILLISECOND, 0);

        Calendar endToday = Calendar.getInstance();
        endToday.set(Calendar.HOUR_OF_DAY, endHour);
        endToday.set(Calendar.MINUTE, endMinute);
        endToday.set(Calendar.SECOND, 0);
        endToday.set(Calendar.MILLISECOND, 0);

        // If end time is configured earlier than start time (overnight active hours)
        if (endToday.before(startToday)) {
            endToday.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (now.before(startToday)) {
            // Before active hours today -> trigger at start time today
            return startToday.getTimeInMillis();
        } else if (now.after(endToday)) {
            // After active hours today -> trigger at start time tomorrow
            Calendar startTomorrow = (Calendar) startToday.clone();
            startTomorrow.add(Calendar.DAY_OF_YEAR, 1);
            return startTomorrow.getTimeInMillis();
        } else {
            // Currently within active hours -> trigger after intervalHours
            Calendar nextInterval = (Calendar) now.clone();
            nextInterval.add(Calendar.HOUR_OF_DAY, intervalHours);

            if (nextInterval.after(endToday)) {
                // If interval pushes past active end time today -> start time tomorrow
                Calendar startTomorrow = (Calendar) startToday.clone();
                startTomorrow.add(Calendar.DAY_OF_YEAR, 1);
                return startTomorrow.getTimeInMillis();
            } else {
                return nextInterval.getTimeInMillis();
            }
        }
    }

    public static boolean isWithinActiveHours(
            int startHour,
            int startMinute,
            int endHour,
            int endMinute
    ) {
        Calendar now = Calendar.getInstance();

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, startHour);
        start.set(Calendar.MINUTE, startMinute);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, endHour);
        end.set(Calendar.MINUTE, endMinute);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        if (end.before(start)) {
            end.add(Calendar.DAY_OF_YEAR, 1);
        }

        return !now.before(start) && !now.after(end);
    }
}
