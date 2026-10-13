package com.fittrack.app.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.fittrack.app.R;
import com.fittrack.app.activities.MainActivity;
import com.fittrack.app.activities.RemindersActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    private static final String CHANNEL_WATER_ID = "fittrack_water_reminders";
    private static final String CHANNEL_WORKOUT_ID = "fittrack_workout_reminders";
    private static final String CHANNEL_MOVE_ID = "fittrack_move_reminders";
    private static final String CHANNEL_SLEEP_ID = "fittrack_sleep_reminders";
    private static final String CHANNEL_MEAL_ID = "fittrack_meal_reminders";
    private static final String CHANNEL_GOAL_ID = "fittrack_goal_reminders";

    private static final int NOTIFICATION_WATER_ID = 2001;
    private static final int NOTIFICATION_WORKOUT_ID = 1001;
    private static final int NOTIFICATION_MOVE_ID = 4001;
    private static final int NOTIFICATION_SLEEP_ID = 5001;
    private static final int NOTIFICATION_GOAL_ID = 7001;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "ReminderReceiver onReceive action: " + action);

        if (WaterReminderScheduler.ACTION_WATER_REMINDER.equals(action)) {
            handleWaterReminder(context, intent);
        } else if (WorkoutReminderScheduler.ACTION_WORKOUT_REMINDER.equals(action)) {
            handleWorkoutReminder(context, intent);
        } else if (MoveReminderScheduler.ACTION_MOVE_REMINDER.equals(action)) {
            handleMoveReminder(context, intent);
        } else if (SleepReminderScheduler.ACTION_SLEEP_BEDTIME.equals(action)) {
            handleSleepBedtimeReminder(context, intent);
        } else if (SleepReminderScheduler.ACTION_SLEEP_WINDDOWN.equals(action)) {
            handleSleepWindDownReminder(context, intent);
        } else if (MealReminderScheduler.ACTION_MEAL_BREAKFAST.equals(action)) {
            handleMealReminder(context, intent, "breakfast");
        } else if (MealReminderScheduler.ACTION_MEAL_LUNCH.equals(action)) {
            handleMealReminder(context, intent, "lunch");
        } else if (MealReminderScheduler.ACTION_MEAL_DINNER.equals(action)) {
            handleMealReminder(context, intent, "dinner");
        } else if (GoalReminderScheduler.ACTION_GOAL_REMINDER.equals(action)) {
            handleGoalReminder(context, intent);
        } else {
            handleGeneralWorkoutReminder(context);
        }
    }

    private void handleWaterReminder(Context context, Intent intent) {
        int startHour = intent.getIntExtra("startHour", 8);
        int startMinute = intent.getIntExtra("startMinute", 0);
        int endHour = intent.getIntExtra("endHour", 22);
        int endMinute = intent.getIntExtra("endMinute", 0);
        int intervalHours = intent.getIntExtra("intervalHours", 2);

        if (WaterReminderScheduler.isWithinActiveHours(startHour, startMinute, endHour, endMinute)) {
            showWaterNotification(context);
        }

        WaterReminderScheduler.scheduleWaterReminder(
                context, startHour, startMinute, endHour, endMinute, intervalHours
        );
    }

    private void showWaterNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_WATER_ID, "Water Intake Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders to drink water during your active hours.");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, RemindersActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 2001, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WATER_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Hydration Reminder 💧")
                .setContentText("Time for a glass of water! Tap to record your water intake.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_WATER_ID, builder.build());
    }

    private void handleWorkoutReminder(Context context, Intent intent) {
        String scheduleTitle = intent.getStringExtra("schedule_title");
        if (scheduleTitle == null || scheduleTitle.trim().isEmpty()) {
            scheduleTitle = "FitTrack Workout";
        }
        showWorkoutNotification(context, scheduleTitle);
    }

    private void showWorkoutNotification(Context context, String title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_WORKOUT_ID, "Workout Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Scheduled reminders for your workouts");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("open_tab", "workout");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, NOTIFICATION_WORKOUT_ID, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_WORKOUT_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title + " 🏋️")
                .setContentText("Time for your scheduled workout! Tap to start.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_WORKOUT_ID, builder.build());
    }

    private void handleMoveReminder(Context context, Intent intent) {
        int startHour = intent.getIntExtra("startHour", 9);
        int startMinute = intent.getIntExtra("startMinute", 0);
        int endHour = intent.getIntExtra("endHour", 18);
        int endMinute = intent.getIntExtra("endMinute", 0);
        int intervalMinutes = intent.getIntExtra("intervalMinutes", 60);

        if (MoveReminderScheduler.isWithinActiveHours(startHour, startMinute, endHour, endMinute)) {
            showMoveNotification(context);
        }

        MoveReminderScheduler.scheduleMoveReminder(
                context, startHour, startMinute, endHour, endMinute, intervalMinutes
        );
    }

    private void showMoveNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_MOVE_ID, "Move & Stretch Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Gentle breaks to stand, stretch, and walk during active hours.");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, RemindersActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 4001, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MOVE_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Time to Move & Stretch! 🚶")
                .setContentText("Take a 2-minute break to stand up, stretch your muscles, or walk around.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_MOVE_ID, builder.build());
    }

    private void handleSleepBedtimeReminder(Context context, Intent intent) {
        showSleepNotification(
                context,
                "Bedtime Reminder 😴",
                "It's bedtime! Get a good night's sleep for optimal physical recovery."
        );
        rescheduleSleep(context, intent);
    }

    private void handleSleepWindDownReminder(Context context, Intent intent) {
        showSleepNotification(
                context,
                "Time to Wind Down 🌙",
                "Prepare for rest! Turn off bright lights and put away screens."
        );
        rescheduleSleep(context, intent);
    }

    private void showSleepNotification(Context context, String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_SLEEP_ID, "Sleep & Wind-Down Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for bedtime and wind-down time.");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, RemindersActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 5001, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SLEEP_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_SLEEP_ID, builder.build());
    }

    private void rescheduleSleep(Context context, Intent intent) {
        int bedtimeHour = intent.getIntExtra("bedtimeHour", 22);
        int bedtimeMinute = intent.getIntExtra("bedtimeMinute", 30);
        boolean windDownEnabled = intent.getBooleanExtra("windDownEnabled", true);
        int leadMinutes = intent.getIntExtra("windDownLeadMinutes", 30);

        SleepReminderScheduler.scheduleSleepReminders(
                context, bedtimeHour, bedtimeMinute, windDownEnabled, leadMinutes
        );
    }

    private void handleMealReminder(Context context, Intent intent, String mealType) {
        String title;
        String message;
        int notificationId;

        if ("lunch".equalsIgnoreCase(mealType)) {
            title = "Lunch Time 🥗";
            message = "Time for a healthy lunch to recharge your energy!";
            notificationId = 6002;
        } else if ("dinner".equalsIgnoreCase(mealType)) {
            title = "Dinner Time 🍽️";
            message = "Time for a balanced dinner!";
            notificationId = 6003;
        } else {
            title = "Breakfast Time 🍳";
            message = "Time to log and enjoy a nutritious breakfast!";
            notificationId = 6001;
        }

        showMealNotification(context, title, message, notificationId);

        int hour = intent.getIntExtra("hour", 8);
        int minute = intent.getIntExtra("minute", 0);
        MealReminderScheduler.scheduleMealReminder(context, mealType, hour, minute, true);
    }

    private void showMealNotification(Context context, String title, String message, int notificationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_MEAL_ID, "Meal Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for healthy daily meal times.");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("open_tab", "diet");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MEAL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    private void handleGoalReminder(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("FitTrackReminders", Context.MODE_PRIVATE);

        boolean checkSteps = prefs.getBoolean("goal_remind_steps", true);
        boolean checkWorkout = prefs.getBoolean("goal_remind_workout", true);

        HealthDataManager healthDataManager = new HealthDataManager(context);
        healthDataManager.getTodayHealthData((actualSteps, calories, heartRate) -> {
            int stepGoal = 8000;

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String todayKey = DATE_FORMAT.format(new Date());

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("workoutHistory")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            int todayWorkouts = 0;
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                if (Boolean.TRUE.equals(doc.getBoolean("completed"))) {
                                    Object timestampObj = doc.get("completedAt");
                                    if (timestampObj instanceof Timestamp) {
                                        String dateKey = DATE_FORMAT.format(((Timestamp) timestampObj).toDate());
                                        if (todayKey.equals(dateKey)) {
                                            todayWorkouts++;
                                        }
                                    }
                                }
                            }

                            evaluateGoalsAndNotify(context, actualSteps, stepGoal, todayWorkouts, 1, checkSteps, checkWorkout);
                        })
                        .addOnFailureListener(error -> evaluateGoalsAndNotify(context, actualSteps, stepGoal, 0, 1, checkSteps, checkWorkout));
            } else {
                evaluateGoalsAndNotify(context, actualSteps, stepGoal, 0, 1, checkSteps, checkWorkout);
            }
        });

        int hour = intent.getIntExtra("hour", 19);
        int minute = intent.getIntExtra("minute", 0);
        GoalReminderScheduler.scheduleGoalReminder(context, hour, minute, true);
    }

    private void evaluateGoalsAndNotify(
            Context context,
            int actualSteps,
            int stepGoal,
            int todayWorkouts,
            int workoutGoal,
            boolean checkSteps,
            boolean checkWorkout
    ) {
        boolean stepsDone = !checkSteps || (actualSteps >= stepGoal);
        boolean workoutDone = !checkWorkout || (todayWorkouts >= workoutGoal);

        if (stepsDone && workoutDone) {
            showGoalNotification(
                    context,
                    "All Goals Completed! 🎉",
                    "Awesome job! You reached all your daily targets today!"
            );
        } else {
            StringBuilder sb = new StringBuilder("Progress today: ");
            if (checkSteps && !stepsDone) {
                sb.append(String.format(Locale.getDefault(), "%d/%d steps", actualSteps, stepGoal));
                if (checkWorkout && !workoutDone) {
                    sb.append(", ");
                }
            }
            if (checkWorkout && !workoutDone) {
                sb.append(String.format(Locale.getDefault(), "%d/%d workouts", todayWorkouts, workoutGoal));
            }
            sb.append(". Keep going to reach your daily targets!");

            showGoalNotification(context, "Daily Goal Progress 🎯", sb.toString());
        }
    }

    private void showGoalNotification(Context context, String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_GOAL_ID, "Daily Goal Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for your daily step and workout targets.");
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, NOTIFICATION_GOAL_ID, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GOAL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_GOAL_ID, builder.build());
    }

    private void handleGeneralWorkoutReminder(Context context) {
        showWorkoutNotification(context, "FitTrack Workout");
    }
}
