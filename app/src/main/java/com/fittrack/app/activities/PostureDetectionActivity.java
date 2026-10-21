package com.fittrack.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fittrack.app.R;
import com.fittrack.app.utils.LivePoseDetectionHelper;
import com.fittrack.app.views.OverlayView;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostureDetectionActivity extends AppCompatActivity {

    private static final String TAG = "PostureDetection";
    private static final int CAMERA_REQUEST = 1002;
    private static final int DEFAULT_TIMER_SECONDS = 60;

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView feedbackText;
    private TextView repCountText;
    private TextView exerciseNameText;
    private TextView postureAccuracyText;
    private MaterialCardView feedbackCard;
    private ImageButton switchCameraButton;

    private ProcessCameraProvider cameraProvider;
    private LivePoseDetectionHelper poseHelper;
    private ExecutorService cameraExecutor;

    private CameraSelector cameraSelector =
            CameraSelector.DEFAULT_BACK_CAMERA;

    private final Handler timerHandler =
            new Handler(Looper.getMainLooper());

    private boolean frontCamera = false;
    private volatile boolean cameraStarting = false;
    private volatile long lastTimestamp = 0L;

    private int targetReps = 15;
    private int targetDurationSeconds = DEFAULT_TIMER_SECONDS;
    private int validPostureSeconds = 0;

    private boolean timeBasedExercise = false;
    private volatile boolean postureCurrentlyCorrect = false;
    private volatile boolean resultSent = false;

    private long lastTimerTickMs = 0L;

    private final Runnable postureTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (resultSent || isFinishing() || isDestroyed()) {
                return;
            }

            long now = System.currentTimeMillis();

            if (lastTimerTickMs == 0L) {
                lastTimerTickMs = now;
            }

            long elapsedMs = now - lastTimerTickMs;
            lastTimerTickMs = now;

            if (postureCurrentlyCorrect) {
                long totalMs =
                        (long) validPostureSeconds * 1000L + elapsedMs;

                validPostureSeconds = (int) (totalMs / 1000L);
                lastTimerTickMs -= totalMs % 1000L;
            }

            updateTimerText();

            if (validPostureSeconds >= targetDurationSeconds) {
                finishSuccessfulTimedSet();
                return;
            }

            timerHandler.postDelayed(this, 200L);
        }
    };

    // Refresh the accuracy label while camera frames are being evaluated.
    private final Runnable postureAccuracyRunnable = new Runnable() {
        @Override
        public void run() {
            if (resultSent || isFinishing() || isDestroyed()) {
                return;
            }

            updatePostureAccuracy();
            timerHandler.postDelayed(this, 250L);
        }
    };

    private void updatePostureAccuracy() {
        if (overlayView == null || postureAccuracyText == null) {
            return;
        }

        double accuracy = overlayView.getPostureAccuracy();
        postureAccuracyText.setText(String.format(
                Locale.US,
                "POSTURE ACCURACY: %.0f%%",
                accuracy
        ));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posture_detection);

        previewView = findViewById(R.id.posture_preview_view);
        overlayView = findViewById(R.id.skeleton_overlay_view);
        feedbackText = findViewById(R.id.text_posture_feedback);
        repCountText = findViewById(R.id.text_rep_count);
        exerciseNameText = findViewById(R.id.text_exercise_name);
        postureAccuracyText = findViewById(R.id.text_posture_accuracy);
        feedbackCard = findViewById(R.id.card_feedback);
        switchCameraButton = findViewById(R.id.btn_switch_camera);

        String selectedExercise =
                getIntent().getStringExtra("exerciseName");

        if (selectedExercise == null
                || selectedExercise.trim().isEmpty()) {
            selectedExercise = "Squats";
        }

        targetReps = Math.max(
                1,
                getIntent().getIntExtra("target_reps", 15)
        );

        timeBasedExercise = getIntent().getBooleanExtra(
                "time_based", false
        );

        // Timer-based exercises default to 60 seconds.
        targetDurationSeconds = Math.max(
                1,
                getIntent().getIntExtra(
                        "target_duration_seconds",
                        DEFAULT_TIMER_SECONDS
                )
        );

        exerciseNameText.setText(
                "Current Exercise: " + selectedExercise
        );

        overlayView.setExercise(selectedExercise);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );

        findViewById(R.id.btn_posture_back)
                .setOnClickListener(v -> finish());

        switchCameraButton.setOnClickListener(v -> {
            if (cameraProvider == null
                    || cameraStarting
                    || resultSent) {
                return;
            }

            frontCamera = !frontCamera;

            cameraSelector = frontCamera
                    ? CameraSelector.DEFAULT_FRONT_CAMERA
                    : CameraSelector.DEFAULT_BACK_CAMERA;

            overlayView.clearPose();
            postureCurrentlyCorrect = false;
            startCamera();
        });

        overlayView.setOnPostureChangeListener((correct, message) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || resultSent) {
                        return;
                    }

                    postureCurrentlyCorrect = correct;
                    feedbackText.setText(message);

                    updatePostureAccuracy();

                    int color = ContextCompat.getColor(
                            this,
                            correct
                                    ? R.color.accent_green
                                    : android.R.color.holo_red_light
                    );

                    feedbackText.setTextColor(color);
                    feedbackCard.setStrokeColor(
                            ColorStateList.valueOf(color)
                    );

                    if (timeBasedExercise) {
                        updateTimerText();
                    }
                })
        );

        overlayView.setOnRepCountChangeListener(count ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || resultSent) {
                        return;
                    }

                    repCountText.setText(
                            "REPS: " + count + " / " + targetReps
                    );

                    if (!timeBasedExercise && count >= targetReps) {
                        finishSuccessfulRepSet(count);
                    }
                })
        );

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            initializePoseHelper();
            startCamera();

        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
        }

        updatePostureAccuracy();
        timerHandler.removeCallbacks(postureAccuracyRunnable);
        timerHandler.post(postureAccuracyRunnable);

        if (timeBasedExercise) {
            validPostureSeconds = 0;
            lastTimerTickMs = System.currentTimeMillis();
            updateTimerText();
            timerHandler.postDelayed(postureTimerRunnable, 200L);
        } else {
            repCountText.setText("REPS: 0 / " + targetReps);
        }
    }

    private void updateTimerText() {
        if (!timeBasedExercise || repCountText == null) {
            return;
        }

        int remaining = Math.max(
                0, targetDurationSeconds - validPostureSeconds
        );

        repCountText.setText(String.format(
                Locale.getDefault(),
                "VALID TIME: %02d:%02d / %02d:%02d",
                validPostureSeconds / 60,
                validPostureSeconds % 60,
                targetDurationSeconds / 60,
                targetDurationSeconds % 60
        ));

        if (feedbackText != null && !postureCurrentlyCorrect) {
            feedbackText.setContentDescription(
                    "Keep correct posture. "
                            + remaining + " seconds remaining."
            );
        }
    }

    private void finishSuccessfulRepSet(int detectedReps) {
        if (resultSent || isFinishing() || isDestroyed()) {
            return;
        }

        resultSent = true;
        timerHandler.removeCallbacks(postureAccuracyRunnable);
        stopPostureTimer();
        timerHandler.removeCallbacks(postureAccuracyRunnable);

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        Toast.makeText(
                this,
                "Target reps reached!",
                Toast.LENGTH_SHORT
        ).show();

        Intent result = new Intent();
        result.putExtra("completed_reps", detectedReps);
        result.putExtra("posture_accuracy", overlayView.getPostureAccuracy());
        result.putExtra("evaluated_frames", overlayView.getTotalEvaluatedFrames());

        setResult(RESULT_OK, result);
        finish();
    }

    private void finishSuccessfulTimedSet() {
        if (resultSent || isFinishing() || isDestroyed()) {
            return;
        }

        resultSent = true;
        timerHandler.removeCallbacks(postureAccuracyRunnable);
        stopPostureTimer();
        timerHandler.removeCallbacks(postureAccuracyRunnable);

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        Toast.makeText(
                this,
                "60 seconds of correct posture completed!",
                Toast.LENGTH_SHORT
        ).show();

        Intent result = new Intent();
        result.putExtra(
                "completed_seconds",
                targetDurationSeconds
        );
        result.putExtra("posture_accuracy", overlayView.getPostureAccuracy());
        result.putExtra("evaluated_frames", overlayView.getTotalEvaluatedFrames());

        setResult(RESULT_OK, result);
        finish();
    }

    private void stopPostureTimer() {
        timerHandler.removeCallbacks(postureTimerRunnable);
    }

    private void initializePoseHelper() {
        if (poseHelper != null) {
            return;
        }

        try {
            poseHelper = new LivePoseDetectionHelper(
                    getApplicationContext(),
                    new LivePoseDetectionHelper.PoseResultListener() {
                        @Override
                        public void onPoseResult(
                                PoseLandmarkerResult result) {

                            if (isFinishing()
                                    || isDestroyed()
                                    || resultSent) {
                                return;
                            }

                            if (result == null
                                    || result.landmarks() == null
                                    || result.landmarks().isEmpty()
                                    || result.landmarks().get(0).isEmpty()) {

                                runOnUiThread(() -> {
                                    if (!isFinishing()
                                            && !isDestroyed()
                                            && !resultSent) {
                                        postureCurrentlyCorrect = false;
                                        overlayView.clearPose();
                                    }
                                });

                                return;
                            }

                            runOnUiThread(() -> {
                                if (!isFinishing()
                                        && !isDestroyed()
                                        && !resultSent) {
                                    overlayView.setPoseLandmarks(
                                            result.landmarks().get(0)
                                    );
                                }
                            });
                        }

                        @Override
                        public void onPoseError(String message) {
                            Log.e(TAG, "Pose detection: " + message);
                        }
                    }
            );

        } catch (Exception | LinkageError e) {
            Log.e(TAG, "Failed to initialize pose detector", e);

            Toast.makeText(
                    this,
                    "Pose detector could not start. Check model/assets.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void startCamera() {
        if (cameraStarting
                || resultSent
                || cameraExecutor == null
                || cameraExecutor.isShutdown()) {
            return;
        }

        cameraStarting = true;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                if (resultSent || isFinishing() || isDestroyed()) {
                    return;
                }

                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .setOutputImageFormat(
                                ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                        )
                        .build();

                analysis.setAnalyzer(
                        cameraExecutor, this::analyzeFrame
                );

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        analysis
                );

            } catch (Exception e) {
                Log.e(TAG, "Unable to start camera", e);

                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(
                            this,
                            "Could not start camera.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            } finally {
                cameraStarting = false;
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        Bitmap bitmap = null;

        try {
            if (poseHelper == null
                    || resultSent
                    || isFinishing()
                    || isDestroyed()) {
                return;
            }

            bitmap = imageProxyToBitmap(imageProxy);

            if (bitmap == null) {
                return;
            }

            int rotation =
                    imageProxy.getImageInfo().getRotationDegrees();

            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);

                Bitmap rotated = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true
                );

                bitmap.recycle();
                bitmap = rotated;
            }

            long timestamp = imageProxy.getImageInfo()
                    .getTimestamp() / 1_000_000L;

            if (timestamp <= lastTimestamp) {
                timestamp = lastTimestamp + 1;
            }

            lastTimestamp = timestamp;

            poseHelper.detect(bitmap, timestamp);
            bitmap = null;

        } catch (Exception | LinkageError e) {
            Log.e(TAG, "Frame analysis error", e);
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }

            imageProxy.close();
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();

        if (planes.length == 0) {
            return null;
        }

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int rowStride = planes[0].getRowStride();
        int pixelStride = planes[0].getPixelStride();

        ByteBuffer buffer = planes[0].getBuffer();
        buffer.rewind();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * rowStride + x * pixelStride;

                if (index + 3 >= bytes.length) {
                    continue;
                }

                int red = bytes[index] & 0xFF;
                int green = bytes[index + 1] & 0xFF;
                int blue = bytes[index + 2] & 0xFF;
                int alpha = bytes[index + 3] & 0xFF;

                pixels[y * width + x] = (alpha << 24)
                        | (red << 16)
                        | (green << 8)
                        | blue;
            }
        }

        return Bitmap.createBitmap(
                pixels,
                width,
                height,
                Bitmap.Config.ARGB_8888
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                initializePoseHelper();
                startCamera();

            } else {
                Toast.makeText(
                        this,
                        "Camera permission is required.",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopPostureTimer();
        timerHandler.removeCallbacks(postureAccuracyRunnable);

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        if (poseHelper != null) {
            poseHelper.close();
            poseHelper = null;
        }

        super.onDestroy();
    }
}