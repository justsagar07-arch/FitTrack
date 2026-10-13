package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class WorkoutActiveActivity extends AppCompatActivity {

    private LinearLayout layoutActive;
    private LinearLayout layoutSuccess;
    private TextView tvExerciseName;
    private TextView tvTimerCount;
    private TextView tvRepLabel;
    private TextView tvMotivationFeedback;
    private CircularProgressIndicator progressTimer;
    private MaterialButton btnFinishActive;
    private MaterialButton btnBackToLibrary;
    private MaterialButton btnViewHistory;

    private CountDownTimer countDownTimer;

    private static final int TOTAL_SETS = 3;
    private static final int REPS_PER_SET = 12;
    private static final int REST_SECONDS = 30;

    private int currentSet = 1;
    private boolean isResting = false;
    private boolean workoutCompleted = false;
    private boolean historySaveStarted = false;
    private long workoutStartTimeMs = 0;

    private String exerciseName = "Exercise Workout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workoutStartTimeMs = System.currentTimeMillis();
        setContentView(R.layout.activity_workout_active);

        String receivedName = getIntent().getStringExtra("exercise_name");
        if (receivedName != null && !receivedName.trim().isEmpty()) {
            exerciseName = receivedName;
        }

        layoutActive = findViewById(R.id.layout_active);
        layoutSuccess = findViewById(R.id.layout_success);
        tvExerciseName = findViewById(R.id.tv_active_exercise_name);
        tvTimerCount = findViewById(R.id.tv_timer_count);
        tvRepLabel = findViewById(R.id.tv_rep_label);
        tvMotivationFeedback = findViewById(R.id.tv_motivation_feedback);
        progressTimer = findViewById(R.id.progress_timer);
        btnFinishActive = findViewById(R.id.btn_finish_active);
        btnBackToLibrary = findViewById(R.id.btn_back_to_library);
        btnViewHistory = findViewById(R.id.btn_view_history);

        tvExerciseName.setText(exerciseName);

        layoutActive.setVisibility(View.VISIBLE);
        layoutSuccess.setVisibility(View.GONE);

        btnFinishActive.setOnClickListener(v -> onWorkoutButtonClick());

        btnBackToLibrary.setOnClickListener(v -> finish());

        btnViewHistory.setEnabled(false);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WorkoutActiveActivity.this,
                    WorkoutHistoryActivity.class
            );
            startActivity(intent);
        });

        showCurrentSet();
    }

    private void showCurrentSet() {
        if (currentSet > TOTAL_SETS) {
            showWorkoutComplete();
            return;
        }

        isResting = false;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        progressTimer.setMax(REPS_PER_SET);
        progressTimer.setProgress(0);

        tvTimerCount.setText("Set " + currentSet);
        tvRepLabel.setText(
                "Complete " + REPS_PER_SET + " reps with controlled form."
        );

        btnFinishActive.setText("Complete Set");
        btnFinishActive.setEnabled(true);
    }

    private void onWorkoutButtonClick() {
        if (workoutCompleted) {
            return;
        }

        if (isResting) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            currentSet++;
            showCurrentSet();
            return;
        }

        if (currentSet < TOTAL_SETS) {
            startRestTimer();
        } else {
            currentSet++;
            showWorkoutComplete();
        }
    }

    private void startRestTimer() {
        isResting = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        progressTimer.setMax(REST_SECONDS);
        progressTimer.setProgress(0);

        tvRepLabel.setText(
                "Set " + currentSet + " completed. Rest before your next set."
        );
        tvTimerCount.setText(
                String.format(Locale.getDefault(), "00:%02d", REST_SECONDS)
        );
        btnFinishActive.setText("Skip Rest");

        countDownTimer = new CountDownTimer(
                REST_SECONDS * 1000L, 1000
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);

                tvTimerCount.setText(
                        String.format(Locale.getDefault(), "00:%02d", seconds)
                );
                progressTimer.setProgress(REST_SECONDS - seconds);
            }

            @Override
            public void onFinish() {
                countDownTimer = null;
                currentSet++;
                showCurrentSet();

                Toast.makeText(
                        WorkoutActiveActivity.this,
                        "Rest finished. Start your next set!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }.start();
    }

    private void showWorkoutComplete() {
        if (workoutCompleted) {
            return;
        }

        workoutCompleted = true;
        isResting = false;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        layoutActive.setVisibility(View.GONE);
        layoutSuccess.setVisibility(View.VISIBLE);

        List<String> feedbacks = Arrays.asList(
                "Great work! You completed your workout.",
                "Well done! Keep focusing on safe, controlled form.",
                "Workout complete! Recover and hydrate."
        );

        String feedback = feedbacks.get(
                new Random().nextInt(feedbacks.size())
        );

        tvMotivationFeedback.setText(
                exerciseName + "\n\n"
                        + "Sets completed: " + TOTAL_SETS + "\n"
                        + "Target reps per set: " + REPS_PER_SET + "\n\n"
                        + feedback
        );

        btnViewHistory.setEnabled(false);
        btnViewHistory.setText("Saving Workout...");

        saveWorkoutToFirestore();
    }

    private void saveWorkoutToFirestore() {
        if (historySaveStarted) {
            return;
        }
        historySaveStarted = true;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            btnViewHistory.setText("History Unavailable");
            Toast.makeText(
                    this,
                    "Please sign in to save workout history.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        long durationSeconds = Math.max(1, (System.currentTimeMillis() - workoutStartTimeMs) / 1000);

        Map<String, Object> workout = new HashMap<>();
        workout.put("exerciseName", exerciseName);
        workout.put("setsCompleted", TOTAL_SETS);
        workout.put("repsPerSet", REPS_PER_SET);
        workout.put("durationSeconds", durationSeconds);
        workout.put("completed", true);
        workout.put("completedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutHistory")
                .add(workout)
                .addOnSuccessListener(documentReference -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnViewHistory.setEnabled(true);
                    btnViewHistory.setText("View Workout History");

                    Toast.makeText(
                            this,
                            "Workout history saved!",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnViewHistory.setText("Save Failed - Retry");
                    historySaveStarted = false;

                    btnViewHistory.setOnClickListener(v -> {
                        btnViewHistory.setEnabled(false);
                        btnViewHistory.setText("Saving Workout...");
                        saveWorkoutToFirestore();
                    });

                    Toast.makeText(
                            this,
                            "Could not save workout. Check internet and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onDestroy();
    }
}
