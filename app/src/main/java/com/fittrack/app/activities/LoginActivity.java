package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.fittrack.app.R;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "FitTrackLogin";

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;

    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;
    private ProgressBar pbLoading;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        pbLoading = findViewById(R.id.pbLoading);

        btnLogin.setOnClickListener(v -> performLogin());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(
                        LoginActivity.this,
                        ForgotPasswordActivity.class
                ))
        );

        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(
                        LoginActivity.this,
                        SignUpActivity.class
                ))
        );
    }

    private void performLogin() {
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";

        String password = etPassword.getText() != null
                ? etPassword.getText().toString() : "";

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Email cannot be empty");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password cannot be empty");
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            showFirebaseUserId(user);
                            Toast.makeText(
                                    this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            openOnboarding();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Login succeeded, but user details are unavailable.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Login failed.";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void signInWithGoogle() {
        setLoading(true);

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id)
                        )
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new CredentialManagerCallback<GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse response) {
                        runOnUiThread(() ->
                                handleGoogleCredential(
                                        response.getCredential()
                                )
                        );
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            setLoading(false);

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Google sign-in cancelled or failed: "
                                            + e.getLocalizedMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void handleGoogleCredential(Credential credential) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {

            try {
                GoogleIdTokenCredential googleCredential =
                        GoogleIdTokenCredential.createFrom(
                                ((CustomCredential) credential).getData()
                        );

                firebaseAuthWithGoogle(
                        googleCredential.getIdToken()
                );

            } catch (Exception e) {
                setLoading(false);

                Toast.makeText(
                        this,
                        "Could not read Google sign-in credential.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } else {
            setLoading(false);

            Toast.makeText(
                    this,
                    "Unsupported sign-in credential.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            showFirebaseUserId(user);

                            Toast.makeText(
                                    this,
                                    "Google login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            openOnboarding();
                        }
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Firebase Google login failed.";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // Firebase UID display and Logcat output
    private void showFirebaseUserId(FirebaseUser user) {
        String uid = user.getUid();

        Log.d(TAG, "Firebase UID: " + uid);

        Toast.makeText(
                this,
                "Firebase UID: " + uid,
                Toast.LENGTH_LONG
        ).show();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
        pbLoading.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );
    }

    private void openOnboarding() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }
}