package com.fittrack.app.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
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
import com.fittrack.app.activities.AboutUsActivity;
import com.fittrack.app.activities.EditProfileActivity;
import com.fittrack.app.activities.HealthSettingsActivity;
import com.fittrack.app.activities.LoginActivity;
import com.fittrack.app.activities.PersonalGoalsActivity;
import com.fittrack.app.activities.RemindersActivity;
import com.fittrack.app.activities.SettingsActivity;
import com.fittrack.app.activities.WorkoutHistoryActivity;
import com.fittrack.app.services.MongoPhotoDownloadService;
import com.fittrack.app.services.MongoPhotoUploadService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends BaseFragment {

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private ImageView ivProfilePic;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean photoUploadInProgress = false;

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
                        uploadProfilePhoto(uri);
                    }
                }
        );

        ivProfilePic.setOnClickListener(v -> {
            if (!photoUploadInProgress) {
                imagePickerLauncher.launch("image/*");
            }
        });

        loadUserProfile();

        view.findViewById(R.id.btn_edit_profile)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                requireActivity(),
                                EditProfileActivity.class
                        ))
                );

        view.findViewById(R.id.btn_workout_history)
                .setOnClickListener(v -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        showMessage("Please login to view workout history");
                        return;
                    }

                    startActivity(new Intent(
                            requireActivity(),
                            WorkoutHistoryActivity.class
                    ));
                });

        View btnPersonalGoals =
                view.findViewById(R.id.btn_personal_goals);

        if (btnPersonalGoals != null) {
            btnPersonalGoals.setOnClickListener(v -> {
                if (firebaseAuth.getCurrentUser() == null) {
                    showMessage("Please login to view personal goals");
                    return;
                }

                startActivity(new Intent(
                        requireActivity(),
                        PersonalGoalsActivity.class
                ));
            });
        }

        View btnHealthSettings =
                view.findViewById(R.id.btn_health_settings);

        if (btnHealthSettings != null) {
            btnHealthSettings.setOnClickListener(v ->
                    startActivity(new Intent(
                            requireActivity(),
                            HealthSettingsActivity.class
                    ))
            );
        }

        view.findViewById(R.id.btn_reminders)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                requireActivity(),
                                RemindersActivity.class
                        ))
                );

        View btnAboutUs = view.findViewById(R.id.btn_about_us);

        if (btnAboutUs != null) {
            btnAboutUs.setOnClickListener(v ->
                    startActivity(new Intent(
                            requireActivity(),
                            AboutUsActivity.class
                    ))
            );
        }

        view.findViewById(R.id.btn_settings)
                .setOnClickListener(v ->
                        startActivity(new Intent(
                                requireActivity(),
                                SettingsActivity.class
                        ))
                );

        view.findViewById(R.id.btn_logout)
                .setOnClickListener(v -> logoutUser());

        return view;
    }

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

        tvProfileEmail.setText(
                email != null && !email.isEmpty()
                        ? email
                        : "Email not available"
        );

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (document.exists()) {
                        String fullName = document.getString("fullName");
                        String username = document.getString("username");

                        if (fullName != null && !fullName.trim().isEmpty()) {
                            tvProfileName.setText(fullName);
                        } else if (username != null
                                && !username.trim().isEmpty()) {
                            tvProfileName.setText(username);
                        } else {
                            tvProfileName.setText("FitTrack User");
                        }

                        String photoId =
                                document.getString("profilePhotoId");

                        if (photoId != null && !photoId.trim().isEmpty()) {
                            downloadProfilePhoto(photoId);
                        } else {
                            ivProfilePic.setImageResource(
                                    R.drawable.ic_person
                            );
                        }
                    } else {
                        tvProfileName.setText("FitTrack User");
                        ivProfilePic.setImageResource(R.drawable.ic_person);
                    }
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }

                    tvProfileName.setText("FitTrack User");
                    showMessage("Could not load profile: " + error.getMessage());
                });
    }

    private void uploadProfilePhoto(Uri imageUri) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            showMessage("Please login first");
            return;
        }

        if (photoUploadInProgress) {
            return;
        }

        photoUploadInProgress = true;

        MongoPhotoUploadService.uploadPhoto(
                requireContext(),
                imageUri,
                "profile",
                new MongoPhotoUploadService.UploadCallback() {
                    @Override
                    public void onSuccess(String photoId) {
                        if (!isAdded()) {
                            return;
                        }

                        firestore.collection("users")
                                .document(user.getUid())
                                .update("profilePhotoId", photoId)
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    photoUploadInProgress = false;
                                    showMessage("Profile photo updated");
                                    downloadProfilePhoto(photoId);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded()) {
                                        return;
                                    }

                                    photoUploadInProgress = false;
                                    showMessage(
                                            "Photo uploaded, but profile could not be updated: "
                                                    + error.getMessage()
                                    );
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) {
                            return;
                        }

                        photoUploadInProgress = false;
                        showMessage("Photo upload failed: " + error);
                    }
                }
        );
    }

    private void downloadProfilePhoto(String photoId) {
        if (!isAdded()) {
            return;
        }

        MongoPhotoDownloadService.downloadPhoto(
                requireContext(),
                photoId,
                new MongoPhotoDownloadService.DownloadCallback() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        if (isAdded()) {
                            ivProfilePic.setImageBitmap(bitmap);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) {
                            return;
                        }

                        ivProfilePic.setImageResource(R.drawable.ic_person);
                        showMessage("Photo download failed: " + error);
                    }
                }
        );
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void logoutUser() {
        firebaseAuth.signOut();

        Intent intent = new Intent(
                requireActivity(),
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null && tvProfileName != null) {
            loadUserProfile();
        }
    }
}