package com.fittrack.app.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.health.connect.client.PermissionController;

import com.fittrack.app.R;
import com.fittrack.app.services.HealthConnectManager;
import com.fittrack.app.services.HealthDataManager;
import com.fittrack.app.services.StepTrackingService;
import com.fittrack.app.utils.UnitUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class HealthSettingsActivity extends AppCompatActivity {

    private static final String PREFS_SYNC = "FitTrackHealthSync";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_LAST_SYNC_RESULT = "last_sync_result";

    private static final int REQUEST_CODE_ACTIVITY_PERM = 2001;

    private TextView tvHealthConnectStatus;
    private TextView tvHealthConnectPermissionsSummary;
    private TextView tvLastSyncTime;
    private TextView tvLastSyncResult;
    private View layoutSyncLoading;

    private TextView tvActivityPermissionStatus;
    private TextView tvStepsPermissionStatus;
    private TextView tvHeartRatePermissionStatus;

    private TextView tvSensorStatus;
    private TextView tvServiceStatus;

    private MaterialButton btnSyncHealthData;
    private MaterialButton btnRequestHealthPermissions;
    private MaterialButton btnManagePermissions;
    private MaterialButton btnToggleService;

    private RadioGroup rgWeightUnit;
    private RadioButton rbWeightKg;
    private RadioButton rbWeightLbs;

    private RadioGroup rgHeightUnit;
    private RadioButton rbHeightCm;
    private RadioButton rbHeightFt;

    private SharedPreferences syncPrefs;
    private SharedPreferences unitPrefs;

    private HealthConnectManager healthConnectManager;
    private HealthDataManager healthDataManager;

    private ActivityResultLauncher<Set<String>> healthPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_settings);

        syncPrefs = getSharedPreferences(PREFS_SYNC, MODE_PRIVATE);
        unitPrefs = getSharedPreferences(UnitUtils.PREFS_UNITS, MODE_PRIVATE);

        healthConnectManager = new HealthConnectManager(this);
        healthDataManager = new HealthDataManager(this);

        // Health Connect Permission Launcher
        healthPermissionLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                grantedPermissions -> {
                    Toast.makeText(this, "Health Connect permissions updated", Toast.LENGTH_SHORT).show();
                    checkPermissionsStatus();
                    checkHealthConnectStatus();
                }
        );

        MaterialToolbar toolbar = findViewById(R.id.toolbar_health_settings);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        initializeViews();

        checkHealthConnectStatus();
        updateLastSyncUI();
        checkPermissionsStatus();
        checkHardwareSensorsStatus();
        setupUnitPreferences();

        setupListeners();
    }

    private void initializeViews() {
        tvHealthConnectStatus = findViewById(R.id.tv_health_connect_status);
        tvHealthConnectPermissionsSummary = findViewById(R.id.tv_health_connect_permissions_summary);
        tvLastSyncTime = findViewById(R.id.tv_last_sync_time);
        tvLastSyncResult = findViewById(R.id.tv_last_sync_result);
        layoutSyncLoading = findViewById(R.id.layout_sync_loading);

        tvActivityPermissionStatus = findViewById(R.id.tv_activity_permission_status);
        tvStepsPermissionStatus = findViewById(R.id.tv_steps_permission_status);
        tvHeartRatePermissionStatus = findViewById(R.id.tv_heart_rate_permission_status);

        tvSensorStatus = findViewById(R.id.tv_sensor_status);
        tvServiceStatus = findViewById(R.id.tv_service_status);

        btnSyncHealthData = findViewById(R.id.btn_sync_health_data);
        btnRequestHealthPermissions = findViewById(R.id.btn_request_health_permissions);
        btnManagePermissions = findViewById(R.id.btn_manage_permissions);
        btnToggleService = findViewById(R.id.btn_toggle_service);

        rgWeightUnit = findViewById(R.id.rg_weight_unit);
        rbWeightKg = findViewById(R.id.rb_weight_kg);
        rbWeightLbs = findViewById(R.id.rb_weight_lbs);

        rgHeightUnit = findViewById(R.id.rg_height_unit);
        rbHeightCm = findViewById(R.id.rb_height_cm);
        rbHeightFt = findViewById(R.id.rb_height_ft);
    }

    private void setupListeners() {
        if (btnSyncHealthData != null) {
            btnSyncHealthData.setOnClickListener(v -> performHealthSync());
        }

        if (btnRequestHealthPermissions != null) {
            btnRequestHealthPermissions.setOnClickListener(v -> requestHealthPermissions());
        }

        if (btnManagePermissions != null) {
            btnManagePermissions.setOnClickListener(v -> openAppSettings());
        }

        if (btnToggleService != null) {
            btnToggleService.setOnClickListener(v -> toggleStepService());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsStatus();
        checkHealthConnectStatus();
        checkHardwareSensorsStatus();
        updateLastSyncUI();
    }

    private void checkHealthConnectStatus() {
        boolean available = healthConnectManager.isAvailable();
        if (tvHealthConnectStatus != null) {
            if (available) {
                tvHealthConnectStatus.setText("Health Connect SDK: Available");
            } else {
                tvHealthConnectStatus.setText("Health Connect SDK: Unavailable on device");
            }
        }

        if (available) {
            healthConnectManager.getGrantedPermissions(new HealthConnectManager.GrantedPermissionsSetCallback() {
                @Override
                public void onResult(Set<String> grantedPermissions) {
                    runOnUiThread(() -> {
                        if (tvHealthConnectPermissionsSummary != null) {
                            Set<String> required = healthConnectManager.getRequiredPermissions();
                            if (grantedPermissions != null && grantedPermissions.containsAll(required)) {
                                tvHealthConnectPermissionsSummary.setText("Health Connect Access: Fully Granted (Steps & Heart Rate)");
                            } else if (grantedPermissions != null && !grantedPermissions.isEmpty()) {
                                tvHealthConnectPermissionsSummary.setText("Health Connect Access: Partially Granted");
                            } else {
                                tvHealthConnectPermissionsSummary.setText("Health Connect Access: Not Granted");
                            }
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    runOnUiThread(() -> {
                        if (tvHealthConnectPermissionsSummary != null) {
                            tvHealthConnectPermissionsSummary.setText("Health Connect Access: Status Check Failed");
                        }
                    });
                }
            });
        } else {
            if (tvHealthConnectPermissionsSummary != null) {
                tvHealthConnectPermissionsSummary.setText("Health Connect Access: Local Step Sensor Fallback Active");
            }
        }
    }

    private void updateLastSyncUI() {
        long lastTime = syncPrefs.getLong(KEY_LAST_SYNC_TIME, 0);
        String lastResult = syncPrefs.getString(KEY_LAST_SYNC_RESULT, "No sync performed yet");

        if (lastTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(lastTime));
            if (tvLastSyncTime != null) {
                tvLastSyncTime.setText("Last Successful Sync: " + formattedDate);
            }
        } else {
            if (tvLastSyncTime != null) {
                tvLastSyncTime.setText("Last Successful Sync: Never");
            }
        }

        if (tvLastSyncResult != null) {
            tvLastSyncResult.setText("Result: " + lastResult);
        }
    }

    private void performHealthSync() {
        if (layoutSyncLoading != null) {
            layoutSyncLoading.setVisibility(View.VISIBLE);
        }
        if (btnSyncHealthData != null) {
            btnSyncHealthData.setEnabled(false);
        }

        Toast.makeText(this, "Syncing health data...", Toast.LENGTH_SHORT).show();

        healthDataManager.syncHealthData(new HealthDataManager.HealthSyncCallback() {
            @Override
            public void onSuccess(int steps, int calories, int heartRate, String summary) {
                runOnUiThread(() -> {
                    long now = System.currentTimeMillis();

                    syncPrefs.edit()
                            .putLong(KEY_LAST_SYNC_TIME, now)
                            .putString(KEY_LAST_SYNC_RESULT, summary)
                            .apply();

                    if (layoutSyncLoading != null) {
                        layoutSyncLoading.setVisibility(View.GONE);
                    }
                    if (btnSyncHealthData != null) {
                        btnSyncHealthData.setEnabled(true);
                    }

                    updateLastSyncUI();
                    Toast.makeText(HealthSettingsActivity.this, "Health data synced successfully!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    syncPrefs.edit()
                            .putString(KEY_LAST_SYNC_RESULT, "Last Attempt Failed: " + errorMessage)
                            .apply();

                    if (layoutSyncLoading != null) {
                        layoutSyncLoading.setVisibility(View.GONE);
                    }
                    if (btnSyncHealthData != null) {
                        btnSyncHealthData.setEnabled(true);
                    }

                    updateLastSyncUI();
                    Toast.makeText(HealthSettingsActivity.this, "Sync failed: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void checkPermissionsStatus() {
        // Physical Activity Permission
        if (Build.VERSION.SDK_INT >= 29) {
            int activityPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION);
            if (activityPermission == PackageManager.PERMISSION_GRANTED) {
                if (tvActivityPermissionStatus != null) {
                    tvActivityPermissionStatus.setText("Physical Activity Sensor: Granted");
                }
            } else {
                if (tvActivityPermissionStatus != null) {
                    tvActivityPermissionStatus.setText("Physical Activity Sensor: Denied");
                }
            }
        } else {
            if (tvActivityPermissionStatus != null) {
                tvActivityPermissionStatus.setText("Physical Activity Sensor: Granted (Pre-Android 10)");
            }
        }

        // Health Connect Permissions
        if (healthConnectManager.isAvailable()) {
            healthConnectManager.getGrantedPermissions(new HealthConnectManager.GrantedPermissionsSetCallback() {
                @Override
                public void onResult(Set<String> grantedPermissions) {
                    runOnUiThread(() -> {
                        boolean stepsGranted = grantedPermissions != null && grantedPermissions.contains("android.permission.health.READ_STEPS");
                        boolean hrGranted = grantedPermissions != null && grantedPermissions.contains("android.permission.health.READ_HEART_RATE");

                        if (tvStepsPermissionStatus != null) {
                            tvStepsPermissionStatus.setText("Health Connect Steps: " + (stepsGranted ? "Granted" : "Denied / Not Granted"));
                        }
                        if (tvHeartRatePermissionStatus != null) {
                            tvHeartRatePermissionStatus.setText("Health Connect Heart Rate: " + (hrGranted ? "Granted" : "Denied / Not Granted"));
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    runOnUiThread(() -> {
                        if (tvStepsPermissionStatus != null) {
                            tvStepsPermissionStatus.setText("Health Connect Steps: Error checking permission");
                        }
                        if (tvHeartRatePermissionStatus != null) {
                            tvHeartRatePermissionStatus.setText("Health Connect Heart Rate: Error checking permission");
                        }
                    });
                }
            });
        } else {
            if (tvStepsPermissionStatus != null) {
                tvStepsPermissionStatus.setText("Health Connect Steps: Unavailable on Device");
            }
            if (tvHeartRatePermissionStatus != null) {
                tvHeartRatePermissionStatus.setText("Health Connect Heart Rate: Unavailable on Device");
            }
        }
    }

    private void requestHealthPermissions() {
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE_ACTIVITY_PERM);
        }

        if (healthConnectManager.isAvailable()) {
            Set<String> permissions = healthConnectManager.getRequiredPermissions();
            if (permissions != null && !permissions.isEmpty()) {
                healthPermissionLauncher.launch(permissions);
            } else {
                Toast.makeText(this, "No Health Connect permissions configured", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Health Connect is unavailable on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open system settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkHardwareSensorsStatus() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            Sensor stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

            if (stepCounter != null) {
                if (tvSensorStatus != null) {
                    tvSensorStatus.setText("Hardware Step Sensor: Available (" + stepCounter.getName() + ")");
                }
            } else if (stepDetector != null) {
                if (tvSensorStatus != null) {
                    tvSensorStatus.setText("Hardware Step Detector: Available (" + stepDetector.getName() + ")");
                }
            } else {
                if (tvSensorStatus != null) {
                    tvSensorStatus.setText("Hardware Step Sensor: Not Supported on Device");
                }
            }
        } else {
            if (tvSensorStatus != null) {
                tvSensorStatus.setText("Hardware Step Sensor: Sensor Service Unavailable");
            }
        }

        boolean serviceRunning = isStepTrackingServiceRunning();
        if (tvServiceStatus != null) {
            if (serviceRunning) {
                tvServiceStatus.setText("Step Tracking Service: Active (Foreground Service Running)");
            } else {
                tvServiceStatus.setText("Step Tracking Service: Stopped / Inactive");
            }
        }

        if (btnToggleService != null) {
            btnToggleService.setText(serviceRunning ? "Stop Step Service" : "Start Step Service");
        }
    }

    private boolean isStepTrackingServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (StepTrackingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void toggleStepService() {
        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        if (isStepTrackingServiceRunning()) {
            stopService(serviceIntent);
            Toast.makeText(this, "Step tracking service stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_CODE_ACTIVITY_PERM);
                return;
            }
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, "Step tracking service started", Toast.LENGTH_SHORT).show();
        }
        checkHardwareSensorsStatus();
    }

    private void setupUnitPreferences() {
        String savedWeightUnit = UnitUtils.getWeightUnit(this);
        String savedHeightUnit = UnitUtils.getHeightUnit(this);

        if (UnitUtils.UNIT_LBS.equalsIgnoreCase(savedWeightUnit)) {
            if (rbWeightLbs != null) rbWeightLbs.setChecked(true);
        } else {
            if (rbWeightKg != null) rbWeightKg.setChecked(true);
        }

        if (UnitUtils.UNIT_FT.equalsIgnoreCase(savedHeightUnit)) {
            if (rbHeightFt != null) rbHeightFt.setChecked(true);
        } else {
            if (rbHeightCm != null) rbHeightCm.setChecked(true);
        }

        if (rgWeightUnit != null) {
            rgWeightUnit.setOnCheckedChangeListener((group, checkedId) -> {
                String unit = (checkedId == R.id.rb_weight_lbs) ? UnitUtils.UNIT_LBS : UnitUtils.UNIT_KG;
                unitPrefs.edit().putString(UnitUtils.KEY_WEIGHT_UNIT, unit).apply();
                Toast.makeText(this, "Weight unit preference set to " + unit, Toast.LENGTH_SHORT).show();
            });
        }

        if (rgHeightUnit != null) {
            rgHeightUnit.setOnCheckedChangeListener((group, checkedId) -> {
                String unit = (checkedId == R.id.rb_height_ft) ? UnitUtils.UNIT_FT : UnitUtils.UNIT_CM;
                unitPrefs.edit().putString(UnitUtils.KEY_HEIGHT_UNIT, unit).apply();
                Toast.makeText(this, "Height unit preference set to " + unit, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ACTIVITY_PERM) {
            checkPermissionsStatus();
            checkHardwareSensorsStatus();
        }
    }
}
