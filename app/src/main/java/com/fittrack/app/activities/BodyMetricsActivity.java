package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.fittrack.app.utils.UnitUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BodyMetricsActivity extends AppCompatActivity {

    private TextView tvHeight;
    private TextView tvWeight;
    private TextView tvBmi;
    private TextView tvBmiCategory;
    private TextView tvBmiInfo;
    private TextView tvAge;
    private TextView tvBmr;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_body_metrics
        );

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        tvHeight =
                findViewById(R.id.tv_height);

        tvWeight =
                findViewById(R.id.tv_weight);

        tvBmi =
                findViewById(R.id.tv_bmi);

        tvBmiCategory =
                findViewById(R.id.tv_bmi_category);

        tvBmiInfo =
                findViewById(R.id.tv_bmi_info);

        tvAge =
                findViewById(R.id.tv_age);

        tvBmr =
                findViewById(R.id.tv_bmr);

        // -----------------------------------------
        // BACK BUTTON
        // -----------------------------------------

        findViewById(R.id.btn_back)
                .setOnClickListener(
                        v -> finish()
                );

        // -----------------------------------------
        // ADVANCED BODY ANALYSIS
        // -----------------------------------------

        findViewById(R.id.btn_advanced_analysis)
                .setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    BodyMetricsActivity.this,
                                    BodyAnalysisActivity.class
                            );

                    startActivity(intent);
                });

        loadBodyMetrics();
    }

    // =================================================
    // LOAD BODY METRICS
    // =================================================

    private void loadBodyMetrics() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            showEmptyValues();

            return;
        }

        String userId =
                user.getUid();

        firestore
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {

                            if (!documentSnapshot.exists()) {

                                showEmptyValues();

                                return;
                            }

                            // -----------------------------------------
                            // HEIGHT
                            // -----------------------------------------

                            Double heightCm =
                                    getNumber(
                                            documentSnapshot.get(
                                                    "height"
                                            )
                                    );

                            // -----------------------------------------
                            // WEIGHT
                            // -----------------------------------------

                            Double weightKg =
                                    getNumber(
                                            documentSnapshot.get(
                                                    "weight"
                                            )
                                    );

                            // -----------------------------------------
                            // DOB
                            // -----------------------------------------

                            String dateOfBirth =
                                    documentSnapshot.getString(
                                            "dateOfBirth"
                                    );

                            // -----------------------------------------
                            // GENDER
                            // -----------------------------------------

                            String gender =
                                    documentSnapshot.getString(
                                            "gender"
                                    );

                            // -----------------------------------------
                            // AGE FROM DOB
                            // -----------------------------------------

                            Integer age =
                                    calculateAgeFromDob(
                                            dateOfBirth
                                    );

                            if (age == null) {

                                Double oldAge =
                                        getNumber(
                                                documentSnapshot.get(
                                                        "age"
                                                )
                                        );

                                if (oldAge != null
                                        && oldAge > 0) {

                                    age =
                                            oldAge.intValue();
                                }
                            }

                            updateMetrics(
                                    heightCm,
                                    weightKg,
                                    age,
                                    gender
                            );
                        }
                )
                .addOnFailureListener(
                        e -> showEmptyValues()
                );
    }

    // =================================================
    // NUMBER HELPER
    // =================================================

    private Double getNumber(
            Object value
    ) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {

            return
                    ((Number) value)
                            .doubleValue();
        }

        if (value instanceof String) {

            try {

                return Double.parseDouble(
                        ((String) value).trim()
                );

            } catch (Exception ignored) {

                return null;
            }
        }

        return null;
    }

    // =================================================
    // DOB → AGE
    // =================================================

    private Integer calculateAgeFromDob(
            String dateOfBirth
    ) {

        if (dateOfBirth == null
                || dateOfBirth.trim().isEmpty()) {

            return null;
        }

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            format.setLenient(false);

            Date birthDate =
                    format.parse(
                            dateOfBirth.trim()
                    );

            if (birthDate == null) {

                return null;
            }

            Calendar dob =
                    Calendar.getInstance();

            dob.setTime(birthDate);

            Calendar today =
                    Calendar.getInstance();

            int age =
                    today.get(
                            Calendar.YEAR
                    )
                            - dob.get(
                            Calendar.YEAR
                    );

            int currentMonth =
                    today.get(
                            Calendar.MONTH
                    );

            int birthMonth =
                    dob.get(
                            Calendar.MONTH
                    );

            int currentDay =
                    today.get(
                            Calendar.DAY_OF_MONTH
                    );

            int birthDay =
                    dob.get(
                            Calendar.DAY_OF_MONTH
                    );

            if (currentMonth < birthMonth
                    || (
                    currentMonth == birthMonth
                            && currentDay < birthDay
            )) {

                age--;
            }

            if (age < 0
                    || age > 120) {

                return null;
            }

            return age;

        } catch (Exception e) {

            return null;
        }
    }

    // =================================================
    // UPDATE METRICS
    // =================================================

    private void updateMetrics(
            Double heightCm,
            Double weightKg,
            Integer age,
            String gender
    ) {

        // -----------------------------------------
        // HEIGHT & WEIGHT
        // -----------------------------------------

        tvHeight.setText(UnitUtils.formatHeight(this, heightCm));
        tvWeight.setText(UnitUtils.formatWeight(this, weightKg));

        // -----------------------------------------
        // AGE
        // -----------------------------------------

        if (age != null
                && age > 0) {

            tvAge.setText(
                    String.valueOf(age)
            );

        } else {

            tvAge.setText(
                    "--"
            );
        }

        // -----------------------------------------
        // BMI & CATEGORY & INFO
        // -----------------------------------------

        if (heightCm != null
                && heightCm > 0
                && weightKg != null
                && weightKg > 0) {

            double heightMeters =
                    heightCm / 100.0;

            double bmi =
                    weightKg
                            / (
                            heightMeters
                                    * heightMeters
                    );

            tvBmi.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f",
                            bmi
                    )
            );

            if (age != null && age < 20) {
                if (tvBmiCategory != null) {
                    tvBmiCategory.setText("Category: Pediatric / Teen Notice");
                }
                if (tvBmiInfo != null) {
                    tvBmiInfo.setText("For children and teenagers under 20, BMI is interpreted using age- and sex-specific growth percentiles rather than adult cutoffs. Please consult a healthcare professional for pediatric BMI growth charts.");
                }
            } else {
                if (bmi < 18.5) {
                    if (tvBmiCategory != null) {
                        tvBmiCategory.setText("Category: Underweight");
                    }
                    if (tvBmiInfo != null) {
                        tvBmiInfo.setText("Your BMI indicates you are in the underweight category. A balanced diet and strength training may help achieve a healthy weight. Consider consulting a healthcare professional for personalized guidance.");
                    }
                } else if (bmi < 25.0) {
                    if (tvBmiCategory != null) {
                        tvBmiCategory.setText("Category: Normal / Healthy Weight");
                    }
                    if (tvBmiInfo != null) {
                        tvBmiInfo.setText("Your BMI is within the healthy weight range (18.5 - 24.9). Maintaining regular physical activity and a balanced diet helps support long-term wellness.");
                    }
                } else if (bmi < 30.0) {
                    if (tvBmiCategory != null) {
                        tvBmiCategory.setText("Category: Overweight");
                    }
                    if (tvBmiInfo != null) {
                        tvBmiInfo.setText("Your BMI indicates you are in the overweight category (25.0 - 29.9). Regular physical activity and nutritious meal planning can support healthy weight management.");
                    }
                } else {
                    if (tvBmiCategory != null) {
                        tvBmiCategory.setText("Category: Obesity");
                    }
                    if (tvBmiInfo != null) {
                        tvBmiInfo.setText("Your BMI indicates you are in the obesity category (30.0 or higher). Small, consistent lifestyle modifications in activity and nutrition can improve overall health. Consider speaking with a healthcare provider for a supportive plan.");
                    }
                }
            }

        } else {

            tvBmi.setText(
                    "--"
            );

            if (tvBmiCategory != null) {
                tvBmiCategory.setText("Category: Data Missing");
            }

            if (tvBmiInfo != null) {
                tvBmiInfo.setText("Height and weight values are required in your profile to calculate BMI and determine category.");
            }
        }

        // -----------------------------------------
        // BMR
        // -----------------------------------------

        if (heightCm != null
                && heightCm > 0
                && weightKg != null
                && weightKg > 0
                && age != null
                && age > 0
                && gender != null
                && !gender.trim().isEmpty()) {

            double bmr =
                    calculateBmr(
                            weightKg,
                            heightCm,
                            age,
                            gender
                    );

            if (bmr > 0) {

                tvBmr.setText(
                        String.format(
                                Locale.getDefault(),
                                "%.0f kcal/day",
                                bmr
                        )
                );

            } else {

                tvBmr.setText(
                        "-- kcal/day"
                );
            }

        } else {

            tvBmr.setText(
                    "-- kcal/day"
            );
        }
    }

    // =================================================
    // BMR CALCULATION
    // =================================================

    private double calculateBmr(
            double weightKg,
            double heightCm,
            int age,
            String gender
    ) {

        if (weightKg <= 0
                || heightCm <= 0
                || age <= 0) {

            return 0;
        }

        String normalizedGender =
                gender
                        .trim()
                        .toLowerCase(
                                Locale.getDefault()
                        );

        if (age >= 10
                && age <= 18) {

            if (isMale(normalizedGender)) {

                return
                        (17.686 * weightKg)
                                + 658.2;

            } else if (
                    isFemale(normalizedGender)
            ) {

                return
                        (13.384 * weightKg)
                                + 692.6;
            }
        }

        if (age >= 19) {

            if (isMale(normalizedGender)) {

                return
                        (10.0 * weightKg)
                                + (
                                6.25
                                        * heightCm
                        )
                                - (
                                5.0
                                        * age
                        )
                                + 5.0;

            } else if (
                    isFemale(normalizedGender)
            ) {

                return
                        (10.0 * weightKg)
                                + (
                                6.25
                                        * heightCm
                        )
                                - (
                                5.0
                                        * age
                        )
                                - 161.0;
            }
        }

        return 0;
    }

    private boolean isMale(
            String gender
    ) {

        return gender.equals("male")
                || gender.equals("m")
                || gender.equals("man")
                || gender.equals("boy")
                || gender.equals("boys");
    }

    private boolean isFemale(
            String gender
    ) {

        return gender.equals("female")
                || gender.equals("f")
                || gender.equals("woman")
                || gender.equals("girl")
                || gender.equals("girls");
    }

    // =================================================
    // EMPTY VALUES
    // =================================================

    private void showEmptyValues() {

        tvHeight.setText(UnitUtils.formatHeight(this, null));

        tvWeight.setText(UnitUtils.formatWeight(this, null));

        tvAge.setText(
                "--"
        );

        tvBmi.setText(
                "--"
        );

        if (tvBmiCategory != null) {
            tvBmiCategory.setText("Category: Data Missing");
        }

        if (tvBmiInfo != null) {
            tvBmiInfo.setText("Height and weight values are required in your profile to calculate BMI and determine category.");
        }

        tvBmr.setText(
                "-- kcal/day"
        );
    }
}
