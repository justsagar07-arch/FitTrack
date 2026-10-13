package com.fittrack.app.services;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreService {

    private final FirebaseFirestore db;
    private final Context context;

    public FirestoreService(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
    }

    // ---------------------------------------------------------
    // SAVE USER PROFILE
    // ---------------------------------------------------------

    public void saveUserProfile(
            String userId,
            String name,
            String email,
            String username
    ) {

        Map<String, Object> user = new HashMap<>();

        user.put("userId", userId);
        user.put("fullName", name);
        user.put("email", email);
        user.put("username", username);

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            context,
                            "User profile saved",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Error saving user profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    // ---------------------------------------------------------
    // GET USER PROFILE
    // ---------------------------------------------------------

    public void getUserProfile(
            String userId,
            OnSuccessListener<DocumentSnapshot> successListener
    ) {

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Error getting user profile: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    // ---------------------------------------------------------
    // GET TODAY ACTIVITY FOR CURRENT USER
    // ---------------------------------------------------------

    public void getTodayActivity(
            String userId,
            OnSuccessListener<DocumentSnapshot> successListener
    ) {

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(
                    context,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("activity")
                .document("today")
                .get()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Error getting activity data: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    // ---------------------------------------------------------
    // SAVE TODAY HEALTH DATA FOR CURRENT USER
    // ---------------------------------------------------------

    public void syncHealthStats(
            String userId,
            int steps,
            int calories,
            int heartRate
    ) {

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(
                    context,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> healthStats = new HashMap<>();

        healthStats.put("userId", userId);
        healthStats.put("project", "FitTrack");
        healthStats.put("steps", steps);
        healthStats.put("calories", calories);
        healthStats.put("heartRate", heartRate);
        healthStats.put(
                "timestamp",
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(userId)
                .collection("activity")
                .document("today")
                .set(healthStats)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            context,
                            "Activity saved to Firebase",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            context,
                            "Error saving activity: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
}