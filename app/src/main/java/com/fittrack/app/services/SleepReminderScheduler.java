package com.fittrack.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class SleepReminderScheduler {

    private static final String TAG = "SleepReminderScheduler";

    public static final int SLEEP_BEDTIME_REQUEST_CODE = 5001;
    public static final int SLEEP_WINDDOWN_REQUEST_CODE = 5002;

    public static final String ACTION_SLEEP_BEDTIME = "com.fittrack.app.ACTION_SLEEP_BEDTIME";
    public static final String ACTION_SLEEP_WINDDOWN = "com.fittrack.app.ACTION_SLEEP_WINDDOWN";

    public static void scheduleSleepReminders(
            Context context,
            int bedtimeHour,
            int bedtimeMinute,
            boolean windDownEnabled,
            int windDownLeadMinutes
    ) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        // 1. Bedtime Alarm
        Intent bedtimeIntent = new Intent(context, ReminderReceiver.class);
        bedtimeIntent.setAction(ACTION_SLEEP_BEDTIME);
        bedtimeIntent.putExtra("bedtimeHour", bedtimeHour);
        bedtimeIntent.putExtra("bedtimeMinute", bedtimeMinute);
        bedtimeIntent.putExtra("windDownEnabled", windDownEnabled);
        bedtimeIntent.putExtra("windDownLeadMinutes", windDownLeadMinutes);

        PendingIntent bedtimePendingIntent = PendingIntent.getBroadcast(
                context,
                SLEEP_BEDTIME_REQUEST_CODE,
                bedtimeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextBedtimeMillis = calculateNextBedtimeMillis(bedtimeHour, bedtimeMinute);
        setAlarm(alarmManager, nextBedtimeMillis, bedtimePendingIntent);
        Log.d(TAG, "Scheduled bedtime alarm for millis: " + nextBedtimeMillis);

        // 2. Wind-Down Alarm
        if (windDownEnabled && windDownLeadMinutes > 0) {
            Intent windDownIntent = new Intent(context, ReminderReceiver.class);
            windDownIntent.setAction(ACTION_SLEEP_WINDDOWN);
            windDownIntent.putExtra("bedtimeHour", bedtimeHour);
            windDownIntent.putExtra("bedtimeMinute", bedtimeMinute);
            windDownIntent.putExtra("windDownEnabled", windDownEnabled);
            windDownIntent.putExtra("windDownLeadMinutes", windDownLeadMinutes);

            PendingIntent windDownPendingIntent = PendingIntent.getBroadcast(
                    context,
                    SLEEP_WINDDOWN_REQUEST_CODE,
                    windDownIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long nextWindDownMillis = calculateNextWindDownMillis(bedtimeHour, bedtimeMinute, windDownLeadMinutes);
            setAlarm(alarmManager, nextWindDownMillis, windDownPendingIntent);
            Log.d(TAG, "Scheduled wind-down alarm for millis: " + nextWindDownMillis);
        } else {
            cancelWindDownAlarm(context);
        }
    }

    private static void setAlarm(AlarmManager alarmManager, long triggerMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    public static void cancelSleepReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        // Cancel Bedtime
        Intent bedtimeIntent = new Intent(context, ReminderReceiver.class);
        bedtimeIntent.setAction(ACTION_SLEEP_BEDTIME);
        PendingIntent bedtimePi = PendingIntent.getBroadcast(
                context, SLEEP_BEDTIME_REQUEST_CODE, bedtimeIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (bedtimePi != null) {
            alarmManager.cancel(bedtimePi);
            bedtimePi.cancel();
        }

        cancelWindDownAlarm(context);
    }

    private static void cancelWindDownAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent windDownIntent = new Intent(context, ReminderReceiver.class);
        windDownIntent.setAction(ACTION_SLEEP_WINDDOWN);
        PendingIntent windDownPi = PendingIntent.getBroadcast(
                context, SLEEP_WINDDOWN_REQUEST_CODE, windDownIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (windDownPi != null) {
            alarmManager.cancel(windDownPi);
            windDownPi.cancel();
        }
    }

    public static long calculateNextBedtimeMillis(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTimeInMillis();
    }

    public static long calculateNextWindDownMillis(int bedtimeHour, int bedtimeMinute, int leadMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, bedtimeHour);
        cal.set(Calendar.MINUTE, bedtimeMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.MINUTE, -leadMinutes);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal.getTimeInMillis();
    }
}
