package com.fittrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.fittrack.app.R;
import com.fittrack.app.models.Exercise;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    private List<Exercise> exercises;
    private OnExerciseClickListener listener;

    public ExerciseAdapter(List<Exercise> exercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    public void updateList(List<Exercise> newList) {
        this.exercises = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.tvName.setText(exercise.getName());
        holder.tvDesc.setText(exercise.getDescription());
        holder.tvDifficulty.setText(exercise.getDifficulty());
        holder.tvDuration.setText(exercise.getDurationOrReps());

        // Dynamic difficulty badge background tint
        int badgeColor;
        if (exercise.getDifficulty().equalsIgnoreCase("Beginner")) {
            badgeColor = R.color.card_workout;
        } else if (exercise.getDifficulty().equalsIgnoreCase("Intermediate")) {
            badgeColor = R.color.card_diet;
        } else {
            badgeColor = R.color.card_heart;
        }
        holder.tvDifficulty.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), badgeColor));

        holder.cardExercise.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExerciseClick(exercise);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardExercise;
        TextView tvName, tvDesc, tvDifficulty, tvDuration;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardExercise = itemView.findViewById(R.id.card_exercise);
            tvName = itemView.findViewById(R.id.tv_exercise_name);
            tvDesc = itemView.findViewById(R.id.tv_exercise_desc);
            tvDifficulty = itemView.findViewById(R.id.tv_exercise_difficulty);
            tvDuration = itemView.findViewById(R.id.tv_exercise_duration);
        }
    }
}
