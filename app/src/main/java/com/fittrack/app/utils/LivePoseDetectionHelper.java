package com.fittrack.app.utils;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

public class LivePoseDetectionHelper {

    public interface PoseResultListener {
        void onPoseResult(PoseLandmarkerResult result);
        void onPoseError(String message);
    }

    private PoseLandmarker poseLandmarker;
    private final PoseResultListener listener;

    public LivePoseDetectionHelper(
            Context context,
            PoseResultListener listener
    ) {
        this.listener = listener;

        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build();

        PoseLandmarker.PoseLandmarkerOptions options =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setResultListener((result, input) -> {
                            if (this.listener != null) {
                                this.listener.onPoseResult(result);
                            }
                        })
                        .setErrorListener(error -> {
                            if (this.listener != null) {
                                this.listener.onPoseError(
                                        error.getMessage() != null
                                                ? error.getMessage()
                                                : "Pose detection error"
                                );
                            }
                        })
                        .build();

        poseLandmarker =
                PoseLandmarker.createFromOptions(context, options);
    }

    public void detect(Bitmap bitmap, long timestampMs) {
        if (poseLandmarker == null || bitmap == null || bitmap.isRecycled()) {
            return;
        }

        MPImage image = new BitmapImageBuilder(bitmap).build();
        poseLandmarker.detectAsync(image, timestampMs);
    }

    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
    }
}