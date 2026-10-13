package com.fittrack.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class OverlayView extends View {

    private Paint jointPaint;
    private Paint bonePaint;
    private Paint textPaint;

    private float animationPhase = 0f;
    private boolean movingDown = true;
    private boolean isCorrectPosture = true;
    private OnPostureChangeListener postureChangeListener;

    public interface OnPostureChangeListener {
        void onPostureChanged(boolean isCorrect, String feedback);
    }

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        jointPaint = new Paint();
        jointPaint.setColor(Color.parseColor("#00E676")); // accent_green
        jointPaint.setStyle(Paint.Style.FILL);
        jointPaint.setAntiAlias(true);

        bonePaint = new Paint();
        bonePaint.setColor(Color.parseColor("#00E676"));
        bonePaint.setStyle(Paint.Style.STROKE);
        bonePaint.setStrokeWidth(8f);
        bonePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dpToPx(14));
        textPaint.setAntiAlias(true);

        // Start a simple animation thread/loop for demo tracking simulation
        postDelayed(this::updatePose, 30);
    }

    public void setOnPostureChangeListener(OnPostureChangeListener listener) {
        this.postureChangeListener = listener;
    }

    private void updatePose() {
        // Animate phase back and forth between 0 and 1 (simulating a squat sequence)
        if (movingDown) {
            animationPhase += 0.01f;
            if (animationPhase >= 1.0f) {
                animationPhase = 1.0f;
                movingDown = false;
            }
        } else {
            animationPhase -= 0.01f;
            if (animationPhase <= 0.0f) {
                animationPhase = 0.0f;
                movingDown = true;
            }
        }

        // Change posture accuracy based on deepness of squat for simulation realism
        boolean oldPosture = isCorrectPosture;
        if (animationPhase > 0.75f) {
            isCorrectPosture = false; // back bent too much at bottom of squat
        } else {
            isCorrectPosture = true;
        }

        if (oldPosture != isCorrectPosture && postureChangeListener != null) {
            if (isCorrectPosture) {
                postureChangeListener.onPostureChanged(true, "Correct Posture");
            } else {
                postureChangeListener.onPostureChanged(false, "Improve Posture: Keep Back Straight!");
            }
        }

        // Update colors based on correctness
        int color = isCorrectPosture ? Color.parseColor("#00E676") : Color.parseColor("#FF5252");
        jointPaint.setColor(color);
        bonePaint.setColor(color);

        invalidate();
        postDelayed(this::updatePose, 30);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Base coordinates for full-body tracking overlay reference points
        float centerX = w / 2f;
        float baseHeadY = h * 0.25f;
        float squatOffset = animationPhase * 120f; // joints moving down during squat

        // Skeleton nodes
        float headX = centerX;
        float headY = baseHeadY + squatOffset * 0.4f;

        float neckX = centerX;
        float neckY = headY + 50;

        float lShoulderX = centerX - 120;
        float rShoulderX = centerX + 120;
        float shouldersY = neckY + 20;

        float lElbowX = lShoulderX - 60;
        float lElbowY = shouldersY + 100 + squatOffset * 0.1f;
        float rElbowX = rShoulderX + 60;
        float rElbowY = shouldersY + 100 + squatOffset * 0.1f;

        float lHipX = centerX - 90;
        float rHipX = centerX + 90;
        float hipsY = shouldersY + 220 + squatOffset;

        float lKneeX = centerX - 140 - (animationPhase * 30); // knees flare slightly out
        float lKneeY = hipsY + 160 - (squatOffset * 0.5f);
        float rKneeX = rHipX + 50 + (animationPhase * 30);
        float rKneeY = hipsY + 160 - (squatOffset * 0.5f);

        float lAnkleX = centerX - 100;
        float lAnkleY = h * 0.85f;
        float rAnkleX = centerX + 100;
        float rAnkleY = h * 0.85f;

        // Draw Bones (Limbs)
        canvas.drawLine(headX, headY, neckX, neckY, bonePaint);
        canvas.drawLine(lShoulderX, shouldersY, rShoulderX, shouldersY, bonePaint);
        canvas.drawLine(neckX, neckY, (lShoulderX + rShoulderX)/2f, shouldersY, bonePaint);
        
        // Arms
        canvas.drawLine(lShoulderX, shouldersY, lElbowX, lElbowY, bonePaint);
        canvas.drawLine(rShoulderX, shouldersY, rElbowX, rElbowY, bonePaint);

        // Torso
        canvas.drawLine(lShoulderX, shouldersY, lHipX, hipsY, bonePaint);
        canvas.drawLine(rShoulderX, shouldersY, rHipX, hipsY, bonePaint);
        canvas.drawLine(lHipX, hipsY, rHipX, hipsY, bonePaint);

        // Legs (Squat Critical Vectors)
        canvas.drawLine(lHipX, hipsY, lKneeX, lKneeY, bonePaint);
        canvas.drawLine(rHipX, hipsY, rKneeX, rKneeY, bonePaint);
        canvas.drawLine(lKneeX, lKneeY, lAnkleX, lAnkleY, bonePaint);
        canvas.drawLine(rKneeX, rKneeY, rAnkleX, rAnkleY, bonePaint);

        // Draw Joints
        float radius = 16f;
        canvas.drawCircle(headX, headY, radius * 1.5f, jointPaint);
        canvas.drawCircle(neckX, neckY, radius, jointPaint);
        canvas.drawCircle(lShoulderX, shouldersY, radius, jointPaint);
        canvas.drawCircle(rShoulderX, shouldersY, radius, jointPaint);
        canvas.drawCircle(lElbowX, lElbowY, radius, jointPaint);
        canvas.drawCircle(rElbowX, rElbowY, radius, jointPaint);
        canvas.drawCircle(lHipX, hipsY, radius, jointPaint);
        canvas.drawCircle(rHipX, hipsY, radius, jointPaint);
        canvas.drawCircle(lKneeX, lKneeY, radius, jointPaint);
        canvas.drawCircle(rKneeX, rKneeY, radius, jointPaint);
        canvas.drawCircle(lAnkleX, lAnkleY, radius, jointPaint);
        canvas.drawCircle(rAnkleX, rAnkleY, radius, jointPaint);

        // Draw Joint Angle/Labels text near knee
        if (!isCorrectPosture) {
            canvas.drawText("Knee angle acute: > 115°", lKneeX - 40, lKneeY - 30, textPaint);
        } else {
            canvas.drawText("Knee angle: 90°", lKneeX - 40, lKneeY - 30, textPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}