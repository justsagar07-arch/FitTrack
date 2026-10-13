package com.fittrack.app.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.fittrack.app.services.FirestoreService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // UI COMPONENTS
    // ---------------------------------------------------------

    private TextInputLayout tilFullName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private MaterialButton btnSignUp;

    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------

    private FirebaseAuth mAuth;
    private FirestoreService firestoreService;

    // ---------------------------------------------------------
    // ON CREATE
    // ---------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        firestoreService = new FirestoreService(this);

        // -----------------------------------------------------
        // TextInputLayouts
        // -----------------------------------------------------

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword =
                findViewById(R.id.tilConfirmPassword);

        // -----------------------------------------------------
        // EditTexts
        // -----------------------------------------------------

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        // -----------------------------------------------------
        // Sign Up Button
        // -----------------------------------------------------

        btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(
                v -> performSignUp()
        );

        // -----------------------------------------------------
        // Login
        // -----------------------------------------------------

        findViewById(R.id.tvLogin)
                .setOnClickListener(v -> finish());
    }

    // ---------------------------------------------------------
    // SIGN UP
    // ---------------------------------------------------------

    private void performSignUp() {

        String fullName = etFullName.getText()
                .toString()
                .trim();

        String email = etEmail.getText()
                .toString()
                .trim();

        String username = etUsername.getText()
                .toString()
                .trim();

        String password = etPassword.getText()
                .toString();

        String confirmPassword =
                etConfirmPassword.getText()
                        .toString();

        boolean isValid = true;

        // -----------------------------------------------------
        // Clear previous errors
        // -----------------------------------------------------

        tilFullName.setError(null);
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // -----------------------------------------------------
        // Full Name
        // -----------------------------------------------------

        if (fullName.isEmpty()) {

            tilFullName.setError(
                    "This field cannot be empty"
            );

            isValid = false;
        }

        // -----------------------------------------------------
        // Email
        // -----------------------------------------------------

        if (email.isEmpty()) {

            tilEmail.setError(
                    "This field cannot be empty"
            );

            isValid = false;

        } else if (
                !Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
        ) {

            tilEmail.setError(
                    "Invalid email format"
            );

            isValid = false;
        }

        // -----------------------------------------------------
        // Username
        // -----------------------------------------------------

        if (username.isEmpty()) {

            tilUsername.setError(
                    "This field cannot be empty"
            );

            isValid = false;

        } else if (username.length() < 3) {

            tilUsername.setError(
                    "Username must be at least 3 characters"
            );

            isValid = false;
        }

        // -----------------------------------------------------
        // Password
        // -----------------------------------------------------

        if (password.isEmpty()) {

            tilPassword.setError(
                    "This field cannot be empty"
            );

            isValid = false;

        } else if (password.length() < 6) {

            tilPassword.setError(
                    "Password must be at least 6 characters"
            );

            isValid = false;
        }

        // -----------------------------------------------------
        // Confirm Password
        // -----------------------------------------------------

        if (confirmPassword.isEmpty()) {

            tilConfirmPassword.setError(
                    "This field cannot be empty"
            );

            isValid = false;

        } else if (
                !confirmPassword.equals(password)
        ) {

            tilConfirmPassword.setError(
                    "Passwords do not match"
            );

            isValid = false;
        }

        // -----------------------------------------------------
        // Stop if validation failed
        // -----------------------------------------------------

        if (!isValid) {
            return;
        }

        // -----------------------------------------------------
        // Disable button
        // -----------------------------------------------------

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating Account...");

        // -----------------------------------------------------
        // CREATE FIREBASE AUTH USER
        // -----------------------------------------------------

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser user =
                                        mAuth.getCurrentUser();

                                if (user != null) {

                                    String uid =
                                            user.getUid();

                                    // -------------------------------------------------
                                    // SAVE USER PROFILE TO FIRESTORE
                                    // -------------------------------------------------

                                    firestoreService
                                            .saveUserProfile(
                                                    uid,
                                                    fullName,
                                                    email,
                                                    username
                                            );

                                    Toast.makeText(
                                            SignUpActivity.this,
                                            "Account Created Successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // -------------------------------------------------
                                    // Go back to Login
                                    // -------------------------------------------------

                                    finish();

                                } else {

                                    btnSignUp.setEnabled(true);
                                    btnSignUp.setText("Sign Up");

                                    Toast.makeText(
                                            SignUpActivity.this,
                                            "User account was created but user data was not found.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            } else {

                                // -------------------------------------------------
                                // Enable button again
                                // -------------------------------------------------

                                btnSignUp.setEnabled(true);
                                btnSignUp.setText("Sign Up");

                                String errorMessage =
                                        "Account creation failed";

                                if (task.getException() != null) {

                                    errorMessage =
                                            task.getException()
                                                    .getMessage();
                                }

                                Toast.makeText(
                                        SignUpActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }
}