package com.fittrack.app.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeeklyProgressActivity extends AppCompatActivity {

    private TextView tvWeekRange;
    private ProgressBar progressLoading;
    private TextView tvTotalCompletedWorkouts;
    private TextView tvTotalWorkoutDuration;
    private TextView tvDurationSubtitle;
    private LinearLayout layoutDailyBreakdown;

    private final TextView[] dayCountTextViews = new TextView[7];
    private final View[] dayBarViews = new View[7];

    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_progress);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_weekly_progress);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvWeekRange = findViewById(R.id.tv_week_range);
        progressLoading = findViewById(R.id.progress_loading);
        tvTotalCompletedWorkouts = findViewById(R.id.tv_total_completed_workouts);
        tvTotalWorkoutDuration = findViewById(R.id.tv_total_workout_duration);
        tvDurationSubtitle = findViewById(R.id.tv_duration_subtitle);
        layoutDailyBreakdown = findViewById(R.id.layout_daily_breakdown);

        dayCountTextViews[0] = findViewById(R.id.tv_count_mon);
        dayCountTextViews[1] = findViewById(R.id.tv_count_tue);
        dayCountTextViews[2] = findViewById(R.id.tv_count_wed);
        dayCountTextViews[3] = findViewById(R.id.tv_count_thu);
        dayCountTextViews[4] = findViewById(R.id.tv_count_fri);
        dayCountTextViews[5] = findViewById(R.id.tv_count_sat);
        dayCountTextViews[6] = findViewById(R.id.tv_count_sun);

        dayBarViews[0] = findViewById(R.id.bar_mon);
        dayBarViews[1] = findViewById(R.id.bar_tue);
        dayBarViews[2] = findViewById(R.id.bar_wed);
        dayBarViews[3] = findViewById(R.id.bar_thu);
        dayBarViews[4] = findViewById(R.id.bar_fri);
        dayBarViews[5] = findViewById(R.id.bar_sat);
        dayBarViews[6] = findViewById(R.id.bar_sun);

        loadWeeklyData();
    }

    private void loadWeeklyData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Please sign in to view weekly progress.", Toast.LENGTH_SHORT).show();
            return;
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        String startDateStr = dateFormat.format(cal.getTime());

        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 6);
        String endDateStr = dateFormat.format(endCal.getTime());

        tvWeekRange.setText("Week: " + startDateStr + " – " + endDateStr);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutHistory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressLoading.setVisibility(View.GONE);

                    int totalWorkouts = 0;
                    long totalDurationSeconds = 0;
                    boolean hasAnyDuration = false;

                    int[] dayCounts = new int[7];
                    List<List<String>> dayExercises = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        dayExercises.add(new ArrayList<>());
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean completed = doc.getBoolean("completed");
                        if (!Boolean.TRUE.equals(completed)) {
                            continue;
                        }

                        Object timestampObj = doc.get("completedAt");
                        if (!(timestampObj instanceof Timestamp)) {
                            continue;
                        }

                        Date completedDate = ((Timestamp) timestampObj).toDate();
                        long timeMillis = completedDate.getTime();

                        if (timeMillis < startOfWeekMillis) {
                            continue;
                        }

                        Calendar workoutCal = Calendar.getInstance();
                        workoutCal.setTime(completedDate);

                        int dayOfWeek = workoutCal.get(Calendar.DAY_OF_WEEK);
                        // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) to Mon=0..Sun=6
                        int dayIndex = (dayOfWeek + 5) % 7;

                        dayCounts[dayIndex]++;
                        totalWorkouts++;

                        String exerciseName = doc.getString("exerciseName");
                        if (exerciseName != null && !exerciseName.trim().isEmpty()) {
                            dayExercises.get(dayIndex).add(exerciseName);
                        } else {
                            dayExercises.get(dayIndex).add("Workout Session");
                        }

                        Long durationSec = doc.getLong("durationSeconds");
                        if (durationSec == null) {
                            durationSec = doc.getLong("duration");
                        }

                        if (durationSec != null && durationSec > 0) {
                            totalDurationSeconds += durationSec;
                            hasAnyDuration = true;
                        }
                    }

                    updateUI(totalWorkouts, totalDurationSeconds, hasAnyDuration, dayCounts, dayExercises);
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(WeeklyProgressActivity.this, "Could not load weekly data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(
            int totalWorkouts,
            long totalDurationSeconds,
            boolean hasAnyDuration,
            int[] dayCounts,
            List<List<String>> dayExercises
    ) {
        tvTotalCompletedWorkouts.setText(String.valueOf(totalWorkouts));

        if (hasAnyDuration && totalDurationSeconds > 0) {
            long minutes = totalDurationSeconds / 60;
            long hours = minutes / 60;
            long remainingMins = minutes % 60;

            if (hours > 0) {
                tvTotalWorkoutDuration.setText(String.format(Locale.getDefault(), "%dh %dm", hours, remainingMins));
            } else {
                tvTotalWorkoutDuration.setText(String.format(Locale.getDefault(), "%d min", Math.max(1, minutes)));
            }
            tvDurationSubtitle.setText("Total Duration");
        } else {
            tvTotalWorkoutDuration.setText("Unavailable");
            tvDurationSubtitle.setText("No duration recorded");
        }

        int maxCount = 1;
        for (int count : dayCounts) {
            if (count > maxCount) {
                maxCount = count;
            }
        }

        for (int i = 0; i < 7; i++) {
            dayCountTextViews[i].setText(String.valueOf(dayCounts[i]));

            float ratio = (float) dayCounts[i] / maxCount;
            int barHeightDp = dayCounts[i] > 0 ? (int) (8 + (ratio * 90)) : 8;

            ViewGroup.LayoutParams params = dayBarViews[i].getLayoutParams();
            params.height = dp(barHeightDp);
            dayBarViews[i].setLayoutParams(params);
        }

        layoutDailyBreakdown.removeAllViews();

        for (int i = 0; i < 7; i++) {
            addDailyBreakdownCard(DAY_NAMES[i], dayCounts[i], dayExercises.get(i));
        }
    }

    private void addDailyBreakdownCard(String dayName, int count, List<String> exercises) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(1));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(8);

        TextView header = new TextView(this);
        header.setText(dayName + " (" + count + " workout" + (count == 1 ? "" : "s") + ")");
        header.setTextSize(15);
        header.setTextColor(count > 0 ? Color.rgb(26, 35, 126) : Color.GRAY);
        header.setTypeface(null, Typeface.BOLD);

        card.addView(header);

        if (count == 0) {
            TextView noWorkout = new TextView(this);
            noWorkout.setText("No workouts completed");
            noWorkout.setTextSize(13);
            noWorkout.setTextColor(Color.LTGRAY);
            noWorkout.setPadding(0, dp(4), 0, 0);
            card.addView(noWorkout);
        } else {
            for (String exName : exercises) {
                TextView exText = new TextView(this);
                exText.setText("• " + exName);
                exText.setTextSize(14);
                exText.setTextColor(Color.DKGRAY);
                exText.setPadding(0, dp(4), 0, 0);
                card.addView(exText);
            }
        }

        layoutDailyBreakdown.addView(card, cardParams);
    }
}
