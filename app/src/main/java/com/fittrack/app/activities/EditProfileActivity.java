
package com.fittrack.app.activities;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.fittrack.app.services.MongoPhotoUploadService;
import com.fittrack.app.utils.UnitUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.fittrack.app.services.MongoPhotoDownloadService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etUsername;
    private TextInputEditText etDob;
    private TextInputEditText etAge;
    private MaterialAutoCompleteTextView actGender;
    private TextInputLayout tilHeight;
    private TextInputLayout tilWeight;
    private TextInputEditText etHeight;
    private TextInputEditText etWeight;
    private TextInputEditText etStepGoal;
    private TextInputEditText etCalorieGoal;

    private MaterialButton btnSaveProfile;
    private MaterialButton btnSelectProfilePhoto;
    private ImageView ivProfilePhoto;
    private void loadProfilePhoto(String photoId) {
        if (photoId == null || photoId.trim().isEmpty()) {
            return;
        }

        MongoPhotoDownloadService.downloadPhoto(
                this,
                photoId,
                new MongoPhotoDownloadService.DownloadCallback() {
                    @Override
                    public void onSuccess(android.graphics.Bitmap bitmap) {
                        if (!isFinishing() && !isDestroyed()) {
                            ivProfilePhoto.setImageBitmap(bitmap);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(
                                EditProfileActivity.this,
                                "Could not load profile photo",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String userId;

    private Uri selectedPhotoUri;
    private boolean isSaving = false;

    private final Calendar selectedDob = Calendar.getInstance();

    private final SimpleDateFormat firestoreDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedPhotoUri = uri;
                            ivProfilePhoto.setImageURI(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupGenderDropdown();
        setupDateOfBirthPicker();
        loadUserProfile();

        btnSelectProfilePhoto.setOnClickListener(
                v -> photoPicker.launch("image/*")
        );

        btnSaveProfile.setOnClickListener(
                v -> saveUserProfile()
        );
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.et_full_name);
        etUsername = findViewById(R.id.et_username);
        etDob = findViewById(R.id.et_dob);
        etAge = findViewById(R.id.et_age);
        actGender = findViewById(R.id.act_gender);
        tilHeight = findViewById(R.id.til_height);
        tilWeight = findViewById(R.id.til_weight);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etStepGoal = findViewById(R.id.et_step_goal);
        etCalorieGoal = findViewById(R.id.et_calorie_goal);

        btnSaveProfile = findViewById(R.id.btn_save_profile);
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        btnSelectProfilePhoto = findViewById(R.id.btn_select_profile_photo);

        setupUnitLabels();
    }

    private void setupUnitLabels() {
        if (tilHeight != null) {
            tilHeight.setHint(
                    UnitUtils.isHeightFt(this)
                            ? "Height (ft)"
                            : "Height (cm)"
            );
        }

        if (tilWeight != null) {
            tilWeight.setHint(
                    UnitUtils.isWeightLbs(this)
                            ? "Weight (lbs)"
                            : "Weight (kg)"
            );
        }
    }

    private void setupGenderDropdown() {
        String[] genders = {"Male", "Female", "Other"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders
        );

        actGender.setAdapter(adapter);
        actGender.setOnClickListener(v -> actGender.showDropDown());

        actGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actGender.showDropDown();
            }
        });
    }

    private void setupDateOfBirthPicker() {
        etDob.setOnClickListener(v -> showDateOfBirthPicker());
    }

    private void showDateOfBirthPicker() {
        Calendar today = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDob.set(Calendar.YEAR, year);
                    selectedDob.set(Calendar.MONTH, month);
                    selectedDob.set(Calendar.DAY_OF_MONTH, day);
                    updateDobAndAge();
                },
                selectedDob.get(Calendar.YEAR),
                selectedDob.get(Calendar.MONTH),
                selectedDob.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMaxDate(today.getTimeInMillis());
        dialog.show();
    }

    private void updateDobAndAge() {
        etDob.setText(displayDateFormat.format(selectedDob.getTime()));
        etAge.setText(String.valueOf(calculateAge(selectedDob)));
    }

    private int calculateAge(Calendar birthDate) {
        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR)
                - birthDate.get(Calendar.YEAR);

        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH)
                || (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH)
                < birthDate.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }
                    String profilePhotoId =
                            document.getString("profilePhotoId");

                    loadProfilePhoto(profilePhotoId);

                    String fullName = document.getString("fullName");
                    if (fullName != null) {
                        etFullName.setText(fullName);
                    }

                    String username = document.getString("username");
                    if (username != null) {
                        etUsername.setText(username);
                    }

                    String dateOfBirth = document.getString("dateOfBirth");

                    if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                        try {
                            java.util.Date parsedDate =
                                    firestoreDateFormat.parse(dateOfBirth);

                            if (parsedDate != null) {
                                selectedDob.setTime(parsedDate);
                                etDob.setText(
                                        displayDateFormat.format(parsedDate)
                                );
                                etAge.setText(
                                        String.valueOf(calculateAge(selectedDob))
                                );
                            }
                        } catch (Exception ignored) {
                            // Invalid stored date: leave existing fields unchanged.
                        }
                    } else {
                        Long oldAge = document.getLong("age");
                        if (oldAge != null) {
                            etAge.setText(String.valueOf(oldAge));
                        }
                    }

                    String gender = document.getString("gender");
                    if (gender != null) {
                        actGender.setText(gender, false);
                    }

                    Double height = document.getDouble("height");
                    if (height != null) {
                        double displayHeight = UnitUtils.isHeightFt(this)
                                ? UnitUtils.cmToFeet(height)
                                : height;

                        etHeight.setText(String.format(
                                Locale.US,
                                UnitUtils.isHeightFt(this) ? "%.2f" : "%.0f",
                                displayHeight
                        ));
                    }

                    Double weight = document.getDouble("weight");
                    if (weight != null) {
                        double displayWeight = UnitUtils.isWeightLbs(this)
                                ? UnitUtils.kgToLbs(weight)
                                : weight;

                        etWeight.setText(String.format(
                                Locale.US,
                                "%.1f",
                                displayWeight
                        ));
                    }

                    Long stepGoal = document.getLong("dailyStepGoal");
                    if (stepGoal != null) {
                        etStepGoal.setText(String.valueOf(stepGoal));
                    }

                    Long calorieGoal = document.getLong("dailyCalorieGoal");
                    if (calorieGoal != null) {
                        etCalorieGoal.setText(String.valueOf(calorieGoal));
                    }

                    // Photo ID is stored in Firestore after upload.
                    // Authenticated photo download/display is a separate step.
                })
                .addOnFailureListener(error ->
                        Toast.makeText(
                                this,
                                "Failed to load profile: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void saveUserProfile() {
        if (isSaving) {
            return;
        }

        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = getText(etFullName);
        String username = getText(etUsername);
        String dateOfBirth = getText(etDob);
        String ageText = getText(etAge);
        String gender = actGender.getText().toString().trim();
        String heightText = getText(etHeight);
        String weightText = getText(etWeight);
        String stepGoalText = getText(etStepGoal);
        String calorieGoalText = getText(etCalorieGoal);

        if (fullName.isEmpty()) {
            etFullName.setError("Enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            etUsername.setError("Enter your username");
            etUsername.requestFocus();
            return;
        }

        if (dateOfBirth.isEmpty()) {
            etDob.setError("Select your date of birth");
            etDob.requestFocus();
            return;
        }

        if (gender.isEmpty()) {
            actGender.setError("Select your gender");
            actGender.requestFocus();
            actGender.showDropDown();
            return;
        }

        if (heightText.isEmpty()) {
            etHeight.setError("Enter your height");
            etHeight.requestFocus();
            return;
        }

        if (weightText.isEmpty()) {
            etWeight.setError("Enter your weight");
            etWeight.requestFocus();
            return;
        }

        if (stepGoalText.isEmpty()) {
            etStepGoal.setError("Enter your daily step goal");
            etStepGoal.requestFocus();
            return;
        }

        if (calorieGoalText.isEmpty()) {
            etCalorieGoal.setError("Enter your daily calories goal");
            etCalorieGoal.requestFocus();
            return;
        }

        int age;
        double height;
        double weight;
        int stepGoal;
        int calorieGoal;

        try {
            age = Integer.parseInt(ageText);
            height = Double.parseDouble(heightText);
            weight = Double.parseDouble(weightText);
            stepGoal = Integer.parseInt(stepGoalText);
            calorieGoal = Integer.parseInt(calorieGoalText);
        } catch (NumberFormatException error) {
            Toast.makeText(
                    this,
                    "Please enter valid numbers",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (age < 0 || age > 120) {
            etAge.setError("Enter a valid age");
            etAge.requestFocus();
            return;
        }

        if (height <= 0) {
            etHeight.setError("Enter a valid height");
            etHeight.requestFocus();
            return;
        }

        if (weight <= 0) {
            etWeight.setError("Enter a valid weight");
            etWeight.requestFocus();
            return;
        }

        if (stepGoal <= 0) {
            etStepGoal.setError("Enter a valid step goal");
            etStepGoal.requestFocus();
            return;
        }

        if (calorieGoal <= 0) {
            etCalorieGoal.setError("Enter a valid calorie goal");
            etCalorieGoal.requestFocus();
            return;
        }

        Map<String, Object> profileData = new HashMap<>();

        profileData.put("fullName", fullName);
        profileData.put("username", username);
        profileData.put(
                "dateOfBirth",
                firestoreDateFormat.format(selectedDob.getTime())
        );
        profileData.put("age", age);
        profileData.put("gender", gender);

        double heightCm = UnitUtils.isHeightFt(this)
                ? UnitUtils.feetToCm(height)
                : height;

        double weightKg = UnitUtils.isWeightLbs(this)
                ? UnitUtils.lbsToKg(weight)
                : weight;

        profileData.put("height", heightCm);
        profileData.put("weight", weightKg);
        profileData.put("dailyStepGoal", stepGoal);
        profileData.put("dailyCalorieGoal", calorieGoal);

        isSaving = true;
        btnSaveProfile.setEnabled(false);

        if (selectedPhotoUri != null) {
            uploadPhotoThenSaveProfile(profileData);
        } else {
            saveProfileData(profileData);
        }
    }

    private void uploadPhotoThenSaveProfile(
            Map<String, Object> profileData
    ) {
        MongoPhotoUploadService.uploadPhoto(
                this,
                selectedPhotoUri,
                "profile",
                new MongoPhotoUploadService.UploadCallback() {
                    @Override
                    public void onSuccess(String photoId) {
                        profileData.put("profilePhotoId", photoId);
                        saveProfileData(profileData);
                    }

                    @Override
                    public void onFailure(String error) {
                        isSaving = false;
                        btnSaveProfile.setEnabled(true);

                        Toast.makeText(
                                EditProfileActivity.this,
                                "Photo upload failed: " + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveProfileData(Map<String, Object> profileData) {
        firestore.collection("users")
                .document(userId)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    isSaving = false;

                    Toast.makeText(
                            this,
                            selectedPhotoUri != null
                                    ? "Profile and photo saved successfully"
                                    : "Profile saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(error -> {
                    isSaving = false;
                    btnSaveProfile.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Failed to save profile: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String getText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }
}
