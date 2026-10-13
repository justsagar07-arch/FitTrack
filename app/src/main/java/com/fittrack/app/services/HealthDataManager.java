
package com.fittrack.app.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HealthDataManager {

    private static final String TAG = "FitTrackHealthData";
    private static final double CALORIES_PER_STEP = 0.04;

    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;
    private final HealthConnectManager healthConnectManager;

    public HealthDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.healthConnectManager = new HealthConnectManager(this.context);
    }

    // Current Firebase user ID
    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Get today's steps, calories and heart rate
    public void getTodayHealthData(OnHealthDataLoadedListener listener) {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "No Firebase user logged in");

            listener.onDataLoaded(0, 0, 0);
            return;
        }

        // Calories are calculated after today's steps are loaded.
        getHealthConnectSteps(listener);
    }

    // Get today's steps from Health Connect
    private void getHealthConnectSteps(
            OnHealthDataLoadedListener listener) {

        if (!healthConnectManager.isAvailable()) {
            Log.e(TAG, "Health Connect is not available");
            listener.onDataLoaded(0, 0, 0);
            return;
        }

        healthConnectManager.getTodaySteps(
                new HealthConnectManager.StepsCallback() {

                    @Override
                    public void onStepsLoaded(long steps) {

                        int healthConnectSteps;

                        if (steps > Integer.MAX_VALUE) {
                            healthConnectSteps = Integer.MAX_VALUE;
                        } else {
                            healthConnectSteps = (int) Math.max(0, steps);
                        }

                        // Existing formula: 0.04 calories per step, rounded.
                        int calculatedCalories = (int) Math.round(
                                healthConnectSteps * CALORIES_PER_STEP
                        );

                        Log.d(
                                TAG,
                                "Health Connect Steps = " + healthConnectSteps
                                        + ", Calculated Calories = "
                                        + calculatedCalories
                        );

                        getHealthConnectHeartRate(
                                healthConnectSteps,
                                calculatedCalories,
                                listener
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Health Connect Steps error", error);

                        if (listener != null) {
                            listener.onDataLoaded(0, 0, 0);
                        }
                    }
                }
        );
    }

    // Get today's heart rate from Health Connect
    private void getHealthConnectHeartRate(
            int steps,
            int calories,
            OnHealthDataLoadedListener listener) {

        healthConnectManager.getTodayHeartRate(
                new HealthConnectManager.HeartRateCallback() {

                    @Override
                    public void onHeartRateLoaded(long bpm) {

                        int heartRate;

                        if (bpm > Integer.MAX_VALUE) {
                            heartRate = Integer.MAX_VALUE;
                        } else {
                            heartRate = (int) Math.max(0, bpm);
                        }

                        Log.d(
                                TAG,
                                "Health Connect Heart Rate = "
                                        + heartRate + " BPM"
                        );

                        listener.onDataLoaded(
                                steps,
                                calories,
                                heartRate
                        );
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e(
                                TAG,
                                "Health Connect Heart Rate error",
                                error
                        );

                        // Heart-rate failure should not hide valid steps/calories.
                        listener.onDataLoaded(steps, calories, 0);
                    }
                }
        );
    }

    // Save today's health data to the current user's Firestore document.
    public void syncToFirestore(
            int steps,
            int calories,
            int heartRate) {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    context,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String userId = user.getUid();

        java.util.HashMap<String, Object> healthData =
                new java.util.HashMap<>();

        healthData.put("userId", userId);
        healthData.put("project", "FitTrack");
        healthData.put("steps", steps);
        healthData.put("calories", calories);
        healthData.put("heartRate", heartRate);
        healthData.put("heartRateUnit", "BPM");
        healthData.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("activity")
                .document("today")
                .set(healthData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Activity saved to Firestore");
                    Toast.makeText(
                            context,
                            "Activity saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Firestore save error", error);
                    Toast.makeText(
                            context,
                            "Save error: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Callback
    public interface OnHealthDataLoadedListener {
        void onDataLoaded(int steps, int calories, int heartRate);
    }
}
