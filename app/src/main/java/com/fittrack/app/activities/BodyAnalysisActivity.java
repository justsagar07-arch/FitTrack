
package com.fittrack.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fittrack.app.R;
import com.fittrack.app.services.MongoPhotoUploadService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BodyAnalysisActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final long MAX_PHOTO_SIZE = 5L * 1024L * 1024L;

    private PreviewView previewView;
    private ProgressBar progressScanningBar;
    private View layoutScannerOverlay;
    private View layoutCameraControls;
    private ScrollView layoutResultsOverlay;
    private TextView textInstructions;

    private TextView textResultWeight;
    private TextView textResultBmi;
    private TextView textResultBmiCategory;
    private TextView textResultBmiInfo;
    private TextView textResultBodyFat;
    private TextView textResultHeight;
    private TextView textResultMuscle;
    private TextView textResultPhysique;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private boolean photoUploadInProgress = false;
    private boolean savingResults = false;
    private String bodyAnalysisPhotoId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_analysis);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        previewView = findViewById(R.id.preview_view);
        progressScanningBar = findViewById(R.id.progress_scanning_bar);
        layoutScannerOverlay = findViewById(R.id.layout_scanner_overlay);
        layoutCameraControls = findViewById(R.id.layout_camera_controls);
        layoutResultsOverlay = findViewById(R.id.layout_results_overlay);
        textInstructions = findViewById(R.id.text_instructions);

        textResultBmi = findViewById(R.id.text_result_bmi);
        textResultBmiCategory = findViewById(R.id.text_result_bmi_category);
        textResultBmiInfo = findViewById(R.id.text_result_bmi_info);
        textResultBodyFat = findViewById(R.id.text_result_body_fat);
        textResultMuscle = findViewById(R.id.text_result_muscle);
        textResultPhysique = findViewById(R.id.text_result_physique);

        int weightId = getResources().getIdentifier(
                "text_result_weight", "id", getPackageName());

        int heightId = getResources().getIdentifier(
                "text_result_height", "id", getPackageName());

        if (weightId != 0) {
            textResultWeight = findViewById(weightId);
        }

        if (heightId != 0) {
            textResultHeight = findViewById(heightId);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_capture).setOnClickListener(
                v -> captureAndUploadPhoto());

        findViewById(R.id.btn_results_done).setOnClickListener(
                v -> saveAdvancedResults());

        findViewById(R.id.btn_results_retake).setOnClickListener(
                v -> resetCaptureView());

        showUnavailableResults();

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void showUnavailableResults() {
        setText(textResultWeight, "Not Available");
        setText(textResultHeight, "Not Available");
        setText(textResultBmi, "Not Available");
        setText(textResultBmiCategory, "Not Available");

        setText(
                textResultBmiInfo,
                "Advanced body measurements are not available yet. "
                        + "A validated scan-analysis model is required."
        );

        setText(textResultBodyFat, "Not Available");
        setText(textResultMuscle, "Not Available");
        setText(textResultPhysique, "Not Available");

        setOptionalText("text_result_body_water", "Not Available");
        setOptionalText("text_result_visceral_fat", "Not Available");
        setOptionalText("text_result_bmr", "Not Available");
        setOptionalText("text_result_metabolic_age", "Not Available");
    }

    private void setText(TextView view, String value) {
        if (view != null) {
            view.setText(value);
        }
    }

    private void setOptionalText(String idName, String value) {
        int id = getResources().getIdentifier(
                idName, "id", getPackageName());

        if (id != 0) {
            TextView view = findViewById(id);
            if (view != null) {
                view.setText(value);
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(75)
                        .build();

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                );

            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Camera initialization failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndUploadPhoto() {
        if (imageCapture == null) {
            showMessage("Camera is not ready. Please wait.");
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            showMessage("Please log in first.");
            return;
        }

        if (photoUploadInProgress) {
            return;
        }

        photoUploadInProgress = true;

        findViewById(R.id.btn_capture).setEnabled(false);
        progressScanningBar.setVisibility(View.VISIBLE);
        textInstructions.setText("Capturing photo...");

        File photoFile = new File(
                getCacheDir(),
                "body_analysis_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults output) {

                        if (isFinishing() || isDestroyed()) {
                            photoUploadInProgress = false;
                            photoFile.delete();
                            return;
                        }

                        if (!photoFile.exists()
                                || photoFile.length() == 0
                                || photoFile.length() > MAX_PHOTO_SIZE) {

                            photoFile.delete();

                            handleFailure(
                                    "Photo must be valid and 5 MB or smaller."
                            );
                            return;
                        }

                        uploadPhotoToPostgreSQL(photoFile);
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception) {

                        photoFile.delete();

                        if (isFinishing() || isDestroyed()) {
                            photoUploadInProgress = false;
                            return;
                        }

                        handleFailure(
                                "Photo capture failed: "
                                        + exception.getMessage()
                        );
                    }
                }
        );
    }

    private void uploadPhotoToPostgreSQL(File photoFile) {
        textInstructions.setText("Uploading photo...");

        Uri photoUri = Uri.fromFile(photoFile);

        MongoPhotoUploadService.uploadPhoto(
                BodyAnalysisActivity.this,
                photoUri,
                "body-analysis",
                new MongoPhotoUploadService.UploadCallback() {

                    @Override
                    public void onSuccess(String photoId) {
                        photoFile.delete();

                        if (isFinishing() || isDestroyed()) {
                            photoUploadInProgress = false;
                            return;
                        }

                        bodyAnalysisPhotoId = photoId;
                        photoUploadInProgress = false;

                        progressScanningBar.setVisibility(View.GONE);
                        layoutResultsOverlay.setVisibility(View.VISIBLE);

                        showUnavailableResults();

                        textInstructions.setText(
                                "Photo saved. Measurements are not available yet."
                        );

                        showMessage("Photo saved to PostgreSQL");
                    }

                    @Override
                    public void onFailure(String error) {
                        photoFile.delete();

                        if (isFinishing() || isDestroyed()) {
                            photoUploadInProgress = false;
                            return;
                        }

                        handleFailure("Photo upload failed: " + error);
                    }
                }
        );
    }

    private void handleFailure(String message) {
        photoUploadInProgress = false;

        progressScanningBar.setVisibility(View.GONE);
        findViewById(R.id.btn_capture).setEnabled(true);

        textInstructions.setText(
                "Align your full body within the frame"
        );

        showMessage(message);
    }

    private void resetCaptureView() {
        if (photoUploadInProgress || savingResults) {
            showMessage("Please wait for the current operation to finish.");
            return;
        }

        bodyAnalysisPhotoId = null;

        findViewById(R.id.btn_capture).setEnabled(true);
        progressScanningBar.setVisibility(View.GONE);
        layoutResultsOverlay.setVisibility(View.GONE);

        showUnavailableResults();

        textInstructions.setText(
                "Align your full body within the frame"
        );

        textInstructions.setTextColor(
                ContextCompat.getColor(this, R.color.accent_green)
        );
    }

    private void saveAdvancedResults() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            showMessage("Please log in first.");
            return;
        }

        if (photoUploadInProgress || savingResults) {
            showMessage("Please wait for the current operation to finish.");
            return;
        }

        if (bodyAnalysisPhotoId == null) {
            showMessage("Capture and upload a photo before saving.");
            return;
        }

        savingResults = true;

        Map<String, Object> scan = new HashMap<>();

        scan.put("userId", user.getUid());
        scan.put("bodyAnalysisPhotoId", bodyAnalysisPhotoId);

        scan.put("height", "Not Available");
        scan.put("weight", "Not Available");
        scan.put("bmi", "Not Available");
        scan.put("bmiCategory", "Not Available");

        scan.put(
                "bmiInfo",
                "A validated scan-analysis model is not integrated yet."
        );

        scan.put("bodyFatPercentage", "Not Available");
        scan.put("muscleMass", "Not Available");
        scan.put("bodyWaterPercentage", "Not Available");
        scan.put("visceralFat", "Not Available");
        scan.put("bmr", "Not Available");
        scan.put("metabolicAge", "Not Available");
        scan.put("physiqueClassification", "Not Available");
        scan.put("timestamp", System.currentTimeMillis());

        firestore.collection("users")
                .document(user.getUid())
                .collection("advancedBodyAnalysis")
                .document("latest")
                .set(scan)
                .addOnSuccessListener(unused -> {
                    savingResults = false;
                    showMessage("Advanced scan record saved.");
                    finish();
                })
                .addOnFailureListener(error -> {
                    savingResults = false;

                    showMessage(
                            "Could not save scan record: "
                                    + error.getMessage()
                    );
                });
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera();

            } else {
                showMessage("Camera permission is required.");
                finish();
            }
        }
    }
}
