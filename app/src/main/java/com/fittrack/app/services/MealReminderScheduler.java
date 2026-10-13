package com.fittrack.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class MealReminderScheduler {

    private static final String TAG = "MealReminderScheduler";

    public static final int MEAL_BREAKFAST_REQUEST_CODE = 6001;
    public static final int MEAL_LUNCH_REQUEST_CODE = 6002;
    public static final int MEAL_DINNER_REQUEST_CODE = 6003;

    public static final String ACTION_MEAL_BREAKFAST = "com.fittrack.app.ACTION_MEAL_BREAKFAST";
    public static final String ACTION_MEAL_LUNCH = "com.fittrack.app.ACTION_MEAL_LUNCH";
    public static final String ACTION_MEAL_DINNER = "com.fittrack.app.ACTION_MEAL_DINNER";

    public static void scheduleMealReminder(Context context, String mealType, int hour, int minute, boolean enabled) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        int requestCode = getRequestCodeForMeal(mealType);
        String action = getActionForMeal(mealType);

        if (!enabled) {
            cancelMealReminder(context, mealType);
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(action);
        intent.putExtra("mealType", mealType);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerMillis = calculateNextMealMillis(hour, minute);
        Log.d(TAG, "Scheduling " + mealType + " meal reminder for millis: " + nextTriggerMillis);

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

    public static void cancelMealReminder(Context context, String mealType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        int requestCode = getRequestCodeForMeal(mealType);
        String action = getActionForMeal(mealType);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled " + mealType + " meal reminder");
        }
    }

    public static long calculateNextMealMillis(int hour, int minute) {
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

    private static int getRequestCodeForMeal(String mealType) {
        if ("lunch".equalsIgnoreCase(mealType)) {
            return MEAL_LUNCH_REQUEST_CODE;
        } else if ("dinner".equalsIgnoreCase(mealType)) {
            return MEAL_DINNER_REQUEST_CODE;
        } else {
            return MEAL_BREAKFAST_REQUEST_CODE;
        }
    }

    private static String getActionForMeal(String mealType) {
        if ("lunch".equalsIgnoreCase(mealType)) {
            return ACTION_MEAL_LUNCH;
        } else if ("dinner".equalsIgnoreCase(mealType)) {
            return ACTION_MEAL_DINNER;
        } else {
            return ACTION_MEAL_BREAKFAST;
        }
    }
}
