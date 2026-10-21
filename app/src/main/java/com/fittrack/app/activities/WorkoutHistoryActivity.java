package com.fittrack.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private ProgressBar progressBar;
    private TextView tvStatus;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        createScreen();
        loadWorkoutHistory();
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 245, 250));
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Workout History");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(30, 35, 100));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitle = new TextView(this);
        subtitle.setText("Your completed workout sessions");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(16);
        root.addView(subtitle, subtitleParams);

        MaterialButton btnWeeklyProgress = new MaterialButton(this);
        btnWeeklyProgress.setText("View Weekly Progress Dashboard");
        btnWeeklyProgress.setTextSize(14);
        btnWeeklyProgress.setCornerRadius(dp(8));
        btnWeeklyProgress.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WorkoutHistoryActivity.this,
                    WeeklyProgressActivity.class
            );
            startActivity(intent);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.bottomMargin = dp(12);
        root.addView(btnWeeklyProgress, btnParams);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        progressParams.topMargin = dp(20);
        root.addView(progressBar, progressParams);

        tvStatus = new TextView(this);
        tvStatus.setText("Loading workout history...");
        tvStatus.setTextSize(15);
        tvStatus.setTextColor(Color.DKGRAY);
        tvStatus.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(12);
        root.addView(tvStatus, statusParams);

        ScrollView scrollView = new ScrollView(this);
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(historyContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        scrollParams.topMargin = dp(12);
        root.addView(scrollView, scrollParams);

        setContentView(root);
    }

    private void loadWorkoutHistory() {
        if (firebaseAuth.getCurrentUser() == null) {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Please login to view workout history.");
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .collection("workoutHistory")
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    historyContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        tvStatus.setText(
                                "No workout history yet. Complete a workout to see it here."
                        );
                        return;
                    }

                    tvStatus.setText("Completed sessions: " + querySnapshot.size());

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String exerciseName = document.getString("exerciseName");
                        Long setsCompleted = document.getLong("setsCompleted");
                        Long repsPerSet = document.getLong("repsPerSet");
                        Boolean completed = document.getBoolean("completed");
                        Object completedAt = document.get("completedAt");

                        // Older workout records may not contain postureAccuracy.
                        Long postureAccuracy = document.getLong("postureAccuracy");
                        Long postureAccuracySets = document.getLong("postureAccuracySets");

                        if (exerciseName == null || exerciseName.trim().isEmpty()) {
                            exerciseName = "Workout";
                        }

                        addHistoryCard(
                                exerciseName,
                                setsCompleted,
                                repsPerSet,
                                completed,
                                completedAt,
                                postureAccuracy,
                                postureAccuracySets
                        );
                    }
                })
                .addOnFailureListener(error -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Could not load history. Please try again.");
                });
    }

    private void addHistoryCard(
            String exerciseName,
            Long setsCompleted,
            Long repsPerSet,
            Boolean completed,
            Object completedAt,
            Long postureAccuracy,
            Long postureAccuracySets
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);

        TextView name = new TextView(this);
        name.setText(exerciseName);
        name.setTextSize(19);
        name.setTextColor(Color.rgb(30, 35, 100));
        name.setTypeface(null, Typeface.BOLD);
        card.addView(name);

        String setsText = setsCompleted == null ? "Not recorded" : String.valueOf(setsCompleted);
        String repsText = repsPerSet == null ? "Not recorded" : String.valueOf(repsPerSet);

        TextView details = new TextView(this);
        details.setText("Sets completed: " + setsText + "\nReps per set: " + repsText);
        details.setTextSize(15);
        details.setTextColor(Color.DKGRAY);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailsParams.topMargin = dp(8);
        card.addView(details, detailsParams);

        TextView accuracy = new TextView(this);
        if (postureAccuracy == null) {
            accuracy.setText("Posture Accuracy: Not recorded for this workout");
            accuracy.setTextColor(Color.GRAY);
        } else {
            int safeAccuracy = Math.max(0, Math.min(100, postureAccuracy.intValue()));
            String accuracyText = "Posture Accuracy: " + safeAccuracy + "%";
            if (postureAccuracySets != null && postureAccuracySets > 0) {
                accuracyText += "\nCamera-checked sets: " + postureAccuracySets;
            }
            accuracy.setText(accuracyText);
            accuracy.setTextColor(
                    safeAccuracy >= 80
                            ? Color.rgb(46, 125, 50)
                            : safeAccuracy >= 60
                              ? Color.rgb(230, 126, 34)
                              : Color.rgb(198, 40, 40)
            );
            accuracy.setTypeface(null, Typeface.BOLD);
        }
        accuracy.setTextSize(15);

        LinearLayout.LayoutParams accuracyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        accuracyParams.topMargin = dp(8);
        card.addView(accuracy, accuracyParams);

        TextView status = new TextView(this);
        status.setText(Boolean.TRUE.equals(completed) ? "Status: Completed" : "Status: Incomplete");
        status.setTextSize(14);
        status.setTextColor(
                Boolean.TRUE.equals(completed) ? Color.rgb(46, 125, 50) : Color.DKGRAY
        );

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(8);
        card.addView(status, statusParams);

        TextView date = new TextView(this);
        date.setText(formatDate(completedAt));
        date.setTextSize(13);
        date.setTextColor(Color.GRAY);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(6);
        card.addView(date, dateParams);

        historyContainer.addView(card, cardParams);
    }

    private String formatDate(Object value) {
        if (value instanceof Timestamp) {
            Date date = ((Timestamp) value).toDate();
            return DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT,
                    Locale.getDefault()
            ).format(date);
        }

        return "Date unavailable";
    }
}
