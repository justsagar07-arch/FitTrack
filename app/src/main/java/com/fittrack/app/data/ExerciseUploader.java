
package com.fittrack.app.data;

import android.util.Log;

import com.fittrack.app.models.Exercise;
import com.fittrack.app.models.ExerciseDataProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseUploader {

    private static final String TAG = "ExerciseUploader";
    private static final String COLLECTION = "exercise";

    public interface UploadCallback {
        void onSuccess(int count);
        void onFailure(String error);
    }

    public static void uploadExercises(UploadCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Exercise> exercises =
                ExerciseDataProvider.getAllExercises();

        if (exercises == null || exercises.isEmpty()) {
            callback.onFailure("Exercise list is empty.");
            return;
        }

        uploadNext(db, exercises, 0, 0, callback);
    }

    private static void uploadNext(
            FirebaseFirestore db,
            List<Exercise> exercises,
            int index,
            int uploadedCount,
            UploadCallback callback
    ) {

        if (index >= exercises.size()) {
            Log.d(TAG, "Uploaded exercises: " + uploadedCount);
            callback.onSuccess(uploadedCount);
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

        String documentId = exercise.getName()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        db.collection(COLLECTION)
                .document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    uploadNext(
                            db,
                            exercises,
                            index + 1,
                            uploadedCount + 1,
                            callback
                    );
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            TAG,
                            "Upload failed for " + exercise.getName(),
                            error
                    );

                    callback.onFailure(
                            "Upload failed for "
                                    + exercise.getName()
                                    + ": "
                                    + error.getMessage()
                    );
                });
    }
}
