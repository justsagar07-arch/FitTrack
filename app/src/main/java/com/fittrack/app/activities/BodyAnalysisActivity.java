package com.fittrack.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fittrack.app.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BodyAnalysisActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1001;

    private PreviewView previewView;
    private ProgressBar progressScanningBar;
    private View layoutScannerOverlay;
    private View layoutCameraControls;
    private ScrollView layoutResultsOverlay;
    private TextView textInstructions;

    // Result TextViews
    private TextView textResultWeight;
    private TextView textResultBmi;
    private TextView textResultBodyFat;
    private TextView textResultHeight;
    private TextView textResultMuscle;
    private TextView textResultPhysique;

    private ProcessCameraProvider cameraProvider;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_analysis);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI Elements
        previewView = findViewById(R.id.preview_view);
        progressScanningBar = findViewById(R.id.progress_scanning_bar);
        layoutScannerOverlay = findViewById(R.id.layout_scanner_overlay);
        layoutCameraControls = findViewById(R.id.layout_camera_controls);
        layoutResultsOverlay = findViewById(R.id.layout_results_overlay);
        textInstructions = findViewById(R.id.text_instructions);

        // Result fields
        textResultBmi = findViewById(R.id.text_result_bmi);
        textResultBodyFat = findViewById(R.id.text_result_body_fat);
        textResultMuscle = findViewById(R.id.text_result_muscle);
        textResultPhysique = findViewById(R.id.text_result_physique);

        /*
         * These two IDs are optional.
         * If your XML does not currently have them,
         * we will add them later.
         */
        int weightId = getResources().getIdentifier(
                "text_result_weight",
                "id",
                getPackageName()
        );

        int heightId = getResources().getIdentifier(
                "text_result_height",
                "id",
                getPackageName()
        );

        if (weightId != 0) {
            textResultWeight = findViewById(weightId);
        }

        if (heightId != 0) {
            textResultHeight = findViewById(heightId);
        }

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Capture button
        findViewById(R.id.btn_capture)
                .setOnClickListener(v -> startScanningSimulation());

        // Save and Complete
        findViewById(R.id.btn_results_done)
                .setOnClickListener(v -> saveBodyAnalysisToFirestore());

        // Retake
        findViewById(R.id.btn_results_retake)
                .setOnClickListener(v -> resetCaptureView());

        // Check and Request Camera Permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA
            );
        }
    }

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                cameraProvider = cameraProviderFuture.get();

                Preview preview =
                        new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                CameraSelector cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Failed to initialize camera: "
                                + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void startScanningSimulation() {

        // Disable capture button
        findViewById(R.id.btn_capture).setEnabled(false);

        // Show progress
        progressScanningBar.setVisibility(View.VISIBLE);

        textInstructions.setText(
                "Analyzing body vectors... Keep still"
        );

        textInstructions.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.card_calories
                )
        );

        // Current project uses simulated scanning
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            progressScanningBar.setVisibility(View.GONE);

            layoutResultsOverlay.setVisibility(View.VISIBLE);

        }, 2500);
    }

    private void resetCaptureView() {

        findViewById(R.id.btn_capture).setEnabled(true);

        progressScanningBar.setVisibility(View.GONE);

        layoutResultsOverlay.setVisibility(View.GONE);

        textInstructions.setText(
                "Align your full body within the frame"
        );

        textInstructions.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.accent_green
                )
        );
    }

    private void saveBodyAnalysisToFirestore() {

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId = currentUser.getUid();

        /*
         * Current Body Analysis screen contains
         * simulated/display values.
         *
         * We save exactly what the current screen
         * displays instead of claiming these are
         * real camera measurements.
         */

        String bmi = getTextValue(
                textResultBmi,
                ""
        );

        String bodyFat = getTextValue(
                textResultBodyFat,
                ""
        );

        String muscleMass = getTextValue(
                textResultMuscle,
                ""
        );

        String physique = getTextValue(
                textResultPhysique,
                ""
        );

        String weight = getTextValue(
                textResultWeight,
                ""
        );

        String height = getTextValue(
                textResultHeight,
                ""
        );

        Map<String, Object> bodyAnalysis =
                new HashMap<>();

        bodyAnalysis.put(
                "userId",
                userId
        );

        bodyAnalysis.put(
                "weight",
                weight
        );

        bodyAnalysis.put(
                "bmi",
                bmi
        );

        bodyAnalysis.put(
                "bodyFatPercentage",
                bodyFat
        );

        bodyAnalysis.put(
                "height",
                height
        );

        bodyAnalysis.put(
                "muscleMass",
                muscleMass
        );

        bodyAnalysis.put(
                "physiqueClassification",
                physique
        );

        /*
         * These fields are reserved for future
         * real body-analysis calculations.
         */
        bodyAnalysis.put(
                "bodyWaterPercentage",
                ""
        );

        bodyAnalysis.put(
                "visceralFat",
                ""
        );

        bodyAnalysis.put(
                "bmr",
                ""
        );

        bodyAnalysis.put(
                "metabolicAge",
                ""
        );

        bodyAnalysis.put(
                "timestamp",
                System.currentTimeMillis()
        );

        firestore
                .collection("users")
                .document(userId)
                .collection("bodyAnalysis")
                .document("latest")
                .set(bodyAnalysis)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Body Analysis saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Failed to save Body Analysis: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String getTextValue(
            TextView textView,
            String defaultValue
    ) {

        if (textView == null) {
            return defaultValue;
        }

        String value =
                textView.getText()
                        .toString()
                        .trim();

        if (value.isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == PERMISSION_REQUEST_CAMERA) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startCamera();

            } else {

                Toast.makeText(
                        this,
                        "Camera permission is mandatory for Body Analysis feature.",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        }
    }
}