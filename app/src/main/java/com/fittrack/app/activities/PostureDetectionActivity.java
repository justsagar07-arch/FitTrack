package com.fittrack.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
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
import com.fittrack.app.views.OverlayView;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;

public class PostureDetectionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1002;

    private PreviewView posturePreviewView;
    private OverlayView skeletonOverlayView;
    private TextView textPostureFeedback;
    private MaterialCardView cardFeedback;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posture_detection);

        posturePreviewView = findViewById(R.id.posture_preview_view);
        skeletonOverlayView = findViewById(R.id.skeleton_overlay_view);
        textPostureFeedback = findViewById(R.id.text_posture_feedback);
        cardFeedback = findViewById(R.id.card_feedback);

        findViewById(R.id.btn_posture_back).setOnClickListener(v -> finish());

        // Connect real-time callback from overlay simulation loop
        skeletonOverlayView.setOnPostureChangeListener((isCorrect, feedback) -> {
            textPostureFeedback.setText(feedback);
            if (isCorrect) {
                textPostureFeedback.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                cardFeedback.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_green)));
            } else {
                textPostureFeedback.setTextColor(Color.parseColor("#FF5252"));
                cardFeedback.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF5252")));
            }
        });

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(posturePreviewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (Exception e) {
                Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is mandatory for Real-Time Posture Detection.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}