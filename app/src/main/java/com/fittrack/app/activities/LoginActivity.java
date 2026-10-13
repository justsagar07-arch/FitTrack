package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fittrack.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private MaterialButton btnLogin;
    private ProgressBar pbLoading;

    // Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Find Views
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        pbLoading = findViewById(R.id.pbLoading);

        // Login Button
        btnLogin.setOnClickListener(v -> performLogin());

        // Forgot Password
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );

            startActivity(intent);
        });

        // Sign Up
        findViewById(R.id.tvSignUp).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    SignUpActivity.class
            );

            startActivity(intent);
        });
    }

    /**
     * Perform Firebase Login
     */
    private void performLogin() {

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim()
                : "";

        boolean isValid = true;

        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // -------------------------
        // Email Validation
        // -------------------------

        if (email.isEmpty()) {

            tilEmail.setError("Email cannot be empty");
            isValid = false;

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }

        // -------------------------
        // Password Validation
        // -------------------------

        if (password.isEmpty()) {

            tilPassword.setError("Password cannot be empty");
            isValid = false;
        }

        // Stop if validation failed
        if (!isValid) {
            return;
        }

        // -------------------------
        // Show Loading
        // -------------------------

        btnLogin.setEnabled(false);
        pbLoading.setVisibility(View.VISIBLE);

        // -------------------------
        // Firebase Login
        // -------------------------

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    // Hide Loading
                    btnLogin.setEnabled(true);
                    pbLoading.setVisibility(View.GONE);

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login Successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // Open Onboarding Screen
                            Intent intent = new Intent(
                                    LoginActivity.this,
                                    OnboardingActivity.class
                            );

                            startActivity(intent);

                            // Close Login Activity
                            finish();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "User not found.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        String errorMessage = "Login failed.";

                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}