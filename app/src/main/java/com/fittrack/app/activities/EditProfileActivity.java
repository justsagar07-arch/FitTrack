package com.fittrack.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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
    private TextInputEditText etHeight;
    private TextInputEditText etWeight;
    private TextInputEditText etStepGoal;
    private TextInputEditText etCalorieGoal;

    private MaterialButton btnSaveProfile;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String userId;

    private final Calendar selectedDob =
            Calendar.getInstance();

    private final SimpleDateFormat firestoreDateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
            );

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            );


    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_edit_profile
        );

        // =========================================
        // FIREBASE
        // =========================================

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();


        // =========================================
        // INITIALIZE VIEWS
        // =========================================

        initializeViews();


        // =========================================
        // GENDER DROPDOWN
        // =========================================

        setupGenderDropdown();


        // =========================================
        // DOB PICKER
        // =========================================

        setupDateOfBirthPicker();


        // =========================================
        // LOAD EXISTING PROFILE
        // =========================================

        loadUserProfile();


        // =========================================
        // SAVE BUTTON
        // =========================================

        btnSaveProfile.setOnClickListener(
                v -> saveUserProfile()
        );
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private void initializeViews() {

        etFullName =
                findViewById(
                        R.id.et_full_name
                );

        etUsername =
                findViewById(
                        R.id.et_username
                );

        etDob =
                findViewById(
                        R.id.et_dob
                );

        etAge =
                findViewById(
                        R.id.et_age
                );

        actGender =
                findViewById(
                        R.id.act_gender
                );

        etHeight =
                findViewById(
                        R.id.et_height
                );

        etWeight =
                findViewById(
                        R.id.et_weight
                );

        etStepGoal =
                findViewById(
                        R.id.et_step_goal
                );

        etCalorieGoal =
                findViewById(
                        R.id.et_calorie_goal
                );

        btnSaveProfile =
                findViewById(
                        R.id.btn_save_profile
                );
    }


    // =====================================================
    // GENDER DROPDOWN
    // =====================================================

    private void setupGenderDropdown() {

        String[] genders = {
                "Male",
                "Female",
                "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_dropdown_item_1line,
                        genders
                );

        actGender.setAdapter(adapter);

        actGender.setOnClickListener(
                v -> actGender.showDropDown()
        );

        actGender.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {

                        actGender.showDropDown();
                    }
                }
        );
    }


    // =====================================================
    // DATE OF BIRTH PICKER
    // =====================================================

    private void setupDateOfBirthPicker() {

        etDob.setOnClickListener(
                v -> showDateOfBirthPicker()
        );
    }


    private void showDateOfBirthPicker() {

        Calendar today =
                Calendar.getInstance();

        int year =
                selectedDob.get(
                        Calendar.YEAR
                );

        int month =
                selectedDob.get(
                        Calendar.MONTH
                );

        int day =
                selectedDob.get(
                        Calendar.DAY_OF_MONTH
                );


        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, selectedYear,
                         selectedMonth,
                         selectedDay) -> {

                            selectedDob.set(
                                    Calendar.YEAR,
                                    selectedYear
                            );

                            selectedDob.set(
                                    Calendar.MONTH,
                                    selectedMonth
                            );

                            selectedDob.set(
                                    Calendar.DAY_OF_MONTH,
                                    selectedDay
                            );

                            updateDobAndAge();
                        },
                        year,
                        month,
                        day
                );


        // Future DOB is not allowed.
        dialog.getDatePicker()
                .setMaxDate(
                        today.getTimeInMillis()
                );


        dialog.show();
    }


    // =====================================================
    // UPDATE DOB + AGE
    // =====================================================

    private void updateDobAndAge() {

        etDob.setText(
                displayDateFormat.format(
                        selectedDob.getTime()
                )
        );

        int age =
                calculateAge(
                        selectedDob
                );

        etAge.setText(
                String.valueOf(age)
        );
    }


    // =====================================================
    // CALCULATE AGE
    // =====================================================

    private int calculateAge(
            Calendar birthDate
    ) {

        Calendar today =
                Calendar.getInstance();

        int age =
                today.get(
                        Calendar.YEAR
                )
                        -
                        birthDate.get(
                                Calendar.YEAR
                        );


        int currentMonth =
                today.get(
                        Calendar.MONTH
                );

        int birthMonth =
                birthDate.get(
                        Calendar.MONTH
                );

        int currentDay =
                today.get(
                        Calendar.DAY_OF_MONTH
                );

        int birthDay =
                birthDate.get(
                        Calendar.DAY_OF_MONTH
                );


        if (currentMonth < birthMonth
                || (
                currentMonth == birthMonth
                        && currentDay < birthDay
        )) {

            age--;
        }

        return age;
    }


    // =====================================================
    // LOAD USER PROFILE
    // =====================================================

    private void loadUserProfile() {

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        userId =
                currentUser.getUid();


        firestore
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {

                            if (!documentSnapshot.exists()) {

                                Toast.makeText(
                                        this,
                                        "Profile data not found",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            // =================================
                            // FULL NAME
                            // =================================

                            String fullName =
                                    documentSnapshot.getString(
                                            "fullName"
                                    );

                            if (fullName != null) {

                                etFullName.setText(
                                        fullName
                                );
                            }


                            // =================================
                            // USERNAME
                            // =================================

                            String username =
                                    documentSnapshot.getString(
                                            "username"
                                    );

                            if (username != null) {

                                etUsername.setText(
                                        username
                                );
                            }


                            // =================================
                            // DATE OF BIRTH
                            // =================================

                            String dateOfBirth =
                                    documentSnapshot.getString(
                                            "dateOfBirth"
                                    );

                            if (dateOfBirth != null
                                    && !dateOfBirth
                                    .trim()
                                    .isEmpty()) {

                                try {

                                    selectedDob.setTime(
                                            firestoreDateFormat
                                                    .parse(
                                                            dateOfBirth
                                                    )
                                    );

                                    etDob.setText(
                                            displayDateFormat
                                                    .format(
                                                            selectedDob
                                                                    .getTime()
                                                    )
                                    );

                                    int calculatedAge =
                                            calculateAge(
                                                    selectedDob
                                            );

                                    etAge.setText(
                                            String.valueOf(
                                                    calculatedAge
                                            )
                                    );

                                } catch (Exception ignored) {

                                    // Keep existing age
                                }

                            } else {

                                // =================================
                                // OLD PROFILE SUPPORT
                                // =================================

                                Long age =
                                        documentSnapshot
                                                .getLong("age");

                                if (age != null) {

                                    etAge.setText(
                                            String.valueOf(
                                                    age
                                            )
                                    );
                                }
                            }


                            // =================================
                            // GENDER
                            // =================================

                            String gender =
                                    documentSnapshot.getString(
                                            "gender"
                                    );

                            if (gender != null
                                    && !gender
                                    .trim()
                                    .isEmpty()) {

                                actGender.setText(
                                        gender,
                                        false
                                );
                            }


                            // =================================
                            // HEIGHT
                            // =================================

                            Double height =
                                    documentSnapshot
                                            .getDouble(
                                                    "height"
                                            );

                            if (height != null) {

                                etHeight.setText(
                                        String.valueOf(
                                                height
                                        )
                                );
                            }


                            // =================================
                            // WEIGHT
                            // =================================

                            Double weight =
                                    documentSnapshot
                                            .getDouble(
                                                    "weight"
                                            );

                            if (weight != null) {

                                etWeight.setText(
                                        String.valueOf(
                                                weight
                                        )
                                );
                            }


                            // =================================
                            // DAILY STEP GOAL
                            // =================================

                            Long stepGoal =
                                    documentSnapshot
                                            .getLong(
                                                    "dailyStepGoal"
                                            );

                            if (stepGoal != null) {

                                etStepGoal.setText(
                                        String.valueOf(
                                                stepGoal
                                        )
                                );
                            }


                            // =================================
                            // DAILY CALORIES GOAL
                            // =================================

                            Long calorieGoal =
                                    documentSnapshot
                                            .getLong(
                                                    "dailyCalorieGoal"
                                            );

                            if (calorieGoal != null) {

                                etCalorieGoal.setText(
                                        String.valueOf(
                                                calorieGoal
                                        )
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {

                            Toast.makeText(
                                    this,
                                    "Failed to load profile: "
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // =====================================================
    // SAVE USER PROFILE
    // =====================================================

    private void saveUserProfile() {

        if (userId == null) {

            Toast.makeText(
                    this,
                    "User not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =========================================
        // GET VALUES
        // =========================================

        String fullName =
                getText(etFullName);

        String username =
                getText(etUsername);

        String dateOfBirth =
                getText(etDob);

        String ageText =
                getText(etAge);

        String gender =
                actGender.getText()
                        .toString()
                        .trim();

        String heightText =
                getText(etHeight);

        String weightText =
                getText(etWeight);

        String stepGoalText =
                getText(etStepGoal);

        String calorieGoalText =
                getText(etCalorieGoal);


        // =========================================
        // VALIDATION
        // =========================================

        if (fullName.isEmpty()) {

            etFullName.setError(
                    "Enter your full name"
            );

            etFullName.requestFocus();

            return;
        }


        if (username.isEmpty()) {

            etUsername.setError(
                    "Enter your username"
            );

            etUsername.requestFocus();

            return;
        }


        if (dateOfBirth.isEmpty()) {

            etDob.setError(
                    "Select your date of birth"
            );

            etDob.requestFocus();

            return;
        }


        if (gender.isEmpty()) {

            actGender.setError(
                    "Select your gender"
            );

            actGender.requestFocus();

            actGender.showDropDown();

            return;
        }


        if (heightText.isEmpty()) {

            etHeight.setError(
                    "Enter your height"
            );

            etHeight.requestFocus();

            return;
        }


        if (weightText.isEmpty()) {

            etWeight.setError(
                    "Enter your weight"
            );

            etWeight.requestFocus();

            return;
        }


        if (stepGoalText.isEmpty()) {

            etStepGoal.setError(
                    "Enter your daily step goal"
            );

            etStepGoal.requestFocus();

            return;
        }


        if (calorieGoalText.isEmpty()) {

            etCalorieGoal.setError(
                    "Enter your daily calories goal"
            );

            etCalorieGoal.requestFocus();

            return;
        }


        // =========================================
        // CONVERT NUMBERS
        // =========================================

        int age;
        double height;
        double weight;
        int stepGoal;
        int calorieGoal;


        try {

            age =
                    Integer.parseInt(
                            ageText
                    );

            height =
                    Double.parseDouble(
                            heightText
                    );

            weight =
                    Double.parseDouble(
                            weightText
                    );

            stepGoal =
                    Integer.parseInt(
                            stepGoalText
                    );

            calorieGoal =
                    Integer.parseInt(
                            calorieGoalText
                    );

        } catch (NumberFormatException error) {

            Toast.makeText(
                    this,
                    "Please enter valid numbers",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =========================================
        // VALIDATE NUMBERS
        // =========================================

        if (age < 0 || age > 120) {

            etAge.setError(
                    "Enter a valid age"
            );

            etAge.requestFocus();

            return;
        }


        if (height <= 0) {

            etHeight.setError(
                    "Enter a valid height"
            );

            etHeight.requestFocus();

            return;
        }


        if (weight <= 0) {

            etWeight.setError(
                    "Enter a valid weight"
            );

            etWeight.requestFocus();

            return;
        }


        if (stepGoal <= 0) {

            etStepGoal.setError(
                    "Enter a valid step goal"
            );

            etStepGoal.requestFocus();

            return;
        }


        if (calorieGoal <= 0) {

            etCalorieGoal.setError(
                    "Enter a valid calorie goal"
            );

            etCalorieGoal.requestFocus();

            return;
        }


        // =========================================
        // CREATE FIRESTORE DATA
        // =========================================

        Map<String, Object> profileData =
                new HashMap<>();


        profileData.put(
                "fullName",
                fullName
        );

        profileData.put(
                "username",
                username
        );

        profileData.put(
                "dateOfBirth",
                convertDisplayDateToFirestoreDate()
        );

        profileData.put(
                "age",
                age
        );

        profileData.put(
                "gender",
                gender
        );

        profileData.put(
                "height",
                height
        );

        profileData.put(
                "weight",
                weight
        );

        profileData.put(
                "dailyStepGoal",
                stepGoal
        );

        // =========================================
        // DAILY CALORIES GOAL
        // =========================================

        profileData.put(
                "dailyCalorieGoal",
                calorieGoal
        );


        // =========================================
        // SAVE TO FIRESTORE
        // =========================================

        firestore
                .collection("users")
                .document(userId)
                .set(
                        profileData,
                        SetOptions.merge()
                )
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "Profile saved successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            Toast.makeText(
                                    this,
                                    "Failed to save profile: "
                                            + error.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // =====================================================
    // CONVERT DOB TO FIRESTORE FORMAT
    // =====================================================

    private String convertDisplayDateToFirestoreDate() {

        return firestoreDateFormat.format(
                selectedDob.getTime()
        );
    }


    // =====================================================
    // GET TEXT
    // =====================================================

    private String getText(
            TextInputEditText editText
    ) {

        if (editText.getText() == null) {

            return "";
        }

        return editText.getText()
                .toString()
                .trim();
    }
}