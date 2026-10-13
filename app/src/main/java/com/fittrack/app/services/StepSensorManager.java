package com.fittrack.app.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepSensorManager
        implements SensorEventListener {

    private static final String TAG =
            "FitTrackStepSensor";

    private final SensorManager sensorManager;
    private final Sensor stepCounterSensor;

    private StepListener listener;
    private boolean registered = false;

    public StepSensorManager(Context context) {

        sensorManager =
                (SensorManager) context.getSystemService(
                        Context.SENSOR_SERVICE
                );

        stepCounterSensor =
                sensorManager.getDefaultSensor(
                        Sensor.TYPE_STEP_COUNTER
                );

        if (stepCounterSensor != null) {

            Log.d(
                    TAG,
                    "Step sensor found: "
                            + stepCounterSensor.getName()
                            + " | type="
                            + stepCounterSensor.getType()
            );

        } else {

            Log.e(
                    TAG,
                    "Step sensor NOT found"
            );
        }
    }

    // ---------------------------------------------------------
    // CHECK SENSOR
    // ---------------------------------------------------------

    public boolean isStepSensorAvailable() {

        return stepCounterSensor != null;
    }

    // ---------------------------------------------------------
    // START SENSOR
    // ---------------------------------------------------------

    public void start(
            StepListener listener
    ) {

        this.listener = listener;

        if (stepCounterSensor == null) {

            Log.e(
                    TAG,
                    "Cannot start: step sensor unavailable"
            );

            if (listener != null) {
                listener.onSensorUnavailable();
            }

            return;
        }

        if (registered) {

            Log.d(
                    TAG,
                    "Sensor already registered"
            );

            return;
        }

        try {

            registered =
                    sensorManager.registerListener(
                            this,
                            stepCounterSensor,
                            SensorManager.SENSOR_DELAY_NORMAL
                    );

            Log.d(
                    TAG,
                    "Sensor registered = "
                            + registered
            );

            if (!registered) {

                Log.e(
                        TAG,
                        "Sensor registration FAILED"
                );

                if (listener != null) {
                    listener.onSensorRegistrationFailed();
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Exception while registering sensor",
                    e
            );

            if (listener != null) {
                listener.onSensorRegistrationFailed();
            }
        }
    }

    // ---------------------------------------------------------
    // STOP SENSOR
    // ---------------------------------------------------------

    public void stop() {

        if (sensorManager != null) {

            sensorManager.unregisterListener(
                    this
            );
        }

        registered = false;
        listener = null;

        Log.d(
                TAG,
                "Step sensor stopped"
        );
    }

    // ---------------------------------------------------------
    // SENSOR DATA
    // ---------------------------------------------------------

    @Override
    public void onSensorChanged(
            SensorEvent event
    ) {

        if (event == null ||
                event.sensor == null) {
            return;
        }

        if (event.sensor.getType()
                != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        float rawValue =
                event.values[0];

        long totalSteps =
                (long) rawValue;

        Log.d(
                TAG,
                "RAW sensor value = "
                        + rawValue
        );

        Log.d(
                TAG,
                "Total steps = "
                        + totalSteps
        );

        if (listener != null) {

            listener.onStepsChanged(
                    totalSteps
            );
        }
    }

    // ---------------------------------------------------------
    // ACCURACY
    // ---------------------------------------------------------

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy
    ) {

        Log.d(
                TAG,
                "Sensor accuracy = "
                        + accuracy
        );
    }

    // ---------------------------------------------------------
    // CALLBACK
    // ---------------------------------------------------------

    public interface StepListener {

        void onStepsChanged(
                long steps
        );

        void onSensorUnavailable();

        void onSensorRegistrationFailed();
    }
}