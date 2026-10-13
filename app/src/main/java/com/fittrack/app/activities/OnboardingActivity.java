package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.health.connect.client.PermissionController;

import com.fittrack.app.R;
import com.fittrack.app.services.HealthConnectManager;

import java.util.Set;

public class OnboardingActivity extends AppCompatActivity {

    private static final String TAG =
            "FitTrackOnboarding";

    private HealthConnectManager healthConnectManager;

    private boolean permissionRequestInProgress =
            false;

    // ---------------------------------------------------------
    // HEALTH CONNECT PERMISSION RESULT
    // ---------------------------------------------------------

    private final ActivityResultLauncher<Set<String>>
            healthPermissionLauncher =
            registerForActivityResult(
                    PermissionController
                            .createRequestPermissionResultContract(),

                    grantedPermissions -> {

                        permissionRequestInProgress =
                                false;

                        Log.d(
                                TAG,
                                "Health Connect permission screen returned"
                        );

                        Log.d(
                                TAG,
                                "Granted permissions: "
                                        + grantedPermissions
                        );

                        Set<String> requiredPermissions =
                                healthConnectManager
                                        .getRequiredPermissions();

                        boolean granted =
                                grantedPermissions != null
                                        && grantedPermissions
                                        .containsAll(
                                                requiredPermissions
                                        );

                        Log.d(
                                TAG,
                                "Required permissions: "
                                        + requiredPermissions
                        );

                        Log.d(
                                TAG,
                                "Health Connect permissions granted: "
                                        + granted
                        );

                        if (granted) {

                            Toast.makeText(
                                    OnboardingActivity.this,
                                    "Health Connect connected",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Log.d(
                                    TAG,
                                    "Health Connect permissions granted"
                            );

                            navigateToMain();

                        } else {

                            Toast.makeText(
                                    OnboardingActivity.this,
                                    "Please allow Health Connect permissions",
                                    Toast.LENGTH_LONG
                            ).show();

                            Log.d(
                                    TAG,
                                    "Some required Health Connect permissions were not granted"
                            );
                        }
                    }
            );

    // ---------------------------------------------------------
    // ON CREATE
    // ---------------------------------------------------------

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_onboarding
        );

        Log.d(
                TAG,
                "OnboardingActivity started"
        );

        // Create Health Connect manager
        healthConnectManager =
                new HealthConnectManager(this);

        // -----------------------------------------------------
        // GRANT PERMISSIONS BUTTON
        // -----------------------------------------------------

        findViewById(R.id.btnGrant)
                .setOnClickListener(
                        v -> {

                            Log.d(
                                    TAG,
                                    "Grant Permissions button clicked"
                            );

                            requestHealthConnectPermissions();
                        }
                );

        // -----------------------------------------------------
        // SKIP BUTTON
        // -----------------------------------------------------

        findViewById(R.id.btnSkip)
                .setOnClickListener(
                        v -> {

                            Log.d(
                                    TAG,
                                    "Skip button clicked"
                            );

                            navigateToMain();
                        }
                );
    }

    // ---------------------------------------------------------
    // REQUEST HEALTH CONNECT PERMISSIONS
    // ---------------------------------------------------------

    private void requestHealthConnectPermissions() {

        Log.d(
                TAG,
                "requestHealthConnectPermissions() called"
        );

        // Prevent duplicate request
        if (permissionRequestInProgress) {

            Log.d(
                    TAG,
                    "Permission request already in progress"
            );

            return;
        }

        // Check Health Connect availability
        if (!healthConnectManager.isAvailable()) {

            Log.e(
                    TAG,
                    "Health Connect is not available"
            );

            Toast.makeText(
                    this,
                    "Health Connect is not available on this device",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Get required permissions
        Set<String> permissions =
                healthConnectManager
                        .getRequiredPermissions();

        Log.d(
                TAG,
                "Permissions to request: "
                        + permissions
        );

        // Safety check
        if (permissions == null
                || permissions.isEmpty()) {

            Log.e(
                    TAG,
                    "No Health Connect permissions configured"
            );

            Toast.makeText(
                    this,
                    "No Health Connect permissions configured",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Start permission request
        permissionRequestInProgress =
                true;

        Log.d(
                TAG,
                "Launching Health Connect permission screen"
        );

        healthPermissionLauncher.launch(
                permissions
        );
    }

    // ---------------------------------------------------------
    // NAVIGATE TO MAIN ACTIVITY
    // ---------------------------------------------------------

    private void navigateToMain() {

        Log.d(
                TAG,
                "Navigating to MainActivity"
        );

        Intent intent =
                new Intent(
                        OnboardingActivity.this,
                        MainActivity.class
                );

        startActivity(intent);

        finish();
    }
}