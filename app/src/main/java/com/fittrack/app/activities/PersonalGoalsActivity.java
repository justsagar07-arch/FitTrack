package com.fittrack.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PersonalGoalsActivity extends AppCompatActivity {

    private ProgressBar progressLoading;
    private TextView tvDailyProgressStatus;
    private TextView tvWeeklyProgressStatus;
    private LinearProgressIndicator progressDailyWorkout;
    private LinearProgressIndicator progressWeeklyWorkout;
    private TextInputEditText etDailyTarget;
    private TextInputEditText etWeeklyTarget;
    private MaterialButton btnSaveGoals;

    private int dailyTarget = 1;
    private int weeklyTarget = 5;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_goals);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_personal_goals);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressLoading = findViewById(R.id.progress_loading);
        tvDailyProgressStatus = findViewById(R.id.tv_daily_progress_status);
        tvWeeklyProgressStatus = findViewById(R.id.tv_weekly_progress_status);
        progressDailyWorkout = findViewById(R.id.progress_daily_workout);
        progressWeeklyWorkout = findViewById(R.id.progress_weekly_workout);
        etDailyTarget = findViewById(R.id.et_daily_target);
        etWeeklyTarget = findViewById(R.id.et_weekly_target);
        btnSaveGoals = findViewById(R.id.btn_save_goals);

        btnSaveGoals.setOnClickListener(v -> savePersonalGoals());

        loadGoalsAndProgress();
    }

    private void loadGoalsAndProgress() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Please sign in to view personal goals.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load goals configuration from users/{uid}
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        Long dailyObj = docSnapshot.getLong("dailyWorkoutTarget");
                        Long weeklyObj = docSnapshot.getLong("weeklyWorkoutTarget");

                        if (dailyObj != null && dailyObj > 0) {
                            dailyTarget = dailyObj.intValue();
                        }
                        if (weeklyObj != null && weeklyObj > 0) {
                            weeklyTarget = weeklyObj.intValue();
                        }
                    }

                    etDailyTarget.setText(String.valueOf(dailyTarget));
                    etWeeklyTarget.setText(String.valueOf(weeklyTarget));

                    loadWorkoutProgress(uid);
                })
                .addOnFailureListener(error -> {
                    etDailyTarget.setText(String.valueOf(dailyTarget));
                    etWeeklyTarget.setText(String.valueOf(weeklyTarget));
                    loadWorkoutProgress(uid);
                });
    }

    private void loadWorkoutProgress(String uid) {
        Calendar todayCal = Calendar.getInstance();
        String todayKey = DATE_FORMAT.format(todayCal.getTime());

        Calendar startOfWeekCal = Calendar.getInstance();
        startOfWeekCal.setFirstDayOfWeek(Calendar.MONDAY);
        startOfWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeekCal.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeekCal.set(Calendar.MINUTE, 0);
        startOfWeekCal.set(Calendar.SECOND, 0);
        startOfWeekCal.set(Calendar.MILLISECOND, 0);

        long startOfWeekMillis = startOfWeekCal.getTimeInMillis();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutHistory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressLoading.setVisibility(View.GONE);

                    int todayCount = 0;
                    int weekCount = 0;

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
                        String dateKey = DATE_FORMAT.format(completedDate);

                        if (todayKey.equals(dateKey)) {
                            todayCount++;
                        }

                        if (completedDate.getTime() >= startOfWeekMillis) {
                            weekCount++;
                        }
                    }

                    updateProgressUI(todayCount, weekCount);
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(PersonalGoalsActivity.this, "Could not load progress data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProgressUI(int todayCount, int weekCount) {
        tvDailyProgressStatus.setText(todayCount + " / " + dailyTarget + " Workouts");
        int dailyPercent = (int) Math.min(100, ((float) todayCount / dailyTarget) * 100);
        progressDailyWorkout.setProgress(dailyPercent);

        tvWeeklyProgressStatus.setText(weekCount + " / " + weeklyTarget + " Workouts");
        int weeklyPercent = (int) Math.min(100, ((float) weekCount / weeklyTarget) * 100);
        progressWeeklyWorkout.setProgress(weeklyPercent);
    }

    private void savePersonalGoals() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to save goals.", Toast.LENGTH_SHORT).show();
            return;
        }

        String dailyStr = etDailyTarget.getText() == null ? "" : etDailyTarget.getText().toString().trim();
        String weeklyStr = etWeeklyTarget.getText() == null ? "" : etWeeklyTarget.getText().toString().trim();

        if (dailyStr.isEmpty() || weeklyStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid daily and weekly targets.", Toast.LENGTH_SHORT).show();
            return;
        }

        int newDaily;
        int newWeekly;

        try {
            newDaily = Integer.parseInt(dailyStr);
            newWeekly = Integer.parseInt(weeklyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Targets must be valid numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newDaily <= 0 || newWeekly <= 0) {
            Toast.makeText(this, "Targets must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        dailyTarget = newDaily;
        weeklyTarget = newWeekly;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> goalsData = new HashMap<>();
        goalsData.put("dailyWorkoutTarget", dailyTarget);
        goalsData.put("weeklyWorkoutTarget", weeklyTarget);

        btnSaveGoals.setEnabled(false);
        progressLoading.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(goalsData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    btnSaveGoals.setEnabled(true);
                    Toast.makeText(PersonalGoalsActivity.this, "Personal goals saved!", Toast.LENGTH_SHORT).show();
                    loadWorkoutProgress(uid);
                })
                .addOnFailureListener(error -> {
                    btnSaveGoals.setEnabled(true);
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(PersonalGoalsActivity.this, "Could not save goals. Check network.", Toast.LENGTH_SHORT).show();
                });
    }
}
