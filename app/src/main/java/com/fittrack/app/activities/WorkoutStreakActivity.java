package com.fittrack.app.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WorkoutStreakActivity extends AppCompatActivity {

    private ProgressBar progressLoading;
    private TextView tvCurrentStreak;
    private TextView tvLongestStreak;
    private TextView tvMonthYear;
    private TextView tvTotalWorkoutDays;
    private GridLayout gridCalendar;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;

    private final Set<String> uniqueWorkoutDates = new HashSet<>();
    private Calendar currentDisplayMonth;

    private static final SimpleDateFormat DATE_FORMAT_KEY =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_streak);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_streak);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressLoading = findViewById(R.id.progress_loading);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);
        tvLongestStreak = findViewById(R.id.tv_longest_streak);
        tvMonthYear = findViewById(R.id.tv_month_year);
        tvTotalWorkoutDays = findViewById(R.id.tv_total_workout_days);
        gridCalendar = findViewById(R.id.grid_calendar);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        currentDisplayMonth = Calendar.getInstance();

        btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        loadWorkoutHistory();
    }

    private void loadWorkoutHistory() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Please sign in to view workout streaks.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutHistory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressLoading.setVisibility(View.GONE);

                    uniqueWorkoutDates.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean completed = doc.getBoolean("completed");
                        if (!Boolean.TRUE.equals(completed)) {
                            continue;
                        }

                        Object timestampObj = doc.get("completedAt");
                        if (timestampObj instanceof Timestamp) {
                            Date date = ((Timestamp) timestampObj).toDate();
                            uniqueWorkoutDates.add(DATE_FORMAT_KEY.format(date));
                        }
                    }

                    calculateStreaksAndRender();
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(WorkoutStreakActivity.this, "Could not load workout history.", Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateStreaksAndRender() {
        int currentStreak = 0;
        int longestStreak = 0;

        if (!uniqueWorkoutDates.isEmpty()) {
            List<String> sortedDateStrings = new ArrayList<>(uniqueWorkoutDates);
            Collections.sort(sortedDateStrings);

            List<Calendar> sortedCalendars = new ArrayList<>();
            for (String dateStr : sortedDateStrings) {
                try {
                    Date d = DATE_FORMAT_KEY.parse(dateStr);
                    if (d != null) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(d);
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        sortedCalendars.add(c);
                    }
                } catch (Exception ignored) {
                }
            }

            // Calculate longest streak
            if (!sortedCalendars.isEmpty()) {
                int runningStreak = 1;
                longestStreak = 1;

                for (int i = 1; i < sortedCalendars.size(); i++) {
                    long prevMillis = sortedCalendars.get(i - 1).getTimeInMillis();
                    long currMillis = sortedCalendars.get(i).getTimeInMillis();

                    long diffDays = Math.round((currMillis - prevMillis) / (1000.0 * 60 * 60 * 24));

                    if (diffDays == 1) {
                        runningStreak++;
                        if (runningStreak > longestStreak) {
                            longestStreak = runningStreak;
                        }
                    } else if (diffDays > 1) {
                        runningStreak = 1;
                    }
                }
            }

            // Calculate current streak
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            String todayKey = DATE_FORMAT_KEY.format(today.getTime());

            Calendar checkCal = (Calendar) today.clone();

            if (!uniqueWorkoutDates.contains(todayKey)) {
                // Check if yesterday has a workout
                checkCal.add(Calendar.DAY_OF_YEAR, -1);
                String yesterdayKey = DATE_FORMAT_KEY.format(checkCal.getTime());

                if (uniqueWorkoutDates.contains(yesterdayKey)) {
                    // Active streak from yesterday
                    while (uniqueWorkoutDates.contains(DATE_FORMAT_KEY.format(checkCal.getTime()))) {
                        currentStreak++;
                        checkCal.add(Calendar.DAY_OF_YEAR, -1);
                    }
                }
            } else {
                // Active streak including today
                while (uniqueWorkoutDates.contains(DATE_FORMAT_KEY.format(checkCal.getTime()))) {
                    currentStreak++;
                    checkCal.add(Calendar.DAY_OF_YEAR, -1);
                }
            }
        }

        tvCurrentStreak.setText(currentStreak + " Day" + (currentStreak == 1 ? "" : "s"));
        tvLongestStreak.setText(longestStreak + " Day" + (longestStreak == 1 ? "" : "s"));
        tvTotalWorkoutDays.setText("Total Active Days: " + uniqueWorkoutDates.size());

        renderCalendar();
    }

    private void renderCalendar() {
        tvMonthYear.setText(MONTH_YEAR_FORMAT.format(currentDisplayMonth.getTime()));

        gridCalendar.removeAllViews();

        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sun, 2 = Mon ... 7 = Sat
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int emptyOffset = firstDayOfWeek - 1;

        // Empty cells before 1st of month
        for (int i = 0; i < emptyOffset; i++) {
            TextView emptyView = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(42);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gridCalendar.addView(emptyView, params);
        }

        // Days of month
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateKey = DATE_FORMAT_KEY.format(cal.getTime());

            boolean hasWorkout = uniqueWorkoutDates.contains(dateKey);

            TextView dayView = new TextView(this);
            dayView.setText(String.valueOf(day));
            dayView.setTextSize(14);
            dayView.setGravity(Gravity.CENTER);

            if (hasWorkout) {
                dayView.setTextColor(Color.WHITE);
                dayView.setTypeface(null, Typeface.BOLD);

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(Color.rgb(0, 200, 83)); // Healthy green accent
                dayView.setBackground(shape);
            } else {
                dayView.setTextColor(Color.rgb(33, 33, 33));
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(42);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            gridCalendar.addView(dayView, params);
        }
    }
}
