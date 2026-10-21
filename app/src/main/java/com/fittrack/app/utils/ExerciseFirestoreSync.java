
package com.fittrack.app.utils;

import com.fittrack.app.models.Exercise;
import com.fittrack.app.models.ExerciseDataProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ExerciseFirestoreSync {

    private static final String COLLECTION = "exercise";

    private ExerciseFirestoreSync() {
    }

    public interface SyncCallback {
        void onSuccess(int added, int alreadyPresent, int totalAfterSync);

        void onError(String message);
    }

    public static void syncMissingExercises(SyncCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Exercise> localExercises =
                ExerciseDataProvider.getAllExercises();

        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {

                    Set<String> existingNames = new HashSet<>();
                    Set<String> existingIds = new HashSet<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        existingIds.add(normalize(document.getId()));

                        String storedName = document.getString("name");

                        if (storedName != null && !storedName.trim().isEmpty()) {
                            existingNames.add(normalize(storedName));
                        } else {
                            // Fallback for documents without a name field.
                            existingNames.add(normalize(document.getId()));
                        }
                    }

                    WriteBatch batch = db.batch();

                    int added = 0;
                    int alreadyPresent = 0;

                    for (Exercise exercise : localExercises) {

                        String name = exercise.getName();

                        if (name == null || name.trim().isEmpty()) {
                            continue;
                        }

                        String normalizedName = normalize(name);
                        String documentId = makeDocumentId(name);
                        String normalizedId = normalize(documentId);

                        // Never overwrite an existing exercise.
                        if (existingNames.contains(normalizedName)
                                || existingIds.contains(normalizedId)) {
                            alreadyPresent++;
                            continue;
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("name", exercise.getName());
                        data.put("category", exercise.getCategory());
                        data.put("description", exercise.getDescription());
                        data.put("difficulty", exercise.getDifficulty());
                        data.put("durationOrReps", exercise.getDurationOrReps());
                        data.put("safetyTips", exercise.getSafetyTips());

                        batch.set(
                                db.collection(COLLECTION).document(documentId),
                                data
                        );

                        // Prevent duplicate writes during this same sync.
                        existingNames.add(normalizedName);
                        existingIds.add(normalizedId);
                        added++;
                    }

                    final int addedCount = added;
                    final int existingCount = alreadyPresent;
                    final int expectedTotal = snapshot.size() + addedCount;

                    if (addedCount == 0) {
                        callback.onSuccess(
                                0,
                                existingCount,
                                snapshot.size()
                        );
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(
                                            addedCount,
                                            existingCount,
                                            expectedTotal
                                    )
                            )
                            .addOnFailureListener(error ->
                                    callback.onError(
                                            "Firestore write failed: "
                                                    + error.getMessage()
                                    )
                            );
                })
                .addOnFailureListener(error ->
                        callback.onError(
                                "Firestore read failed: "
                                        + error.getMessage()
                        )
                );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static String makeDocumentId(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
