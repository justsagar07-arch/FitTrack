
package com.fittrack.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fittrack.app.R;
import com.fittrack.app.activities.EditProfileActivity;
import com.fittrack.app.activities.LoginActivity;
import com.fittrack.app.activities.PersonalGoalsActivity;
import com.fittrack.app.activities.RemindersActivity;
import com.fittrack.app.activities.SettingsActivity;
import com.fittrack.app.activities.WorkoutHistoryActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends BaseFragment {

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private ImageView ivProfilePic;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private ActivityResultLauncher<String> imagePickerLauncher;

    private static final String PROFILE_PHOTO_FILE =
            "profile_photo.jpg";

    private static final String PREFS_NAME =
            "FitTrackProfile";

    private static final String KEY_PROFILE_PHOTO =
            "profile_photo_path";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveProfilePhotoLocally(uri);
                    }
                }
        );

        ivProfilePic.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );

        loadUserProfile();

        // EDIT PROFILE
        view.findViewById(R.id.btn_edit_profile)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            requireActivity(),
                            EditProfileActivity.class
                    );
                    startActivity(intent);
                });

        // WORKOUT HISTORY
        view.findViewById(R.id.btn_workout_history)
                .setOnClickListener(v -> {

                    if (firebaseAuth.getCurrentUser() == null) {
                        Toast.makeText(
                                requireContext(),
                                "Please login to view workout history",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Intent intent = new Intent(
                            requireActivity(),
                            WorkoutHistoryActivity.class
                    );
                    startActivity(intent);
                });

        // PERSONAL GOALS
        View btnPersonalGoals = view.findViewById(R.id.btn_personal_goals);
        if (btnPersonalGoals != null) {
            btnPersonalGoals.setOnClickListener(v -> {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(
                            requireContext(),
                            "Please login to view personal goals",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                Intent intent = new Intent(
                        requireActivity(),
                        PersonalGoalsActivity.class
                );
                startActivity(intent);
            });
        }

        // HEALTH SETTINGS
        view.findViewById(R.id.btn_health_settings)
                .setOnClickListener(v ->
                        Toast.makeText(
                                requireContext(),
                                "Health Settings clicked",
                                Toast.LENGTH_SHORT
                        ).show()
                );

        // REMINDERS
        view.findViewById(R.id.btn_reminders)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            requireActivity(),
                            RemindersActivity.class
                    );
                    startActivity(intent);
                });

        // SETTINGS
        view.findViewById(R.id.btn_settings)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            requireActivity(),
                            SettingsActivity.class
                    );
                    startActivity(intent);
                });

        // LOGOUT
        view.findViewById(R.id.btn_logout)
                .setOnClickListener(v -> logoutUser());

        return view;
    }

    // LOAD USER PROFILE
    private void loadUserProfile() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            tvProfileName.setText("Guest");
            tvProfileEmail.setText("Not logged in");
            ivProfilePic.setImageResource(R.drawable.ic_person);
            return;
        }

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();

        if (email != null && !email.isEmpty()) {
            tvProfileEmail.setText(email);
        } else {
            tvProfileEmail.setText("Email not available");
        }

        loadLocalProfilePhoto();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {

                        String fullName =
                                documentSnapshot.getString("fullName");

                        String username =
                                documentSnapshot.getString("username");

                        if (fullName != null &&
                                !fullName.trim().isEmpty()) {

                            tvProfileName.setText(fullName);

                        } else if (username != null &&
                                !username.trim().isEmpty()) {

                            tvProfileName.setText(username);

                        } else {
                            tvProfileName.setText("FitTrack User");
                        }

                    } else {
                        tvProfileName.setText("FitTrack User");
                    }
                })
                .addOnFailureListener(error -> {

                    if (!isAdded()) return;

                    tvProfileName.setText("FitTrack User");

                    Toast.makeText(
                            requireContext(),
                            "Could not load profile",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // SAVE PROFILE PHOTO LOCALLY
    private void saveProfilePhotoLocally(Uri imageUri) {

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(
                    requireContext(),
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            File profilePhotoFile = new File(
                    requireContext().getFilesDir(),
                    PROFILE_PHOTO_FILE
            );

            try (InputStream inputStream =
                         requireContext().getContentResolver()
                                 .openInputStream(imageUri)) {

                if (inputStream == null) {
                    Toast.makeText(
                            requireContext(),
                            "Could not read selected photo",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                try (FileOutputStream outputStream =
                             new FileOutputStream(profilePhotoFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    outputStream.flush();
                }
            }

            requireContext()
                    .getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                    )
                    .edit()
                    .putString(
                            KEY_PROFILE_PHOTO,
                            profilePhotoFile.getAbsolutePath()
                    )
                    .apply();

            displayProfilePhoto(profilePhotoFile.getAbsolutePath());

            Toast.makeText(
                    requireContext(),
                    "Profile photo updated",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception error) {
            Toast.makeText(
                    requireContext(),
                    "Photo save failed: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // LOAD LOCAL PROFILE PHOTO
    private void loadLocalProfilePhoto() {

        String savedPhotoPath = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PROFILE_PHOTO, null);

        if (savedPhotoPath == null || savedPhotoPath.isEmpty()) {
            ivProfilePic.setImageResource(R.drawable.ic_person);
            return;
        }

        File photoFile = new File(savedPhotoPath);

        if (!photoFile.exists()) {
            ivProfilePic.setImageResource(R.drawable.ic_person);
            return;
        }

        displayProfilePhoto(savedPhotoPath);
    }

    // DISPLAY PROFILE PHOTO
    private void displayProfilePhoto(String photoPath) {

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);

            if (bitmap != null) {
                ivProfilePic.setImageBitmap(bitmap);
            } else {
                ivProfilePic.setImageResource(R.drawable.ic_person);
            }

        } catch (Exception error) {
            ivProfilePic.setImageResource(R.drawable.ic_person);
        }
    }

    // LOGOUT
    private void logoutUser() {

        firebaseAuth.signOut();

        Intent intent = new Intent(
                requireActivity(),
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }

    // REFRESH PROFILE
    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null && tvProfileName != null) {
            loadUserProfile();
        }
    }
}
