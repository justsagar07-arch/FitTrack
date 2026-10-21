package com.fittrack.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverlayView extends View {

    private final Paint bonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint jointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Object lock = new Object();

    private List<NormalizedLandmark> landmarks = new ArrayList<>();

    private String exercise = "SQUATS";
    private String feedback = "Waiting for pose...";
    private boolean postureCorrect = true;
    private boolean postureEvaluated = false;
    private int totalEvaluatedFrames = 0;
    private int correctPostureFrames = 0;

    private int reps = 0;
    private String pushUpPhase = "WAITING";
    private String squatPhase = "UP";
    private String armPhase = "EXTENDED";
    private String shoulderPhase = "DOWN";

    private OnPostureChangeListener postureListener;
    private OnRepCountChangeListener repListener;

    private static final int GREEN = Color.rgb(0, 230, 118);
    private static final int RED = Color.rgb(255, 55, 55);
    private static final int WHITE = Color.WHITE;

    private static final int[][] BONES = {
            {11, 12}, {11, 13}, {13, 15}, {12, 14}, {14, 16},
            {11, 23}, {12, 24}, {23, 24},
            {23, 25}, {25, 27}, {24, 26}, {26, 28},
            {27, 29}, {29, 31}, {28, 30}, {30, 32}
    };

    private static final int[] JOINTS = {
            11, 12, 13, 14, 15, 16,
            23, 24, 25, 26, 27, 28
    };

    public interface OnPostureChangeListener {
        void onPostureChanged(boolean correct, String message);
    }

    public interface OnRepCountChangeListener {
        void onRepCountChanged(int count);
    }

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs,
                       int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bonePaint.setStyle(Paint.Style.STROKE);
        bonePaint.setStrokeWidth(dp(3));
        bonePaint.setStrokeCap(Paint.Cap.ROUND);

        jointPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setShadowLayer(dp(3), 0, 0, Color.BLACK);
    }

    public void setOnPostureChangeListener(
            OnPostureChangeListener listener) {
        postureListener = listener;
    }

    public void setOnRepCountChangeListener(
            OnRepCountChangeListener listener) {
        repListener = listener;
    }

    public int getRepCount() {
        synchronized (lock) {
            return reps;
        }
    }


    public int getPostureAccuracy() {
        synchronized (lock) {
            if (totalEvaluatedFrames == 0) {
                return 0;
            }
            return Math.round(
                    (correctPostureFrames * 100.0f) / totalEvaluatedFrames
            );
        }
    }

    public int getTotalEvaluatedFrames() {
        synchronized (lock) {
            return totalEvaluatedFrames;
        }
    }

    public void resetPostureAccuracy() {
        synchronized (lock) {
            totalEvaluatedFrames = 0;
            correctPostureFrames = 0;
        }
    }

    public void setExercise(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        synchronized (lock) {
            exercise = name.trim().toUpperCase(Locale.ROOT);
            reps = 0;
            pushUpPhase = "WAITING";
            squatPhase = "UP";
            armPhase = "EXTENDED";
            shoulderPhase = "DOWN";
            postureEvaluated = false;
            postureCorrect = true;
            feedback = "Waiting for pose...";
        }

        postInvalidate();

        if (repListener != null) {
            repListener.onRepCountChanged(0);
        }
    }

    public void setPoseLandmarks(List<NormalizedLandmark> points) {
        if (points == null || points.size() < 33) {
            clearPose();
            return;
        }

        synchronized (lock) {
            landmarks = new ArrayList<>(points);
        }

        if (exercise.contains("PUSH")) {
            evaluatePushUp();
        } else if (exercise.contains("SQUAT")) {
            evaluateSquat();
        } else if (exercise.contains("HANGING KNEE RAISE")
                || exercise.contains("KNEE RAISE")) {
            evaluateKneeRaise();
        } else if (exercise.contains("BURPEE")) {
            evaluateBurpee();
        } else if (exercise.contains("KETTLEBELL SWING")) {
            evaluateKettlebellSwing();
        } else if (exercise.contains("JUMP ROPE")
                || exercise.contains("HIIT")) {
            evaluateTimedMovement();
        } else if (exercise.contains("PLANK")) {
            evaluatePlank();
        } else if (isChestExercise()) {
            evaluateChestExercise();
        } else if (isBackExercise()) {
            evaluateBackExercise();
        } else if (isShoulderExercise()) {
            evaluateShoulderExercise();
        } else if (isArmExercise()) {
            evaluateArmExercise();
        } else {
            updateFeedback(
                    false,
                    "Posture rule not configured for this exercise yet"
            );
        }

        postInvalidate();
    }

    public void clearPose() {
        synchronized (lock) {
            landmarks.clear();
            feedback = "Pose not detected - show your full body";
            postureEvaluated = true;
            postureCorrect = false;
        }

        if (postureListener != null) {
            postureListener.onPostureChanged(
                    false,
                    "Pose not detected - show your full body"
            );
        }

        postInvalidate();
    }

    private void evaluatePlank() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(hip) || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Show shoulder, hip and ankle from the side"
            );
            return;
        }

        double bodyAngle = angle(shoulder, hip, ankle);
        boolean aligned = bodyAngle >= 155 && bodyAngle <= 205;

        updateFeedback(
                aligned,
                aligned
                        ? "Plank alignment looks good; keep breathing"
                        : "Keep shoulders, hips and ankles in one line"
        );
    }

    private void evaluateTimedMovement() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark knee = p.get(25);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(hip)
                || !visible(knee) || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Keep your body visible to the camera"
            );
            return;
        }

        updateFeedback(
                true,
                "Body detected. Follow the selected movement safely"
        );
    }

    private void evaluatePushUp() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark elbow = p.get(13);
        NormalizedLandmark wrist = p.get(15);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(elbow)
                || !visible(wrist) || !visible(hip)
                || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Show your full body from the side"
            );
            return;
        }

        double elbowAngle = angle(shoulder, elbow, wrist);
        double bodyAngle = angle(shoulder, hip, ankle);

        boolean aligned = bodyAngle >= 155 && bodyAngle <= 205;

        updateFeedback(
                aligned,
                aligned
                        ? "Good body alignment"
                        : "Keep shoulders, hips and ankles aligned"
        );

        boolean completed = false;

        synchronized (lock) {
            if (elbowAngle > 150) {
                if ("DOWN".equals(pushUpPhase)) {
                    reps++;
                    completed = true;
                }
                pushUpPhase = "UP";
            } else if (elbowAngle < 95) {
                pushUpPhase = "DOWN";
            }
        }

        notifyRep(completed);
    }

    private void evaluateSquat() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark lh = p.get(23);
        NormalizedLandmark lk = p.get(25);
        NormalizedLandmark la = p.get(27);

        NormalizedLandmark rh = p.get(24);
        NormalizedLandmark rk = p.get(26);
        NormalizedLandmark ra = p.get(28);

        boolean left = visible(lh) && visible(lk) && visible(la);
        boolean right = visible(rh) && visible(rk) && visible(ra);

        if (!left && !right) {
            updateFeedback(false, "Show your legs to the camera");
            return;
        }

        double kneeAngle;

        if (left && right) {
            kneeAngle = (
                    angle(lh, lk, la) + angle(rh, rk, ra)
            ) / 2.0;
        } else if (left) {
            kneeAngle = angle(lh, lk, la);
        } else {
            kneeAngle = angle(rh, rk, ra);
        }

        boolean completed = false;

        synchronized (lock) {
            if (kneeAngle < 105) {
                squatPhase = "DOWN";
            } else if (kneeAngle > 155) {
                if ("DOWN".equals(squatPhase)) {
                    reps++;
                    completed = true;
                }
                squatPhase = "UP";
            }
        }

        if (kneeAngle < 55) {
            updateFeedback(
                    false,
                    "Check your squat depth and balance"
            );
        } else if (kneeAngle < 140) {
            updateFeedback(
                    true,
                    "Squat detected - keep your knees aligned"
            );
        } else {
            updateFeedback(
                    true,
                    "Stand tall and prepare for the next squat"
            );
        }

        notifyRep(completed);
    }

    private boolean isChestExercise() {
        return exercise.contains("BENCH PRESS")
                || exercise.contains("CHEST PRESS")
                || exercise.contains("CHEST FLY")
                || exercise.contains("DUMBBELL FLY")
                || exercise.contains("PEC DECK")
                || exercise.contains("INCLINE DUMBBELL FLY")
                || exercise.contains("MACHINE CHEST PRESS")
                || exercise.contains("CABLE CHEST FLY");
    }

    private void evaluateChestExercise() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark ls = p.get(11);
        NormalizedLandmark le = p.get(13);
        NormalizedLandmark lw = p.get(15);

        NormalizedLandmark rs = p.get(12);
        NormalizedLandmark re = p.get(14);
        NormalizedLandmark rw = p.get(16);

        boolean left = visible(ls) && visible(le) && visible(lw);
        boolean right = visible(rs) && visible(re) && visible(rw);

        if (!left && !right) {
            updateFeedback(
                    false,
                    "Keep shoulders, elbows and wrists visible"
            );
            return;
        }

        double leftAngle = left ? angle(ls, le, lw) : -1;
        double rightAngle = right ? angle(rs, re, rw) : -1;
        double elbowAngle = averageAvailable(leftAngle, rightAngle);

        boolean completed = false;

        synchronized (lock) {
            if (elbowAngle < 95) {
                armPhase = "FLEXED";
            } else if (elbowAngle > 150) {
                if ("FLEXED".equals(armPhase)) {
                    reps++;
                    completed = true;
                }
                armPhase = "EXTENDED";
            }
        }

        if (elbowAngle < 95) {
            updateFeedback(
                    true,
                    "Lower under control; keep wrists aligned"
            );
        } else if (elbowAngle > 150) {
            updateFeedback(
                    true,
                    "Arms extended; avoid locking elbows forcefully"
            );
        } else {
            updateFeedback(
                    true,
                    "Move slowly and keep shoulders stable"
            );
        }

        notifyRep(completed);
    }

    private boolean isBackExercise() {
        return exercise.contains("DEADLIFT")
                || exercise.contains("LAT PULLDOWN")
                || exercise.contains("PULL-DOWN")
                || exercise.contains("BENT-OVER BARBELL ROW")
                || exercise.contains("BENT OVER BARBELL ROW")
                || exercise.contains("SEATED CABLE ROW")
                || exercise.contains("ASSISTED PULL-UP")
                || exercise.contains("ASSISTED PULL UP")
                || exercise.contains("STRAIGHT-ARM CABLE PULLDOWN")
                || exercise.contains("STRAIGHT ARM CABLE PULLDOWN")
                || exercise.contains("ROMANIAN DEADLIFT");
    }

    private void evaluateBackExercise() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark ls = p.get(11);
        NormalizedLandmark le = p.get(13);
        NormalizedLandmark lw = p.get(15);
        NormalizedLandmark lh = p.get(23);

        NormalizedLandmark rs = p.get(12);
        NormalizedLandmark re = p.get(14);
        NormalizedLandmark rw = p.get(16);
        NormalizedLandmark rh = p.get(24);

        boolean left = visible(ls) && visible(le)
                && visible(lw) && visible(lh);
        boolean right = visible(rs) && visible(re)
                && visible(rw) && visible(rh);

        if (!left && !right) {
            updateFeedback(
                    false,
                    "Keep shoulders, elbows, wrists and hips visible"
            );
            return;
        }

        double leftElbow = left ? angle(ls, le, lw) : -1;
        double rightElbow = right ? angle(rs, re, rw) : -1;
        double elbowAngle = averageAvailable(leftElbow, rightElbow);

        double leftTorso = left ? angle(ls, lh, p.get(25)) : -1;
        double rightTorso = right ? angle(rs, rh, p.get(26)) : -1;
        double torsoAngle = averageAvailable(leftTorso, rightTorso);

        boolean deadlift = exercise.contains("DEADLIFT");
        boolean row = exercise.contains("ROW");
        boolean latPulldown = exercise.contains("PULLDOWN")
                || exercise.contains("PULL-DOWN")
                || exercise.contains("PULL-UP")
                || exercise.contains("PULL UP");

        boolean completed = false;

        if (deadlift) {
            if (left && right) {
                torsoAngle = (
                        angle(ls, lh, p.get(25))
                                + angle(rs, rh, p.get(26))
                ) / 2.0;
            }

            synchronized (lock) {
                if (torsoAngle < 125) {
                    armPhase = "FLEXED";
                } else if (torsoAngle > 155) {
                    if ("FLEXED".equals(armPhase)) {
                        reps++;
                        completed = true;
                    }
                    armPhase = "EXTENDED";
                }
            }

            updateFeedback(
                    torsoAngle >= 70,
                    torsoAngle < 70
                            ? "Keep your back controlled; avoid rounding"
                            : "Hinge at the hips and stand up with control"
            );

        } else if (row || latPulldown) {
            synchronized (lock) {
                if (elbowAngle < 100) {
                    armPhase = "FLEXED";
                } else if (elbowAngle > 145) {
                    if ("FLEXED".equals(armPhase)) {
                        reps++;
                        completed = true;
                    }
                    armPhase = "EXTENDED";
                }
            }

            updateFeedback(
                    true,
                    row
                            ? "Pull toward your torso without swinging"
                            : "Pull with control and keep shoulders stable"
            );

        } else {
            synchronized (lock) {
                if (elbowAngle < 100) {
                    armPhase = "FLEXED";
                } else if (elbowAngle > 145) {
                    if ("FLEXED".equals(armPhase)) {
                        reps++;
                        completed = true;
                    }
                    armPhase = "EXTENDED";
                }
            }

            updateFeedback(
                    true,
                    "Keep your torso stable and move with control"
            );
        }

        notifyRep(completed);
    }

    private boolean isArmExercise() {
        return exercise.contains("CURL")
                || exercise.contains("TRICEP")
                || exercise.contains("TRICEPS")
                || exercise.contains("KICKBACK")
                || exercise.contains("OVERHEAD EXTENSION");
    }

    private void evaluateArmExercise() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark ls = p.get(11);
        NormalizedLandmark le = p.get(13);
        NormalizedLandmark lw = p.get(15);

        NormalizedLandmark rs = p.get(12);
        NormalizedLandmark re = p.get(14);
        NormalizedLandmark rw = p.get(16);

        boolean left = visible(ls) && visible(le) && visible(lw);
        boolean right = visible(rs) && visible(re) && visible(rw);

        if (!left && !right) {
            updateFeedback(
                    false,
                    "Keep shoulders, elbows and wrists visible"
            );
            return;
        }

        double leftAngle = left ? angle(ls, le, lw) : -1;
        double rightAngle = right ? angle(rs, re, rw) : -1;
        double elbowAngle = averageAvailable(leftAngle, rightAngle);

        boolean completed = false;

        synchronized (lock) {
            if (elbowAngle < 80) {
                armPhase = "FLEXED";
            } else if (elbowAngle > 145) {
                if ("FLEXED".equals(armPhase)) {
                    reps++;
                    completed = true;
                }
                armPhase = "EXTENDED";
            }
        }

        updateFeedback(
                true,
                elbowAngle < 80
                        ? "Bend your elbows with control"
                        : elbowAngle > 145
                          ? "Extend your arms slowly"
                          : "Continue a controlled arm movement"
        );

        notifyRep(completed);
    }

    private boolean isShoulderExercise() {
        return exercise.contains("SHOULDER PRESS")
                || exercise.contains("OVERHEAD BARBELL PRESS")
                || exercise.contains("FRONT RAISE")
                || exercise.contains("REAR DELT FLY")
                || exercise.contains("LATERAL RAISE");
    }

    private void evaluateShoulderExercise() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark ls = p.get(11);
        NormalizedLandmark le = p.get(13);
        NormalizedLandmark lw = p.get(15);
        NormalizedLandmark lh = p.get(23);

        NormalizedLandmark rs = p.get(12);
        NormalizedLandmark re = p.get(14);
        NormalizedLandmark rw = p.get(16);
        NormalizedLandmark rh = p.get(24);

        boolean left = visible(ls) && visible(le)
                && visible(lw) && visible(lh);
        boolean right = visible(rs) && visible(re)
                && visible(rw) && visible(rh);

        if (!left && !right) {
            updateFeedback(
                    false,
                    "Keep shoulders, elbows, wrists and hips visible"
            );
            return;
        }

        boolean press = exercise.contains("SHOULDER PRESS")
                || exercise.contains("OVERHEAD BARBELL PRESS");
        boolean rearFly = exercise.contains("REAR DELT FLY");

        double leftAngle;
        double rightAngle;

        if (press) {
            leftAngle = left ? angle(ls, le, lw) : -1;
            rightAngle = right ? angle(rs, re, rw) : -1;
        } else if (rearFly) {
            leftAngle = left ? angle(lh, ls, le) : -1;
            rightAngle = right ? angle(rh, rs, re) : -1;
        } else {
            leftAngle = left ? angle(lh, ls, lw) : -1;
            rightAngle = right ? angle(rh, rs, rw) : -1;
        }

        double movementAngle = averageAvailable(leftAngle, rightAngle);
        boolean completed = false;

        synchronized (lock) {
            if (press) {
                if (movementAngle < 100) {
                    shoulderPhase = "DOWN";
                } else if (movementAngle > 150) {
                    if ("DOWN".equals(shoulderPhase)) {
                        reps++;
                        completed = true;
                    }
                    shoulderPhase = "UP";
                }
            } else {
                if (movementAngle > 55) {
                    shoulderPhase = "UP";
                } else if (movementAngle < 30) {
                    if ("UP".equals(shoulderPhase)) {
                        reps++;
                        completed = true;
                    }
                    shoulderPhase = "DOWN";
                }
            }
        }

        updateFeedback(
                true,
                press
                        ? "Press with control; avoid excessive back arching"
                        : rearFly
                          ? "Keep your back stable and move with control"
                          : "Raise and lower your arms without swinging"
        );

        notifyRep(completed);
    }

    private void evaluateKneeRaise() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark knee = p.get(25);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(hip)
                || !visible(knee) || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Keep shoulder, hip, knee and ankle visible"
            );
            return;
        }

        double hipAngle = angle(shoulder, hip, knee);
        boolean raised = hipAngle < 125;
        boolean completed = false;

        synchronized (lock) {
            if (raised) {
                pushUpPhase = "DOWN";
            } else if (hipAngle > 150 && "DOWN".equals(pushUpPhase)) {
                reps++;
                completed = true;
                pushUpPhase = "WAITING";
            }
        }

        updateFeedback(
                true,
                raised
                        ? "Raise knees with control; avoid swinging"
                        : "Lower legs slowly and keep your torso steady"
        );

        notifyRep(completed);
    }

    private void evaluateBurpee() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark knee = p.get(25);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(hip)
                || !visible(knee) || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Keep your upper body and legs visible"
            );
            return;
        }

        double kneeAngle = angle(hip, knee, ankle);
        boolean crouched = kneeAngle < 115;
        boolean completed = false;

        synchronized (lock) {
            if (crouched) {
                squatPhase = "DOWN";
            } else if (kneeAngle > 155 && "DOWN".equals(squatPhase)) {
                reps++;
                completed = true;
                squatPhase = "UP";
            }
        }

        updateFeedback(
                true,
                crouched
                        ? "Move into the squat phase with control"
                        : "Stand tall to finish; this is a basic burpee check"
        );

        notifyRep(completed);
    }

    private void evaluateKettlebellSwing() {
        List<NormalizedLandmark> p = getLandmarks();

        NormalizedLandmark shoulder = p.get(11);
        NormalizedLandmark hip = p.get(23);
        NormalizedLandmark knee = p.get(25);
        NormalizedLandmark ankle = p.get(27);

        if (!visible(shoulder) || !visible(hip)
                || !visible(knee) || !visible(ankle)) {
            updateFeedback(
                    false,
                    "Show shoulder, hip, knee and ankle from the side"
            );
            return;
        }

        double torsoAngle = angle(shoulder, hip, knee);
        boolean hinged = torsoAngle < 135;
        boolean completed = false;

        synchronized (lock) {
            if (hinged) {
                armPhase = "FLEXED";
            } else if (torsoAngle > 155 && "FLEXED".equals(armPhase)) {
                reps++;
                completed = true;
                armPhase = "EXTENDED";
            }
        }

        updateFeedback(
                true,
                hinged
                        ? "Hinge at the hips; keep your back controlled"
                        : "Drive through the hips and stand tall without leaning back"
        );

        notifyRep(completed);
    }

    private List<NormalizedLandmark> getLandmarks() {
        synchronized (lock) {
            return new ArrayList<>(landmarks);
        }
    }

    private double averageAvailable(double left, double right) {
        if (left >= 0 && right >= 0) {
            return (left + right) / 2.0;
        }

        return left >= 0 ? left : right;
    }

    private void notifyRep(boolean completed) {
        if (completed && repListener != null) {
            int count;

            synchronized (lock) {
                count = reps;
            }

            repListener.onRepCountChanged(count);
        }
    }

    private double angle(NormalizedLandmark a,
                         NormalizedLandmark b,
                         NormalizedLandmark c) {
        double abX = a.x() - b.x();
        double abY = a.y() - b.y();
        double cbX = c.x() - b.x();
        double cbY = c.y() - b.y();

        double dot = abX * cbX + abY * cbY;
        double magAB = Math.sqrt(abX * abX + abY * abY);
        double magCB = Math.sqrt(cbX * cbX + cbY * cbY);

        if (magAB < 0.0001 || magCB < 0.0001) {
            return 180;
        }

        double cos = dot / (magAB * magCB);
        cos = Math.max(-1.0, Math.min(1.0, cos));

        return Math.toDegrees(Math.acos(cos));
    }

    private void updateFeedback(boolean correct, String message) {
        boolean changed;

        synchronized (lock) {
            changed = postureCorrect != correct
                    || !feedback.equals(message);

            postureCorrect = correct;
            feedback = message;
            postureEvaluated = true;

            totalEvaluatedFrames++;
            if (correct) {
                correctPostureFrames++;
            }
        }

        if (changed && postureListener != null) {
            postureListener.onPostureChanged(correct, message);
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        List<NormalizedLandmark> p = getLandmarks();
        boolean correct;
        boolean evaluated;
        String currentFeedback;

        synchronized (lock) {
            correct = postureCorrect;
            evaluated = postureEvaluated;
            currentFeedback = feedback;
        }

        if (p.size() < 33) {
            canvas.drawText(
                    "Waiting for pose...",
                    dp(16),
                    dp(28),
                    textPaint
            );
            return;
        }

        float w = getWidth();
        float h = getHeight();

        for (int[] bone : BONES) {
            NormalizedLandmark a = p.get(bone[0]);
            NormalizedLandmark b = p.get(bone[1]);

            if (!visible(a) || !visible(b)) {
                continue;
            }

            boolean red = evaluated && !correct
                    && isAffectedBone(bone[0], bone[1]);

            bonePaint.setColor(red ? RED : GREEN);

            canvas.drawLine(
                    a.x() * w,
                    a.y() * h,
                    b.x() * w,
                    b.y() * h,
                    bonePaint
            );
        }

        for (int index : JOINTS) {
            NormalizedLandmark point = p.get(index);

            if (!visible(point)) {
                continue;
            }

            boolean red = evaluated && !correct && isAffectedJoint(index);
            jointPaint.setColor(red ? RED : GREEN);

            canvas.drawCircle(
                    point.x() * w,
                    point.y() * h,
                    dp(5),
                    jointPaint
            );
        }

        textPaint.setColor(WHITE);

        canvas.drawText(
                "Reps: " + getRepCount(),
                dp(16),
                dp(28),
                textPaint
        );

        if (currentFeedback != null && !currentFeedback.isEmpty()) {
            canvas.drawText(
                    currentFeedback,
                    dp(16),
                    dp(52),
                    textPaint
            );
        }
    }

    private boolean isAffectedBone(int a, int b) {
        if (exercise.contains("SQUAT")
                || exercise.contains("KNEE RAISE")
                || exercise.contains("BURPEE")
                || exercise.contains("KETTLEBELL SWING")
                || exercise.contains("DEADLIFT")) {
            return (a == 23 && b == 25)
                    || (a == 25 && b == 27)
                    || (a == 24 && b == 26)
                    || (a == 26 && b == 28)
                    || (a == 11 && b == 23)
                    || (a == 12 && b == 24);
        }

        if (exercise.contains("PUSH")) {
            return (a == 11 && b == 23)
                    || (a == 23 && b == 24)
                    || (a == 12 && b == 24);
        }

        if (isArmExercise() || isShoulderExercise()
                || isChestExercise() || isBackExercise()) {
            return (a == 11 && b == 13)
                    || (a == 13 && b == 15)
                    || (a == 12 && b == 14)
                    || (a == 14 && b == 16)
                    || (a == 11 && b == 23)
                    || (a == 12 && b == 24);
        }

        return false;
    }

    private boolean isAffectedJoint(int index) {
        if (exercise.contains("SQUAT")
                || exercise.contains("KNEE RAISE")
                || exercise.contains("BURPEE")
                || exercise.contains("KETTLEBELL SWING")
                || exercise.contains("DEADLIFT")) {
            return index == 23 || index == 24
                    || index == 25 || index == 26
                    || index == 27 || index == 28;
        }

        if (exercise.contains("PUSH")) {
            return index == 11 || index == 12
                    || index == 23 || index == 24;
        }

        if (isArmExercise() || isShoulderExercise()
                || isChestExercise() || isBackExercise()) {
            return index == 11 || index == 12
                    || index == 13 || index == 14
                    || index == 15 || index == 16;
        }

        return false;
    }

    private boolean visible(NormalizedLandmark p) {
        return p != null
                && Float.isFinite(p.x())
                && Float.isFinite(p.y())
                && p.x() >= 0 && p.x() <= 1
                && p.y() >= 0 && p.y() <= 1;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
