package com.fittrack.app.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fittrack.app.R;
import com.fittrack.app.activities.AiAssistantActivity;
import com.fittrack.app.activities.BodyMetricsActivity;
import com.fittrack.app.activities.WorkoutStreakActivity;
import com.fittrack.app.services.StepTrackingService;
import com.fittrack.app.services.HealthDataManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private static final String TAG = "FitTrackHome";

    private HealthDataManager healthDataManager;

    private TextView tvSteps;
    private TextView tvCalories;
    private TextView tvHeartRate;

    private TextView tvHomeCompletedWorkouts;
    private TextView tvHomeCompletedStatus;
    private TextView tvHomeTotalDuration;
    private ProgressBar progressHomeWorkoutLoading;

    private final TextView[] homeChartCounts = new TextView[7];
    private final View[] homeChartBars = new View[7];
    private final TextView[] homeChartLabels = new TextView[7];

    private View cardAiAssistant;
    private View cardBodyAnalysis;
    private View cardDietPlan;
    private View cardExercises;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private final ActivityResultLauncher<String>
            activityRecognitionPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                        Log.d(
                                TAG,
                                "ACTIVITY_RECOGNITION permission = "
                                        + isGranted
                        );

                        if (isGranted) {

                            if (isAdded()) {
                                Toast.makeText(
                                        requireContext(),
                                        "Physical Activity permission granted",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            startBackgroundStepTracking();

                        } else {

                            if (isAdded()) {
                                Toast.makeText(
                                        requireContext(),
                                        "Physical Activity permission required",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            Log.e(
                                    TAG,
                                    "ACTIVITY_RECOGNITION permission denied"
                            );
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );

        healthDataManager = new HealthDataManager(requireContext());

        // SUMMARY CARDS
        tvSteps = view.findViewById(R.id.tv_steps_value);
        tvCalories = view.findViewById(R.id.tv_calories_value);
        tvHeartRate = view.findViewById(R.id.tv_heart_rate_value);

        // WORKOUT DASHBOARD VIEWS
        tvHomeCompletedWorkouts = view.findViewById(R.id.tv_home_completed_workouts);
        tvHomeCompletedStatus = view.findViewById(R.id.tv_home_completed_status);
        tvHomeTotalDuration = view.findViewById(R.id.tv_home_total_duration);
        progressHomeWorkoutLoading = view.findViewById(R.id.progress_home_workout_loading);

        homeChartCounts[0] = view.findViewById(R.id.tv_home_chart_mon);
        homeChartCounts[1] = view.findViewById(R.id.tv_home_chart_tue);
        homeChartCounts[2] = view.findViewById(R.id.tv_home_chart_wed);
        homeChartCounts[3] = view.findViewById(R.id.tv_home_chart_thu);
        homeChartCounts[4] = view.findViewById(R.id.tv_home_chart_fri);
        homeChartCounts[5] = view.findViewById(R.id.tv_home_chart_sat);
        homeChartCounts[6] = view.findViewById(R.id.tv_home_chart_sun);

        homeChartBars[0] = view.findViewById(R.id.bar_home_mon);
        homeChartBars[1] = view.findViewById(R.id.bar_home_tue);
        homeChartBars[2] = view.findViewById(R.id.bar_home_wed);
        homeChartBars[3] = view.findViewById(R.id.bar_home_thu);
        homeChartBars[4] = view.findViewById(R.id.bar_home_fri);
        homeChartBars[5] = view.findViewById(R.id.bar_home_sat);
        homeChartBars[6] = view.findViewById(R.id.bar_home_sun);

        homeChartLabels[0] = view.findViewById(R.id.tv_label_mon);
        homeChartLabels[1] = view.findViewById(R.id.tv_label_tue);
        homeChartLabels[2] = view.findViewById(R.id.tv_label_wed);
        homeChartLabels[3] = view.findViewById(R.id.tv_label_thu);
        homeChartLabels[4] = view.findViewById(R.id.tv_label_fri);
        homeChartLabels[5] = view.findViewById(R.id.tv_label_sat);
        homeChartLabels[6] = view.findViewById(R.id.tv_label_sun);

        // WORKOUT STREAK CARD
        View cardWorkoutStreak = view.findViewById(R.id.card_workout_streak);
        if (cardWorkoutStreak != null) {
            cardWorkoutStreak.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireContext(),
                        WorkoutStreakActivity.class
                );
                startActivity(intent);
            });
        }

        // AI ASSISTANT
        cardAiAssistant = view.findViewById(R.id.card_ai_assistant);

        if (cardAiAssistant != null) {
            cardAiAssistant.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireContext(),
                        AiAssistantActivity.class
                );
                startActivity(intent);
            });
        }

        // BODY ANALYSIS
        cardBodyAnalysis = view.findViewById(R.id.card_body_analysis);

        if (cardBodyAnalysis != null) {
            cardBodyAnalysis.setOnClickListener(v -> {
                Intent intent = new Intent(
                        requireContext(),
                        BodyMetricsActivity.class
                );
                startActivity(intent);
            });
        }

        // DIET PLAN
        cardDietPlan = view.findViewById(R.id.card_diet_plan);

        if (cardDietPlan != null) {
            cardDietPlan.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new DietFragment()
                        )
                        .addToBackStack(null)
                        .commit();
            });
        }

        // EXERCISES
        cardExercises = view.findViewById(R.id.card_exercises);

        if (cardExercises != null) {
            cardExercises.setOnClickListener(v -> {
                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                new WorkoutFragment()
                        )
                        .addToBackStack(null)
                        .commit();
            });
        }

        loadHealthData();
        loadCompletedWorkoutsSummary();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "HomeFragment onResume");

        checkActivityRecognitionPermission();
        loadHealthData();
        loadCompletedWorkoutsSummary();
    }

    private void loadCompletedWorkoutsSummary() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (tvHomeCompletedWorkouts != null) {
                tvHomeCompletedWorkouts.setText("0 Workouts");
            }
            if (tvHomeCompletedStatus != null) {
                tvHomeCompletedStatus.setText("Please sign in to view history");
            }
            if (tvHomeTotalDuration != null) {
                tvHomeTotalDuration.setText("Total Duration: Unavailable");
            }
            return;
        }

        if (progressHomeWorkoutLoading != null) {
            progressHomeWorkoutLoading.setVisibility(View.VISIBLE);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfWeekMillis = cal.getTimeInMillis();

        Calendar todayCal = Calendar.getInstance();
        int todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK); // Sun=1, Mon=2..Sat=7
        int todayIndex = (todayDayOfWeek + 5) % 7; // Mon=0..Sun=6

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutHistory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    if (progressHomeWorkoutLoading != null) {
                        progressHomeWorkoutLoading.setVisibility(View.GONE);
                    }

                    int completedCount = 0;
                    long totalDurationSeconds = 0;
                    boolean hasAnyDuration = false;
                    int[] weekDayCounts = new int[7];

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean completed = doc.getBoolean("completed");
                        if (!Boolean.TRUE.equals(completed)) {
                            continue;
                        }

                        completedCount++;

                        Long durationSec = doc.getLong("durationSeconds");
                        if (durationSec == null) {
                            durationSec = doc.getLong("duration");
                        }
                        if (durationSec != null && durationSec > 0) {
                            totalDurationSeconds += durationSec;
                            hasAnyDuration = true;
                        }

                        Object timestampObj = doc.get("completedAt");
                        if (timestampObj instanceof Timestamp) {
                            Date date = ((Timestamp) timestampObj).toDate();
                            long timeMillis = date.getTime();

                            if (timeMillis >= startOfWeekMillis) {
                                Calendar wCal = Calendar.getInstance();
                                wCal.setTime(date);
                                int dayOfWeek = wCal.get(Calendar.DAY_OF_WEEK);
                                int dayIdx = (dayOfWeek + 5) % 7;
                                weekDayCounts[dayIdx]++;
                            }
                        }
                    }

                    if (tvHomeCompletedWorkouts != null) {
                        tvHomeCompletedWorkouts.setText(completedCount + " Workout" + (completedCount == 1 ? "" : "s"));
                    }

                    if (tvHomeCompletedStatus != null) {
                        if (completedCount == 0) {
                            tvHomeCompletedStatus.setText("No completed workouts yet");
                        } else {
                            tvHomeCompletedStatus.setText("Successfully completed");
                        }
                    }

                    if (tvHomeTotalDuration != null) {
                        if (hasAnyDuration && totalDurationSeconds > 0) {
                            long mins = totalDurationSeconds / 60;
                            long hrs = mins / 60;
                            long remMins = mins % 60;

                            if (hrs > 0) {
                                tvHomeTotalDuration.setText(String.format(Locale.getDefault(), "Total Duration: %dh %dm", hrs, remMins));
                            } else {
                                tvHomeTotalDuration.setText(String.format(Locale.getDefault(), "Total Duration: %d min", Math.max(1, mins)));
                            }
                        } else {
                            tvHomeTotalDuration.setText("Total Duration: Unavailable (no duration recorded)");
                        }
                    }

                    updateWeeklyChartUI(weekDayCounts, todayIndex);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) return;

                    if (progressHomeWorkoutLoading != null) {
                        progressHomeWorkoutLoading.setVisibility(View.GONE);
                    }

                    if (tvHomeCompletedStatus != null) {
                        tvHomeCompletedStatus.setText("Could not load summary");
                    }
                });
    }

    private void updateWeeklyChartUI(int[] weekDayCounts, int todayIndex) {
        int maxCount = 1;
        for (int count : weekDayCounts) {
            if (count > maxCount) {
                maxCount = count;
            }
        }

        int greenColor = Color.rgb(0, 200, 83);
        int defaultBlue = Color.rgb(26, 35, 126);

        for (int i = 0; i < 7; i++) {
            if (homeChartCounts[i] != null) {
                homeChartCounts[i].setText(String.valueOf(weekDayCounts[i]));
            }

            if (homeChartBars[i] != null) {
                float ratio = (float) weekDayCounts[i] / maxCount;
                int barHeightDp = weekDayCounts[i] > 0 ? (int) (8 + (ratio * 55)) : 8;

                ViewGroup.LayoutParams params = homeChartBars[i].getLayoutParams();
                params.height = dp(barHeightDp);
                homeChartBars[i].setLayoutParams(params);

                if (i == todayIndex) {
                    homeChartBars[i].setBackgroundColor(greenColor);
                } else {
                    homeChartBars[i].setBackgroundColor(defaultBlue);
                }
            }

            if (homeChartLabels[i] != null) {
                if (i == todayIndex) {
                    homeChartLabels[i].setTextColor(greenColor);
                    homeChartLabels[i].setTypeface(null, Typeface.BOLD);
                } else {
                    homeChartLabels[i].setTextColor(Color.DKGRAY);
                    homeChartLabels[i].setTypeface(null, Typeface.NORMAL);
                }
            }
        }
    }

    private void checkActivityRecognitionPermission() {

        if (!isAdded()) {
            return;
        }

        if (Build.VERSION.SDK_INT < 29) {
            startBackgroundStepTracking();
            return;
        }

        int permissionStatus = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
        );

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {

            Log.d(
                    TAG,
                    "ACTIVITY_RECOGNITION permission = GRANTED"
            );

            startBackgroundStepTracking();

        } else {

            Log.d(
                    TAG,
                    "ACTIVITY_RECOGNITION permission = NOT GRANTED"
            );

            activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
            );
        }
    }

    private void startBackgroundStepTracking() {

        if (!isAdded()) {
            return;
        }

        try {

            Intent serviceIntent = new Intent(
                    requireContext(),
                    StepTrackingService.class
            );

            ContextCompat.startForegroundService(
                    requireContext(),
                    serviceIntent
            );

            Log.d(TAG, "StepTrackingService started");

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not start StepTrackingService",
                    e
            );

            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        "Could not start activity tracking",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void loadHealthData() {

        if (healthDataManager == null || !isAdded()) {
            return;
        }

        healthDataManager.getTodayHealthData(
                (steps, calories, heartRate) -> {

                    if (!isAdded()) {
                        return;
                    }

                    requireActivity().runOnUiThread(() -> {

                        if (!isAdded()) {
                            return;
                        }

                        if (tvSteps != null) {
                            tvSteps.setText(String.valueOf(steps));
                        }

                        if (tvCalories != null) {
                            tvCalories.setText(
                                    getString(
                                            R.string.calories_value_format,
                                            calories
                                    )
                            );
                        }

                        if (tvHeartRate != null) {
                            if (heartRate > 0) {
                                tvHeartRate.setText(
                                        String.valueOf(heartRate)
                                );
                            } else {
                                tvHeartRate.setText("--");
                            }
                        }
                    });
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "HomeFragment onPause");
    }
}
