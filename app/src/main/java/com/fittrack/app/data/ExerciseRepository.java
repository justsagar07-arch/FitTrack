
package com.fittrack.app.data;

import com.fittrack.app.models.Exercise;
import com.fittrack.app.models.ExerciseDataProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExerciseRepository {

    public interface Callback {
        void onLoaded(List<Exercise> exercises, boolean fromFirestore);
    }

    public interface SyncCallback {
        void onComplete(int uploaded, int alreadyPresent, String error);
    }

    private static String makeKey(String name, String category) {
        return (category == null ? "" : category.trim().toLowerCase(Locale.ROOT))
                + "|"
                + (name == null ? "" : name.trim().toLowerCase(Locale.ROOT));
    }

    public static void loadExercises(Callback callback) {
        FirebaseFirestore.getInstance()
                .collection("exercise")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Exercise> exercises = new ArrayList<>();
                    Set<String> existingKeys = new HashSet<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        String category = doc.getString("category");

                        if (name == null || category == null) {
                            continue;
                        }

                        exercises.add(new Exercise(
                                name,
                                category,
                                valueOrDefault(doc.getString("description"), ""),
                                valueOrDefault(doc.getString("difficulty"), "Beginner"),
                                valueOrDefault(doc.getString("durationOrReps"), ""),
                                valueOrDefault(doc.getString("safetyTips"), "")
                        ));

                        existingKeys.add(makeKey(name, category));
                    }

                    for (Exercise exercise : ExerciseDataProvider.getAllExercises()) {
                        String key = makeKey(
                                exercise.getName(),
                                exercise.getCategory()
                        );

                        if (existingKeys.add(key)) {
                            exercises.add(exercise);
                        }
                    }

                    callback.onLoaded(exercises, !snapshot.isEmpty());
                })
                .addOnFailureListener(error ->
                        callback.onLoaded(
                                ExerciseDataProvider.getAllExercises(),
                                false
                        )
                );
    }

    // Upload missing local exercises without deleting existing documents.
    public static void syncLocalExercisesToFirestore(SyncCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("exercise")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, String> existingDocumentIds = new HashMap<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String category = doc.getString("category");

                        if (name != null && category != null) {
                            existingDocumentIds.put(
                                    makeKey(name, category),
                                    doc.getId()
                            );
                        }
                    }

                    List<Exercise> localExercises =
                            ExerciseDataProvider.getAllExercises();

                    List<Exercise> missingExercises = new ArrayList<>();

                    for (Exercise exercise : localExercises) {
                        String key = makeKey(
                                exercise.getName(),
                                exercise.getCategory()
                        );

                        if (!existingDocumentIds.containsKey(key)) {
                            missingExercises.add(exercise);
                            // Reserve the key to prevent duplicate local entries.
                            existingDocumentIds.put(key, "");
                        }
                    }

                    if (missingExercises.isEmpty()) {
                        if (callback != null) {
                            callback.onComplete(0, localExercises.size(), null);
                        }
                        return;
                    }

                    uploadNext(db, missingExercises, 0, 0, callback);
                })
                .addOnFailureListener(error -> {
                    if (callback != null) {
                        callback.onComplete(
                                0, 0, error.getLocalizedMessage()
                        );
                    }
                });
    }

    private static void uploadNext(
            FirebaseFirestore db,
            List<Exercise> exercises,
            int index,
            int uploaded,
            SyncCallback callback
    ) {
        if (index >= exercises.size()) {
            if (callback != null) {
                callback.onComplete(uploaded, 0, null);
            }
            return;
        }

        Exercise exercise = exercises.get(index);

        Map<String, Object> data = new HashMap<>();
        data.put("name", exercise.getName());
        data.put("category", exercise.getCategory());
        data.put("description", exercise.getDescription());
        data.put("difficulty", exercise.getDifficulty());
        data.put("durationOrReps", exercise.getDurationOrReps());
        data.put("safetyTips", exercise.getSafetyTips());

        String documentId = makeKey(
                exercise.getName(),
                exercise.getCategory()
        ).replace("|", "_").replace("/", "_");

        db.collection("exercise")
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        uploadNext(
                                db, exercises, index + 1, uploaded + 1, callback
                        )
                )
                .addOnFailureListener(error -> {
                    if (callback != null) {
                        callback.onComplete(
                                uploaded, 0, error.getLocalizedMessage()
                        );
                    }
                });
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
