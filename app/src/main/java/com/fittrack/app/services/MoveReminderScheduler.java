package com.fittrack.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class MoveReminderScheduler {

    private static final String TAG = "MoveReminderScheduler";
    public static final int MOVE_REMINDER_REQUEST_CODE = 4001;
    public static final String ACTION_MOVE_REMINDER = "com.fittrack.app.ACTION_MOVE_REMINDER";

    public static void scheduleMoveReminder(
            Context context,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int intervalMinutes
    ) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_MOVE_REMINDER);
        intent.putExtra("startHour", startHour);
        intent.putExtra("startMinute", startMinute);
        intent.putExtra("endHour", endHour);
        intent.putExtra("endMinute", endMinute);
        intent.putExtra("intervalMinutes", intervalMinutes);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                MOVE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerMillis = calculateNextTriggerMillis(
                startHour, startMinute, endHour, endMinute, intervalMinutes
        );

        Log.d(TAG, "Scheduling move & stretch reminder for millis: " + nextTriggerMillis);

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

    public static void cancelMoveReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_MOVE_REMINDER);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                MOVE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled move reminder");
        }
    }

    public static long calculateNextTriggerMillis(
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int intervalMinutes
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

        if (endToday.before(startToday)) {
            endToday.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (now.before(startToday)) {
            return startToday.getTimeInMillis();
        } else if (now.after(endToday)) {
            Calendar startTomorrow = (Calendar) startToday.clone();
            startTomorrow.add(Calendar.DAY_OF_YEAR, 1);
            return startTomorrow.getTimeInMillis();
        } else {
            Calendar nextInterval = (Calendar) now.clone();
            nextInterval.add(Calendar.MINUTE, intervalMinutes);

            if (nextInterval.after(endToday)) {
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
