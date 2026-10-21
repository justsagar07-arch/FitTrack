
package com.fittrack.app.utils;

import android.content.Context;
import android.os.Build;

import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class PoseDetectionHelper {

    public static class ScanResult {
        public final boolean poseDetected;
        public final int landmarkCount;
        public final String message;

        public ScanResult(
                boolean poseDetected,
                int landmarkCount,
                String message
        ) {
            this.poseDetected = poseDetected;
            this.landmarkCount = landmarkCount;
            this.message = message;
        }
    }

    public static ScanResult detectPose(
            Context context,
            File photoFile
    ) {
        // Temporary workaround: MediaPipe's x86_64 native library
        // is missing from the current app APK.
        if (Build.SUPPORTED_ABIS != null) {
            for (String abi : Build.SUPPORTED_ABIS) {
                if ("x86_64".equals(abi)) {
                    return new ScanResult(
                            false,
                            0,
                            "Pose detection is not supported on this emulator. "
                                    + "Photo upload can continue."
                    );
                }
            }
        }

        PoseLandmarker poseLandmarker = null;

        try {
            if (photoFile == null || !photoFile.exists()) {
                return new ScanResult(
                        false,
                        0,
                        "Photo file not found."
                );
            }

            Bitmap bitmap = BitmapFactory.decodeFile(
                    photoFile.getAbsolutePath()
            );

            if (bitmap == null) {
                return new ScanResult(
                        false,
                        0,
                        "Could not read the photo."
                );
            }

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();

            PoseLandmarker.PoseLandmarkerOptions options =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(
                                    com.google.mediapipe.tasks.core.BaseOptions
                                            .builder()
                                            .setModelAssetPath(
                                                    "pose_landmarker_lite.task"
                                            )
                                            .build()
                            )
                            .setRunningMode(RunningMode.IMAGE)
                            .build();

            poseLandmarker = PoseLandmarker.createFromOptions(
                    context,
                    options
            );

            PoseLandmarkerResult result =
                    poseLandmarker.detect(mpImage);

            int landmarkCount = 0;

            if (result != null
                    && result.landmarks() != null
                    && !result.landmarks().isEmpty()) {
                landmarkCount = result.landmarks().get(0).size();
            }

            bitmap.recycle();

            if (landmarkCount > 0) {
                return new ScanResult(
                        true,
                        landmarkCount,
                        "Pose detected successfully."
                );
            }

            return new ScanResult(
                    false,
                    0,
                    "No pose detected in the photo."
            );

        } catch (LinkageError e) {
            return new ScanResult(
                    false,
                    0,
                    "Pose detection native library is unavailable. "
                            + "Photo upload can continue."
            );

        } catch (Exception e) {
            return new ScanResult(
                    false,
                    0,
                    "Pose detection failed: " + e.getMessage()
            );

        } finally {
            if (poseLandmarker != null) {
                try {
                    poseLandmarker.close();
                } catch (Exception ignored) {
                    // Ignore cleanup errors.
                }
            }
        }
    }
}
