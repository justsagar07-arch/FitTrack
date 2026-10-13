package com.fittrack.app.data;

import com.fittrack.app.models.Exercise;
import com.fittrack.app.models.ExerciseDataProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ExerciseRepository {

    public interface Callback {
        void onLoaded(List<Exercise> exercises, boolean fromFirestore);
    }

    public static void loadExercises(Callback callback) {
        FirebaseFirestore.getInstance()
                .collection("exercise")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Exercise> exercises = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        String description = doc.getString("description");
                        String difficulty = doc.getString("difficulty");
                        String durationOrReps = doc.getString("durationOrReps");
                        String safetyTips = doc.getString("safetyTips");

                        if (name != null && category != null) {
                            exercises.add(new Exercise(
                                    name,
                                    category,
                                    description == null ? "" : description,
                                    difficulty == null ? "Beginner" : difficulty,
                                    durationOrReps == null ? "" : durationOrReps,
                                    safetyTips == null ? "" : safetyTips
                            ));
                        }
                    }

                    if (exercises.isEmpty()) {
                        callback.onLoaded(
                                ExerciseDataProvider.getAllExercises(),
                                false
                        );
                    } else {
                        callback.onLoaded(exercises, true);
                    }
                })
                .addOnFailureListener(error -> {
                    callback.onLoaded(
                            ExerciseDataProvider.getAllExercises(),
                            false
                    );
                });
    }
}