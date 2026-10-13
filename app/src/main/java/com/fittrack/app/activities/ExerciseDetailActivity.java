package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.fittrack.app.R;
import com.fittrack.app.models.Exercise;
import com.google.android.material.button.MaterialButton;

public class ExerciseDetailActivity extends AppCompatActivity {

    private Exercise exercise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        exercise = (Exercise) getIntent().getSerializableExtra("exercise");
        if (exercise == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(exercise.getName());
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvDifficulty = findViewById(R.id.tv_detail_difficulty);
        TextView tvDesc = findViewById(R.id.tv_detail_desc);
        TextView tvDuration = findViewById(R.id.tv_detail_duration);
        TextView tvSafety = findViewById(R.id.tv_detail_safety);
        MaterialButton btnStartWorkout = findViewById(R.id.btn_start_workout);

        tvName.setText(exercise.getName());
        tvCategory.setText(exercise.getCategory().toUpperCase());
        tvDifficulty.setText(exercise.getDifficulty());
        tvDesc.setText(exercise.getDescription());
        tvDuration.setText(exercise.getDurationOrReps());
        tvSafety.setText(exercise.getSafetyTips());

        // Set dynamic color for difficulty badge
        int badgeColor;
        if (exercise.getDifficulty().equalsIgnoreCase("Beginner")) {
            badgeColor = R.color.card_workout;
        } else if (exercise.getDifficulty().equalsIgnoreCase("Intermediate")) {
            badgeColor = R.color.card_diet;
        } else {
            badgeColor = R.color.card_heart;
        }
        tvDifficulty.setBackgroundColor(ContextCompat.getColor(this, badgeColor));

        btnStartWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseDetailActivity.this, WorkoutActiveActivity.class);
            intent.putExtra("exercise_name", exercise.getName());
            startActivity(intent);
        });
    }
}
