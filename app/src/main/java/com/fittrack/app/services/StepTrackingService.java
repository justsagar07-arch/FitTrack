package com.fittrack.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fittrack.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StepTrackingService extends Service {

    private static final String TAG =
            "FitTrackStepService";

    private static final String CHANNEL_ID =
            "fittrack_step_tracking";

    private static final int NOTIFICATION_ID =
            1001;

    private StepSensorManager stepSensorManager;
    private DailyStepManager dailyStepManager;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(
                TAG,
                "StepTrackingService created"
        );

        stepSensorManager =
                new StepSensorManager(this);

        dailyStepManager =
                new DailyStepManager(this);

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        createNotificationChannel();

        startForeground(
                NOTIFICATION_ID,
                createNotification(
                        0,
                        0
                )
        );

        startStepTracking();
    }

    private void startStepTracking() {

        if (!stepSensorManager
                .isStepSensorAvailable()) {

            Log.e(
                    TAG,
                    "Step sensor unavailable"
            );

            return;
        }

        Log.d(
                TAG,
                "Starting background step sensor"
        );

        stepSensorManager.start(
                new StepSensorManager.StepListener() {

                    @Override
                    public void onStepsChanged(
                            long sensorTotalSteps
                    ) {

                        Log.d(
                                TAG,
                                "Background RAW steps = "
                                        + sensorTotalSteps
                        );

                        long todaySteps =
                                dailyStepManager
                                        .getTodaySteps(
                                                sensorTotalSteps
                                        );

                        int calories =
                                CalorieCalculator
                                        .calculateCalories(
                                                todaySteps
                                        );

                        Log.d(
                                TAG,
                                "Today steps = "
                                        + todaySteps
                        );

                        Log.d(
                                TAG,
                                "Calories = "
                                        + calories
                        );

                        updateNotification(
                                todaySteps,
                                calories
                        );

                        syncToFirestore(
                                todaySteps,
                                calories
                        );
                    }

                    @Override
                    public void onSensorUnavailable() {

                        Log.e(
                                TAG,
                                "Step sensor unavailable"
                        );
                    }

                    @Override
                    public void onSensorRegistrationFailed() {

                        Log.e(
                                TAG,
                                "Step sensor registration failed"
                        );
                    }
                }
        );
    }

    private void syncToFirestore(
            long todaySteps,
            int calories
    ) {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Log.e(
                    TAG,
                    "No Firebase user logged in"
            );

            return;
        }

        String userId =
                user.getUid();

        firestore
                .collection("users")
                .document(userId)
                .collection("activity")
                .document("today")
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {

                            long heartRate = 0;

                            if (documentSnapshot.exists()) {

                                Long oldHeartRate =
                                        documentSnapshot
                                                .getLong(
                                                        "heartRate"
                                                );

                                if (oldHeartRate != null) {

                                    heartRate =
                                            oldHeartRate;
                                }
                            }

                            java.util.HashMap<String, Object>
                                    healthData =
                                    new java.util.HashMap<>();

                            healthData.put(
                                    "userId",
                                    userId
                            );

                            healthData.put(
                                    "project",
                                    "FitTrack"
                            );

                            healthData.put(
                                    "steps",
                                    (int) Math.min(
                                            todaySteps,
                                            Integer.MAX_VALUE
                                    )
                            );

                            healthData.put(
                                    "calories",
                                    calories
                            );

                            healthData.put(
                                    "heartRate",
                                    (int) Math.min(
                                            heartRate,
                                            Integer.MAX_VALUE
                                    )
                            );

                            healthData.put(
                                    "timestamp",
                                    System.currentTimeMillis()
                            );

                            firestore
                                    .collection("users")
                                    .document(userId)
                                    .collection("activity")
                                    .document("today")
                                    .set(
                                            healthData
                                    )
                                    .addOnSuccessListener(
                                            unused ->
                                                    Log.d(
                                                            TAG,
                                                            "Activity synced to Firestore"
                                                    )
                                    )
                                    .addOnFailureListener(
                                            error ->
                                                    Log.e(
                                                            TAG,
                                                            "Firestore sync failed",
                                                            error
                                                    )
                                    );
                        }
                )
                .addOnFailureListener(
                        error ->
                                Log.e(
                                        TAG,
                                        "Could not read existing activity",
                                        error
                                )
                );
    }

    private Notification createNotification(
            long steps,
            int calories
    ) {

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle(
                        "FitTrack is tracking activity"
                )
                .setContentText(
                        "Steps: "
                                + steps
                                + " • Calories: "
                                + calories
                )
                .setSmallIcon(
                        R.mipmap.ic_launcher
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification(
            long steps,
            int calories
    ) {

        NotificationManager notificationManager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (notificationManager != null) {

            notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification(
                            steps,
                            calories
                    )
            );
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "FitTrack Activity Tracking",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Tracks daily steps and calories"
            );

            NotificationManager notificationManager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (notificationManager != null) {

                notificationManager
                        .createNotificationChannel(
                                channel
                        );
            }
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        Log.d(
                TAG,
                "StepTrackingService started"
        );

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        Log.d(
                TAG,
                "StepTrackingService destroyed"
        );

        if (stepSensorManager != null) {

            stepSensorManager.stop();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}