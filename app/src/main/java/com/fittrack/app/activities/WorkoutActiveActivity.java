package com.fittrack.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private static final int REST_SECONDS = 30;
    private static final int DEFAULT_SETS = 3;
    private static final int DEFAULT_REPS = 15;
    private static final int DEFAULT_TIMER_SECONDS = 60;

    private int totalSets = DEFAULT_SETS;
    private int repsPerSet = DEFAULT_REPS;
    private int currentSet = 1;
    private int completedSets = 0;
    private int currentSetReps = 0;
    private int postureAccuracyTotal = 0;
    private int postureAccuracySets = 0;
    private int latestPostureAccuracy = 0;
    private int latestEvaluatedFrames = 0;

    private boolean isResting = false;
    private boolean workoutCompleted = false;
    private boolean historySaveStarted = false;
    private boolean cameraWorkout = false;
    private boolean timeBasedWorkout = false;

    private long workoutStartTimeMs;
    private String exerciseName = "Exercise Workout";

    private final ActivityResultLauncher<Intent> postureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (workoutCompleted || isFinishing()) {
                            return;
                        }

                        if (result.getResultCode() != RESULT_OK
                                || result.getData() == null) {

                            btnFinishActive.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Set was not completed. You can try again.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        latestPostureAccuracy = result.getData()
                                .getIntExtra("posture_accuracy", 0);
                        latestEvaluatedFrames = result.getData()
                                .getIntExtra("evaluated_frames", 0);
                        if (cameraWorkout && latestEvaluatedFrames > 0) {
                            postureAccuracyTotal += latestPostureAccuracy;
                            postureAccuracySets++;
                        }

                        if (timeBasedWorkout) {
                            int completedSeconds = result.getData()
                                    .getIntExtra("completed_seconds", 0);

                            if (completedSeconds < repsPerSet) {
                                btnFinishActive.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Target valid-posture time not reached. Try again.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            currentSetReps = completedSeconds;
                            completeCurrentSet();
                            return;
                        }

                        int detectedReps = result.getData()
                                .getIntExtra("completed_reps", 0);

                        if (detectedReps < repsPerSet) {
                            btnFinishActive.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Target reps not reached yet.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        currentSetReps = detectedReps;
                        completeCurrentSet();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_active);

        workoutStartTimeMs = System.currentTimeMillis();

        String receivedName =
                getIntent().getStringExtra("exercise_name");

        if (receivedName != null && !receivedName.trim().isEmpty()) {
            exerciseName = receivedName.trim();
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

        String upperName = exerciseName
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");

        // All workouts use the existing posture-detection camera.
        cameraWorkout = true;

        // Timer-based exercises.
        timeBasedWorkout = upperName.contains("PLANK")
                || upperName.contains("HIIT")
                || upperName.contains("JUMP ROPE")
                || upperName.contains("JUMPING JACK")
                || upperName.contains("JUMPING JACKS")
                || upperName.contains("HIGH KNEE")
                || upperName.contains("HIGH KNEES")
                || upperName.contains("JOGGING IN PLACE")
                || upperName.contains("JOG IN PLACE")
                || upperName.contains("RUNNING IN PLACE");

        // Set defaults according to exercise type.
        totalSets = DEFAULT_SETS;
        repsPerSet = timeBasedWorkout
                ? DEFAULT_TIMER_SECONDS
                : DEFAULT_REPS;

        tvExerciseName.setText(exerciseName);

        layoutActive.setVisibility(View.VISIBLE);
        layoutSuccess.setVisibility(View.GONE);

        tvRepLabel.setOnClickListener(v -> showTargetEditor());
        btnFinishActive.setOnClickListener(v -> onWorkoutButtonClick());
        btnBackToLibrary.setOnClickListener(v -> finish());

        btnViewHistory.setEnabled(false);
        btnViewHistory.setOnClickListener(v -> startActivity(
                new Intent(
                        WorkoutActiveActivity.this,
                        WorkoutHistoryActivity.class
                )
        ));

        showCurrentSet();
    }

    private void showTargetEditor() {
        if (workoutCompleted || isResting) {
            return;
        }

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);

        int pad = (int) (16 * getResources()
                .getDisplayMetrics().density);

        fields.setPadding(pad, pad, pad, pad);

        EditText setsInput = new EditText(this);
        setsInput.setHint("Number of sets (1–10)");
        setsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        setsInput.setText(String.valueOf(totalSets));

        EditText repsInput = new EditText(this);
        repsInput.setHint(
                timeBasedWorkout
                        ? "Duration in seconds per set"
                        : "Reps per set (1–100)"
        );
        repsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        repsInput.setText(String.valueOf(repsPerSet));

        fields.addView(setsInput);
        fields.addView(repsInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit workout target")
                .setMessage("Update your target before continuing.")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int sets = Integer.parseInt(
                                setsInput.getText().toString().trim()
                        );

                        int target = Integer.parseInt(
                                repsInput.getText().toString().trim()
                        );

                        if (sets < 1 || sets > 10
                                || target < 1 || target > 100) {

                            Toast.makeText(
                                    this,
                                    "Sets must be 1–10 and target 1–100.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        totalSets = sets;
                        repsPerSet = target;
                        currentSet = 1;
                        completedSets = 0;
                        currentSetReps = 0;

                        showCurrentSet();

                    } catch (NumberFormatException e) {
                        Toast.makeText(
                                this,
                                "Enter valid numbers.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .show();
    }

    private void showCurrentSet() {
        if (currentSet > totalSets) {
            showWorkoutComplete();
            return;
        }

        isResting = false;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        currentSetReps = 0;

        progressTimer.setMax(repsPerSet);
        progressTimer.setProgress(0);

        tvTimerCount.setText("Set " + currentSet);

        tvRepLabel.setText(
                "Target: " + totalSets + " sets × " + repsPerSet
                        + (timeBasedWorkout ? " seconds" : " reps")
                        + "\nTap here to edit target."
        );

        btnFinishActive.setEnabled(true);

        btnFinishActive.setText(
                timeBasedWorkout
                        ? "Start Camera & Timer"
                        : cameraWorkout
                          ? "Start Camera & Count"
                          : "Complete Set"
        );
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

        if (timeBasedWorkout || cameraWorkout) {
            launchPostureCamera();
            return;
        }

        currentSetReps = repsPerSet;
        completeCurrentSet();
    }

    private void launchPostureCamera() {
        btnFinishActive.setEnabled(false);

        Intent intent = new Intent(
                this,
                PostureDetectionActivity.class
        );

        intent.putExtra("exerciseName", exerciseName);
        intent.putExtra("target_reps", repsPerSet);
        intent.putExtra("current_set", currentSet);
        intent.putExtra("time_based", timeBasedWorkout);

        // Send 60 seconds for timer-based exercises.
        intent.putExtra(
                "target_duration_seconds",
                timeBasedWorkout
                        ? DEFAULT_TIMER_SECONDS
                        : repsPerSet
        );

        postureLauncher.launch(intent);
    }

    private void completeCurrentSet() {
        completedSets++;

        if (currentSet >= totalSets) {
            currentSet = totalSets + 1;
            showWorkoutComplete();
        } else {
            startRestTimer();
        }
    }

    private void startRestTimer() {
        isResting = true;
        btnFinishActive.setEnabled(true);

        progressTimer.setMax(REST_SECONDS);
        progressTimer.setProgress(0);

        tvRepLabel.setText(
                "Set " + currentSet
                        + " completed. Rest before your next set."
        );

        tvTimerCount.setText(String.format(
                Locale.getDefault(),
                "00:%02d",
                REST_SECONDS
        ));

        btnFinishActive.setText("Skip Rest");

        countDownTimer = new CountDownTimer(
                REST_SECONDS * 1000L,
                1000
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(
                        millisUntilFinished / 1000.0
                );

                tvTimerCount.setText(String.format(
                        Locale.getDefault(),
                        "00:%02d",
                        seconds
                ));

                progressTimer.setProgress(REST_SECONDS - seconds);
            }

            @Override
            public void onFinish() {
                countDownTimer = null;
                currentSet++;
                showCurrentSet();
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
                        + "Sets completed: " + completedSets + "\n"
                        + "Target per set: " + repsPerSet
                        + (timeBasedWorkout ? " seconds" : " reps")
                        + "\n\n" + feedback
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

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        long durationSeconds = Math.max(
                1,
                (System.currentTimeMillis() - workoutStartTimeMs) / 1000
        );

        Map<String, Object> workout = new HashMap<>();

        workout.put("exerciseName", exerciseName);
        workout.put("setsTarget", totalSets);
        workout.put("setsCompleted", completedSets);
        workout.put("repsPerSet", repsPerSet);
        workout.put("timeBased", timeBasedWorkout);
        workout.put("durationSeconds", durationSeconds);
        workout.put("completed", completedSets == totalSets);
        workout.put("completedAt", FieldValue.serverTimestamp());

        // Average frame-based posture score across camera-checked sets.
        // If no valid posture frames were evaluated, store 0 and the set count.
        int averagePostureAccuracy = postureAccuracySets > 0
                ? Math.round((float) postureAccuracyTotal / postureAccuracySets)
                : 0;
        workout.put("postureAccuracy", averagePostureAccuracy);
        workout.put("postureAccuracySets", postureAccuracySets);
        workout.put("postureAccuracyType", "frame_based_estimate");

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

