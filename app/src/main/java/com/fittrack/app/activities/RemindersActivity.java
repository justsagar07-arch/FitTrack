package com.fittrack.app.activities;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.adapters.WorkoutScheduleAdapter;
import com.fittrack.app.models.WorkoutSchedule;
import com.fittrack.app.services.GoalReminderScheduler;
import com.fittrack.app.services.MealReminderScheduler;
import com.fittrack.app.services.MoveReminderScheduler;
import com.fittrack.app.services.SleepReminderScheduler;
import com.fittrack.app.services.WaterReminderScheduler;
import com.fittrack.app.services.WorkoutReminderScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RemindersActivity extends AppCompatActivity implements WorkoutScheduleAdapter.OnScheduleActionListener {

    private static final String PREFS_NAME = "FitTrackReminders";

    // Water Reminder Views
    private MaterialSwitch switchWaterReminder;
    private TextView tvWaterProgressText;
    private LinearProgressIndicator progressWaterDaily;
    private MaterialButton btnQuickAddWater;
    private TextInputEditText etWaterGoal;
    private TextInputEditText etWaterInterval;
    private MaterialButton btnWaterStartTime;
    private MaterialButton btnWaterEndTime;

    private int waterStartHour = 8;
    private int waterStartMinute = 0;
    private int waterEndHour = 22;
    private int waterEndMinute = 0;

    private int todayWaterConsumed = 0;
    private int dailyWaterGoal = 8;

    // Workout Schedules Views
    private MaterialButton btnAddWorkoutSchedule;
    private RecyclerView rvWorkoutSchedules;
    private TextView tvNoWorkoutSchedules;
    private WorkoutScheduleAdapter workoutScheduleAdapter;
    private final List<WorkoutSchedule> workoutSchedules = new ArrayList<>();

    // Move & Stretch Views
    private MaterialSwitch switchMoveReminder;
    private TextInputEditText etMoveInterval;
    private MaterialButton btnMoveStartTime;
    private MaterialButton btnMoveEndTime;

    private int moveStartHour = 9;
    private int moveStartMinute = 0;
    private int moveEndHour = 18;
    private int moveEndMinute = 0;

    // Sleep Views
    private MaterialSwitch switchSleepReminder;
    private MaterialButton btnSleepBedtime;
    private MaterialSwitch switchWindDownEnabled;
    private TextInputEditText etWindDownLeadMinutes;

    private int sleepBedtimeHour = 22;
    private int sleepBedtimeMinute = 30;

    // Meal Views
    private MaterialButton btnMealBreakfastTime;
    private MaterialSwitch switchMealBreakfast;
    private MaterialButton btnMealLunchTime;
    private MaterialSwitch switchMealLunch;
    private MaterialButton btnMealDinnerTime;
    private MaterialSwitch switchMealDinner;

    private int breakfastHour = 8;
    private int breakfastMinute = 0;
    private int lunchHour = 13;
    private int lunchMinute = 0;
    private int dinnerHour = 19;
    private int dinnerMinute = 30;

    // Goal Views
    private MaterialSwitch switchGoalReminder;
    private MaterialButton btnGoalReminderTime;
    private CheckBox cbRemindSteps;
    private CheckBox cbRemindWorkout;

    private int goalReminderHour = 19;
    private int goalReminderMinute = 0;

    private SharedPreferences prefs;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission required for reminders", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Water Views
        switchWaterReminder = findViewById(R.id.switch_water_reminder);
        tvWaterProgressText = findViewById(R.id.tv_water_progress_text);
        progressWaterDaily = findViewById(R.id.progress_water_daily);
        btnQuickAddWater = findViewById(R.id.btn_quick_add_water);
        etWaterGoal = findViewById(R.id.et_water_goal);
        etWaterInterval = findViewById(R.id.et_water_interval);
        btnWaterStartTime = findViewById(R.id.btn_water_start_time);
        btnWaterEndTime = findViewById(R.id.btn_water_end_time);

        MaterialButton btnSaveWaterSettings = findViewById(R.id.btn_save_water_settings);

        // Workout Views
        btnAddWorkoutSchedule = findViewById(R.id.btn_add_workout_schedule);
        rvWorkoutSchedules = findViewById(R.id.rv_workout_schedules);
        tvNoWorkoutSchedules = findViewById(R.id.tv_no_workout_schedules);

        // Move Views
        switchMoveReminder = findViewById(R.id.switch_move_reminder);
        etMoveInterval = findViewById(R.id.et_move_interval);
        btnMoveStartTime = findViewById(R.id.btn_move_start_time);
        btnMoveEndTime = findViewById(R.id.btn_move_end_time);
        MaterialButton btnSaveMoveSettings = findViewById(R.id.btn_save_move_settings);

        // Sleep Views
        switchSleepReminder = findViewById(R.id.switch_sleep_reminder);
        btnSleepBedtime = findViewById(R.id.btn_sleep_bedtime);
        switchWindDownEnabled = findViewById(R.id.switch_winddown_enabled);
        etWindDownLeadMinutes = findViewById(R.id.et_winddown_lead_minutes);
        MaterialButton btnSaveSleepSettings = findViewById(R.id.btn_save_sleep_settings);

        // Meal Views
        btnMealBreakfastTime = findViewById(R.id.btn_meal_breakfast_time);
        switchMealBreakfast = findViewById(R.id.switch_meal_breakfast);
        btnMealLunchTime = findViewById(R.id.btn_meal_lunch_time);
        switchMealLunch = findViewById(R.id.switch_meal_lunch);
        btnMealDinnerTime = findViewById(R.id.btn_meal_dinner_time);
        switchMealDinner = findViewById(R.id.switch_meal_dinner);
        MaterialButton btnSaveMealSettings = findViewById(R.id.btn_save_meal_settings);

        // Goal Views
        switchGoalReminder = findViewById(R.id.switch_goal_reminder);
        btnGoalReminderTime = findViewById(R.id.btn_goal_reminder_time);
        cbRemindSteps = findViewById(R.id.cb_remind_steps);
        cbRemindWorkout = findViewById(R.id.cb_remind_workout);
        MaterialButton btnSaveGoalSettings = findViewById(R.id.btn_save_goal_settings);

        setupWorkoutRecyclerView();

        loadWaterPreferences();
        loadWorkoutSchedules();
        loadMovePreferences();
        loadSleepPreferences();
        loadMealPreferences();
        loadGoalPreferences();
        checkNotificationPermission();

        btnWaterStartTime.setOnClickListener(v -> showStartTimePicker());
        btnWaterEndTime.setOnClickListener(v -> showEndTimePicker());
        btnQuickAddWater.setOnClickListener(v -> quickLogWater());
        btnSaveWaterSettings.setOnClickListener(v -> saveWaterSettings());

        btnAddWorkoutSchedule.setOnClickListener(v -> showWorkoutScheduleDialog(null));

        btnMoveStartTime.setOnClickListener(v -> showMoveStartTimePicker());
        btnMoveEndTime.setOnClickListener(v -> showMoveEndTimePicker());
        btnSaveMoveSettings.setOnClickListener(v -> saveMoveSettings());

        btnSleepBedtime.setOnClickListener(v -> showBedtimePicker());
        btnSaveSleepSettings.setOnClickListener(v -> saveSleepSettings());

        btnMealBreakfastTime.setOnClickListener(v -> showMealTimePicker("breakfast"));
        btnMealLunchTime.setOnClickListener(v -> showMealTimePicker("lunch"));
        btnMealDinnerTime.setOnClickListener(v -> showMealTimePicker("dinner"));
        btnSaveMealSettings.setOnClickListener(v -> saveMealSettings());

        btnGoalReminderTime.setOnClickListener(v -> showGoalTimePicker());
        btnSaveGoalSettings.setOnClickListener(v -> saveGoalSettings());

        loadTodayWaterLog();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // =========================================================================
    // WATER REMINDER LOGIC
    // =========================================================================

    private void loadWaterPreferences() {
        boolean enabled = prefs.getBoolean("water_enabled", false);
        dailyWaterGoal = prefs.getInt("water_goal", 8);
        int interval = prefs.getInt("water_interval_hours", 2);

        waterStartHour = prefs.getInt("water_start_hour", 8);
        waterStartMinute = prefs.getInt("water_start_minute", 0);
        waterEndHour = prefs.getInt("water_end_hour", 22);
        waterEndMinute = prefs.getInt("water_end_minute", 0);

        switchWaterReminder.setChecked(enabled);
        etWaterGoal.setText(String.valueOf(dailyWaterGoal));
        etWaterInterval.setText(String.valueOf(interval));

        updateTimeButtonText(btnWaterStartTime, "Start", waterStartHour, waterStartMinute);
        updateTimeButtonText(btnWaterEndTime, "End", waterEndHour, waterEndMinute);
        updateWaterProgressUI();
    }

    private void showStartTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            waterStartHour = hourOfDay;
            waterStartMinute = minute;
            updateTimeButtonText(btnWaterStartTime, "Start", waterStartHour, waterStartMinute);
        }, waterStartHour, waterStartMinute, false);
        dialog.show();
    }

    private void showEndTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            waterEndHour = hourOfDay;
            waterEndMinute = minute;
            updateTimeButtonText(btnWaterEndTime, "End", waterEndHour, waterEndMinute);
        }, waterEndHour, waterEndMinute, false);
        dialog.show();
    }

    private void updateTimeButtonText(MaterialButton button, String prefix, int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        button.setText(String.format(Locale.getDefault(), "%s: %02d:%02d %s", prefix, displayHour, minute, amPm));
    }

    private void updateWaterProgressUI() {
        int percent = (int) Math.min(100, ((float) todayWaterConsumed / dailyWaterGoal) * 100);
        tvWaterProgressText.setText(
                String.format(Locale.getDefault(), "Today: %d / %d glasses (%d%%)", todayWaterConsumed, dailyWaterGoal, percent)
        );
        progressWaterDaily.setProgress(percent);
    }

    private void loadTodayWaterLog() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dateKey = DATE_FORMAT.format(new Date());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("waterLogs")
                .document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long consumedObj = doc.getLong("waterConsumed");
                        if (consumedObj != null) {
                            todayWaterConsumed = consumedObj.intValue();
                        }
                    }
                    updateWaterProgressUI();
                });
    }

    private void quickLogWater() {
        todayWaterConsumed++;
        updateWaterProgressUI();

        Toast.makeText(this, "Water logged! 💧", Toast.LENGTH_SHORT).show();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dateKey = DATE_FORMAT.format(new Date());

        Map<String, Object> logData = new HashMap<>();
        logData.put("date", dateKey);
        logData.put("waterConsumed", todayWaterConsumed);
        logData.put("waterGoal", dailyWaterGoal);
        logData.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("waterLogs")
                .document(dateKey)
                .set(logData, SetOptions.merge());
    }

    private void saveWaterSettings() {
        boolean enabled = switchWaterReminder.isChecked();

        String goalStr = etWaterGoal.getText() == null ? "" : etWaterGoal.getText().toString().trim();
        String intervalStr = etWaterInterval.getText() == null ? "" : etWaterInterval.getText().toString().trim();

        if (goalStr.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(this, "Please enter valid goal and interval numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        int goal;
        int interval;

        try {
            goal = Integer.parseInt(goalStr);
            interval = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Goal and interval must be numbers.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (goal <= 0 || interval <= 0) {
            Toast.makeText(this, "Goal and interval must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        dailyWaterGoal = goal;

        prefs.edit()
                .putBoolean("water_enabled", enabled)
                .putInt("water_goal", dailyWaterGoal)
                .putInt("water_interval_hours", interval)
                .putInt("water_start_hour", waterStartHour)
                .putInt("water_start_minute", waterStartMinute)
                .putInt("water_end_hour", waterEndHour)
                .putInt("water_end_minute", waterEndMinute)
                .apply();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> settingsData = new HashMap<>();
            settingsData.put("waterEnabled", enabled);
            settingsData.put("waterGoal", dailyWaterGoal);
            settingsData.put("waterIntervalHours", interval);
            settingsData.put("waterStartHour", waterStartHour);
            settingsData.put("waterStartMinute", waterStartMinute);
            settingsData.put("waterEndHour", waterEndHour);
            settingsData.put("waterEndMinute", waterEndMinute);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("reminderSettings")
                    .document("water")
                    .set(settingsData, SetOptions.merge());
        }

        if (enabled) {
            WaterReminderScheduler.scheduleWaterReminder(
                    this, waterStartHour, waterStartMinute, waterEndHour, waterEndMinute, interval
            );
            Toast.makeText(this, "Water reminder settings saved and enabled!", Toast.LENGTH_SHORT).show();
        } else {
            WaterReminderScheduler.cancelWaterReminder(this);
            Toast.makeText(this, "Water reminder settings saved (disabled).", Toast.LENGTH_SHORT).show();
        }

        updateWaterProgressUI();
    }

    // =========================================================================
    // WORKOUT SCHEDULE REMINDER LOGIC
    // =========================================================================

    private void setupWorkoutRecyclerView() {
        workoutScheduleAdapter = new WorkoutScheduleAdapter(workoutSchedules, this);
        rvWorkoutSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutSchedules.setAdapter(workoutScheduleAdapter);
    }

    private void loadWorkoutSchedules() {
        workoutSchedules.clear();

        int scheduleCount = prefs.getInt("workout_schedule_count", 0);
        for (int i = 0; i < scheduleCount; i++) {
            String id = prefs.getString("ws_id_" + i, "ws_" + i);
            String title = prefs.getString("ws_title_" + i, "Workout Session");
            int hour = prefs.getInt("ws_hour_" + i, 8);
            int minute = prefs.getInt("ws_minute_" + i, 0);
            boolean enabled = prefs.getBoolean("ws_enabled_" + i, true);

            String daysStr = prefs.getString("ws_days_" + i, "2,4,6");
            List<Integer> days = new ArrayList<>();
            for (String s : daysStr.split(",")) {
                try {
                    days.add(Integer.parseInt(s.trim()));
                } catch (Exception ignored) {
                }
            }

            WorkoutSchedule schedule = new WorkoutSchedule(id, title, hour, minute, days, enabled);
            workoutSchedules.add(schedule);
        }

        updateWorkoutSchedulesUI();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("workoutSchedules")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            workoutSchedules.clear();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String id = doc.getId();
                                String title = doc.getString("title");
                                Long hourObj = doc.getLong("hour");
                                Long minObj = doc.getLong("minute");
                                Boolean enabledObj = doc.getBoolean("enabled");
                                List<Long> daysLong = (List<Long>) doc.get("daysOfWeek");

                                List<Integer> days = new ArrayList<>();
                                if (daysLong != null) {
                                    for (Long d : daysLong) {
                                        days.add(d.intValue());
                                    }
                                }

                                WorkoutSchedule schedule = new WorkoutSchedule(
                                        id,
                                        title == null ? "Workout Session" : title,
                                        hourObj == null ? 8 : hourObj.intValue(),
                                        minObj == null ? 0 : minObj.intValue(),
                                        days,
                                        Boolean.TRUE.equals(enabledObj)
                                );
                                workoutSchedules.add(schedule);
                            }
                            saveWorkoutSchedulesLocally();
                            updateWorkoutSchedulesUI();
                        }
                    });
        }
    }

    private void updateWorkoutSchedulesUI() {
        if (workoutSchedules.isEmpty()) {
            rvWorkoutSchedules.setVisibility(View.GONE);
            tvNoWorkoutSchedules.setVisibility(View.VISIBLE);
        } else {
            tvNoWorkoutSchedules.setVisibility(View.GONE);
            rvWorkoutSchedules.setVisibility(View.VISIBLE);
            workoutScheduleAdapter.notifyDataSetChanged();
        }
    }

    private void saveWorkoutSchedulesLocally() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("workout_schedule_count", workoutSchedules.size());

        for (int i = 0; i < workoutSchedules.size(); i++) {
            WorkoutSchedule s = workoutSchedules.get(i);
            editor.putString("ws_id_" + i, s.getId());
            editor.putString("ws_title_" + i, s.getTitle());
            editor.putInt("ws_hour_" + i, s.getHour());
            editor.putInt("ws_minute_" + i, s.getMinute());
            editor.putBoolean("ws_enabled_" + i, s.isEnabled());

            StringBuilder sb = new StringBuilder();
            if (s.getDaysOfWeek() != null) {
                for (int j = 0; j < s.getDaysOfWeek().size(); j++) {
                    sb.append(s.getDaysOfWeek().get(j));
                    if (j < s.getDaysOfWeek().size() - 1) {
                        sb.append(",");
                    }
                }
            }
            editor.putString("ws_days_" + i, sb.toString());
        }
        editor.apply();
    }

    private void syncWorkoutScheduleToFirestore(WorkoutSchedule schedule) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("title", schedule.getTitle());
        map.put("hour", schedule.getHour());
        map.put("minute", schedule.getMinute());
        map.put("daysOfWeek", schedule.getDaysOfWeek());
        map.put("enabled", schedule.isEnabled());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutSchedules")
                .document(schedule.getId())
                .set(map, SetOptions.merge());
    }

    private void deleteWorkoutScheduleFromFirestore(String scheduleId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workoutSchedules")
                .document(scheduleId)
                .delete();
    }

    private void showWorkoutScheduleDialog(WorkoutSchedule scheduleToEdit) {
        boolean isEdit = scheduleToEdit != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Workout Schedule" : "Add Workout Schedule");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(48, 32, 48, 16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Schedule Title (e.g. Leg Day)");
        if (isEdit) {
            etTitle.setText(scheduleToEdit.getTitle());
        } else {
            etTitle.setText("Workout Session");
        }
        dialogLayout.addView(etTitle);

        MaterialButton btnPickTime = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        final int[] selectedTime = new int[]{
                isEdit ? scheduleToEdit.getHour() : 8,
                isEdit ? scheduleToEdit.getMinute() : 0
        };

        Runnable updateTimeBtn = () -> {
            String amPm = selectedTime[0] >= 12 ? "PM" : "AM";
            int h = selectedTime[0] > 12 ? selectedTime[0] - 12 : (selectedTime[0] == 0 ? 12 : selectedTime[0]);
            btnPickTime.setText(String.format(Locale.getDefault(), "Reminder Time: %02d:%02d %s", h, selectedTime[1], amPm));
        };
        updateTimeBtn.run();

        btnPickTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedTime[0] = hourOfDay;
                selectedTime[1] = minute;
                updateTimeBtn.run();
            }, selectedTime[0], selectedTime[1], false);
            dialog.show();
        });
        dialogLayout.addView(btnPickTime);

        TextView tvDaysHeader = new TextView(this);
        tvDaysHeader.setText("Active Days:");
        tvDaysHeader.setTextSize(14);
        tvDaysHeader.setPadding(0, 16, 0, 8);
        dialogLayout.addView(tvDaysHeader);

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        int[] calendarDays = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        CheckBox[] checkBoxes = new CheckBox[7];

        List<Integer> initialDays = isEdit ? scheduleToEdit.getDaysOfWeek() : new ArrayList<>();
        if (!isEdit) {
            initialDays.add(Calendar.MONDAY);
            initialDays.add(Calendar.WEDNESDAY);
            initialDays.add(Calendar.FRIDAY);
        }

        for (int i = 0; i < 7; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(dayNames[i]);
            cb.setChecked(initialDays.contains(calendarDays[i]));
            checkBoxes[i] = cb;
            dialogLayout.addView(cb);
        }

        builder.setView(dialogLayout);

        builder.setPositiveButton(isEdit ? "Save Changes" : "Add Schedule", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                title = "Workout Session";
            }

            List<Integer> selectedDays = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (checkBoxes[i].isChecked()) {
                    selectedDays.add(calendarDays[i]);
                }
            }

            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                scheduleToEdit.setTitle(title);
                scheduleToEdit.setHour(selectedTime[0]);
                scheduleToEdit.setMinute(selectedTime[1]);
                scheduleToEdit.setDaysOfWeek(selectedDays);

                if (scheduleToEdit.isEnabled()) {
                    WorkoutReminderScheduler.scheduleWorkoutReminder(this, scheduleToEdit);
                }
                syncWorkoutScheduleToFirestore(scheduleToEdit);
                Toast.makeText(this, "Workout schedule updated!", Toast.LENGTH_SHORT).show();
            } else {
                String id = UUID.randomUUID().toString();
                WorkoutSchedule newSchedule = new WorkoutSchedule(id, title, selectedTime[0], selectedTime[1], selectedDays, true);
                workoutSchedules.add(newSchedule);

                WorkoutReminderScheduler.scheduleWorkoutReminder(this, newSchedule);
                syncWorkoutScheduleToFirestore(newSchedule);
                Toast.makeText(this, "Workout schedule created and enabled!", Toast.LENGTH_SHORT).show();
            }

            saveWorkoutSchedulesLocally();
            updateWorkoutSchedulesUI();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Adapter Callbacks
    @Override
    public void onToggle(WorkoutSchedule schedule, boolean enabled) {
        schedule.setEnabled(enabled);

        if (enabled) {
            WorkoutReminderScheduler.scheduleWorkoutReminder(this, schedule);
            Toast.makeText(this, schedule.getTitle() + " enabled", Toast.LENGTH_SHORT).show();
        } else {
            WorkoutReminderScheduler.cancelWorkoutReminder(this, schedule);
            Toast.makeText(this, schedule.getTitle() + " disabled", Toast.LENGTH_SHORT).show();
        }

        saveWorkoutSchedulesLocally();
        syncWorkoutScheduleToFirestore(schedule);
    }

    @Override
    public void onEdit(WorkoutSchedule schedule) {
        showWorkoutScheduleDialog(schedule);
    }

    @Override
    public void onDelete(WorkoutSchedule schedule) {
        WorkoutReminderScheduler.cancelWorkoutReminder(this, schedule);
        workoutSchedules.remove(schedule);

        saveWorkoutSchedulesLocally();
        deleteWorkoutScheduleFromFirestore(schedule.getId());
        updateWorkoutSchedulesUI();

        Toast.makeText(this, "Workout schedule deleted", Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // MOVE & STRETCH REMINDER LOGIC
    // =========================================================================

    private void loadMovePreferences() {
        boolean enabled = prefs.getBoolean("move_enabled", false);
        int interval = prefs.getInt("move_interval_minutes", 60);

        moveStartHour = prefs.getInt("move_start_hour", 9);
        moveStartMinute = prefs.getInt("move_start_minute", 0);
        moveEndHour = prefs.getInt("move_end_hour", 18);
        moveEndMinute = prefs.getInt("move_end_minute", 0);

        switchMoveReminder.setChecked(enabled);
        etMoveInterval.setText(String.valueOf(interval));

        updateTimeButtonText(btnMoveStartTime, "Start", moveStartHour, moveStartMinute);
        updateTimeButtonText(btnMoveEndTime, "End", moveEndHour, moveEndMinute);
    }

    private void showMoveStartTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            moveStartHour = hourOfDay;
            moveStartMinute = minute;
            updateTimeButtonText(btnMoveStartTime, "Start", moveStartHour, moveStartMinute);
        }, moveStartHour, moveStartMinute, false);
        dialog.show();
    }

    private void showMoveEndTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            moveEndHour = hourOfDay;
            moveEndMinute = minute;
            updateTimeButtonText(btnMoveEndTime, "End", moveEndHour, moveEndMinute);
        }, moveEndHour, moveEndMinute, false);
        dialog.show();
    }

    private void saveMoveSettings() {
        boolean enabled = switchMoveReminder.isChecked();
        String intervalStr = etMoveInterval.getText() == null ? "" : etMoveInterval.getText().toString().trim();

        if (intervalStr.isEmpty()) {
            Toast.makeText(this, "Please enter a valid interval in minutes.", Toast.LENGTH_SHORT).show();
            return;
        }

        int interval;
        try {
            interval = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Interval must be a valid number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (interval <= 0) {
            Toast.makeText(this, "Interval must be greater than 0 minutes.", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putBoolean("move_enabled", enabled)
                .putInt("move_interval_minutes", interval)
                .putInt("move_start_hour", moveStartHour)
                .putInt("move_start_minute", moveStartMinute)
                .putInt("move_end_hour", moveEndHour)
                .putInt("move_end_minute", moveEndMinute)
                .apply();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> settingsData = new HashMap<>();
            settingsData.put("moveEnabled", enabled);
            settingsData.put("moveIntervalMinutes", interval);
            settingsData.put("moveStartHour", moveStartHour);
            settingsData.put("moveStartMinute", moveStartMinute);
            settingsData.put("moveEndHour", moveEndHour);
            settingsData.put("moveEndMinute", moveEndMinute);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("reminderSettings")
                    .document("move")
                    .set(settingsData, SetOptions.merge());
        }

        if (enabled) {
            MoveReminderScheduler.scheduleMoveReminder(
                    this, moveStartHour, moveStartMinute, moveEndHour, moveEndMinute, interval
            );
            Toast.makeText(this, "Move & Stretch reminder enabled!", Toast.LENGTH_SHORT).show();
        } else {
            MoveReminderScheduler.cancelMoveReminder(this);
            Toast.makeText(this, "Move & Stretch reminder disabled.", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // SLEEP REMINDER LOGIC
    // =========================================================================

    private void loadSleepPreferences() {
        boolean enabled = prefs.getBoolean("sleep_enabled", false);
        sleepBedtimeHour = prefs.getInt("sleep_bedtime_hour", 22);
        sleepBedtimeMinute = prefs.getInt("sleep_bedtime_minute", 30);
        boolean windDownEnabled = prefs.getBoolean("sleep_winddown_enabled", true);
        int leadMinutes = prefs.getInt("sleep_winddown_lead_minutes", 30);

        switchSleepReminder.setChecked(enabled);
        switchWindDownEnabled.setChecked(windDownEnabled);
        etWindDownLeadMinutes.setText(String.valueOf(leadMinutes));

        updateTimeButtonText(btnSleepBedtime, "Bedtime Target", sleepBedtimeHour, sleepBedtimeMinute);
    }

    private void showBedtimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            sleepBedtimeHour = hourOfDay;
            sleepBedtimeMinute = minute;
            updateTimeButtonText(btnSleepBedtime, "Bedtime Target", sleepBedtimeHour, sleepBedtimeMinute);
        }, sleepBedtimeHour, sleepBedtimeMinute, false);
        dialog.show();
    }

    private void saveSleepSettings() {
        boolean enabled = switchSleepReminder.isChecked();
        boolean windDownEnabled = switchWindDownEnabled.isChecked();

        String leadStr = etWindDownLeadMinutes.getText() == null ? "" : etWindDownLeadMinutes.getText().toString().trim();
        int leadMinutes = 30;
        if (!leadStr.isEmpty()) {
            try {
                leadMinutes = Integer.parseInt(leadStr);
            } catch (NumberFormatException ignored) {
            }
        }

        prefs.edit()
                .putBoolean("sleep_enabled", enabled)
                .putInt("sleep_bedtime_hour", sleepBedtimeHour)
                .putInt("sleep_bedtime_minute", sleepBedtimeMinute)
                .putBoolean("sleep_winddown_enabled", windDownEnabled)
                .putInt("sleep_winddown_lead_minutes", leadMinutes)
                .apply();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> settingsData = new HashMap<>();
            settingsData.put("sleepEnabled", enabled);
            settingsData.put("bedtimeHour", sleepBedtimeHour);
            settingsData.put("bedtimeMinute", sleepBedtimeMinute);
            settingsData.put("windDownEnabled", windDownEnabled);
            settingsData.put("windDownLeadMinutes", leadMinutes);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("reminderSettings")
                    .document("sleep")
                    .set(settingsData, SetOptions.merge());
        }

        if (enabled) {
            SleepReminderScheduler.scheduleSleepReminders(
                    this, sleepBedtimeHour, sleepBedtimeMinute, windDownEnabled, leadMinutes
            );
            Toast.makeText(this, "Sleep reminder settings saved and enabled!", Toast.LENGTH_SHORT).show();
        } else {
            SleepReminderScheduler.cancelSleepReminders(this);
            Toast.makeText(this, "Sleep reminder settings saved (disabled).", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // MEAL REMINDERS LOGIC
    // =========================================================================

    private void loadMealPreferences() {
        boolean bEnabled = prefs.getBoolean("meal_breakfast_enabled", false);
        breakfastHour = prefs.getInt("meal_breakfast_hour", 8);
        breakfastMinute = prefs.getInt("meal_breakfast_min", 0);

        boolean lEnabled = prefs.getBoolean("meal_lunch_enabled", false);
        lunchHour = prefs.getInt("meal_lunch_hour", 13);
        lunchMinute = prefs.getInt("meal_lunch_min", 0);

        boolean dEnabled = prefs.getBoolean("meal_dinner_enabled", false);
        dinnerHour = prefs.getInt("meal_dinner_hour", 19);
        dinnerMinute = prefs.getInt("meal_dinner_min", 30);

        switchMealBreakfast.setChecked(bEnabled);
        switchMealLunch.setChecked(lEnabled);
        switchMealDinner.setChecked(dEnabled);

        updateTimeButtonText(btnMealBreakfastTime, "Breakfast", breakfastHour, breakfastMinute);
        updateTimeButtonText(btnMealLunchTime, "Lunch", lunchHour, lunchMinute);
        updateTimeButtonText(btnMealDinnerTime, "Dinner", dinnerHour, dinnerMinute);
    }

    private void showMealTimePicker(String mealType) {
        int initialHour;
        int initialMinute;

        if ("lunch".equalsIgnoreCase(mealType)) {
            initialHour = lunchHour;
            initialMinute = lunchMinute;
        } else if ("dinner".equalsIgnoreCase(mealType)) {
            initialHour = dinnerHour;
            initialMinute = dinnerMinute;
        } else {
            initialHour = breakfastHour;
            initialMinute = breakfastMinute;
        }

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if ("lunch".equalsIgnoreCase(mealType)) {
                lunchHour = hourOfDay;
                lunchMinute = minute;
                updateTimeButtonText(btnMealLunchTime, "Lunch", lunchHour, lunchMinute);
            } else if ("dinner".equalsIgnoreCase(mealType)) {
                dinnerHour = hourOfDay;
                dinnerMinute = minute;
                updateTimeButtonText(btnMealDinnerTime, "Dinner", dinnerHour, dinnerMinute);
            } else {
                breakfastHour = hourOfDay;
                breakfastMinute = minute;
                updateTimeButtonText(btnMealBreakfastTime, "Breakfast", breakfastHour, breakfastMinute);
            }
        }, initialHour, initialMinute, false);
        dialog.show();
    }

    private void saveMealSettings() {
        boolean bEnabled = switchMealBreakfast.isChecked();
        boolean lEnabled = switchMealLunch.isChecked();
        boolean dEnabled = switchMealDinner.isChecked();

        prefs.edit()
                .putBoolean("meal_breakfast_enabled", bEnabled)
                .putInt("meal_breakfast_hour", breakfastHour)
                .putInt("meal_breakfast_min", breakfastMinute)
                .putBoolean("meal_lunch_enabled", lEnabled)
                .putInt("meal_lunch_hour", lunchHour)
                .putInt("meal_lunch_min", lunchMinute)
                .putBoolean("meal_dinner_enabled", dEnabled)
                .putInt("meal_dinner_hour", dinnerHour)
                .putInt("meal_dinner_min", dinnerMinute)
                .apply();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> map = new HashMap<>();
            map.put("breakfastEnabled", bEnabled);
            map.put("breakfastHour", breakfastHour);
            map.put("breakfastMin", breakfastMinute);
            map.put("lunchEnabled", lEnabled);
            map.put("lunchHour", lunchHour);
            map.put("lunchMin", lunchMinute);
            map.put("dinnerEnabled", dEnabled);
            map.put("dinnerHour", dinnerHour);
            map.put("dinnerMin", dinnerMinute);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("reminderSettings")
                    .document("meals")
                    .set(map, SetOptions.merge());
        }

        MealReminderScheduler.scheduleMealReminder(this, "breakfast", breakfastHour, breakfastMinute, bEnabled);
        MealReminderScheduler.scheduleMealReminder(this, "lunch", lunchHour, lunchMinute, lEnabled);
        MealReminderScheduler.scheduleMealReminder(this, "dinner", dinnerHour, dinnerMinute, dEnabled);

        Toast.makeText(this, "Meal reminders saved!", Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // DAILY GOAL REMINDERS LOGIC
    // =========================================================================

    private void loadGoalPreferences() {
        boolean enabled = prefs.getBoolean("goal_reminder_enabled", false);
        goalReminderHour = prefs.getInt("goal_reminder_hour", 19);
        goalReminderMinute = prefs.getInt("goal_reminder_minute", 0);
        boolean remindSteps = prefs.getBoolean("goal_remind_steps", true);
        boolean remindWorkout = prefs.getBoolean("goal_remind_workout", true);

        switchGoalReminder.setChecked(enabled);
        cbRemindSteps.setChecked(remindSteps);
        cbRemindWorkout.setChecked(remindWorkout);

        updateTimeButtonText(btnGoalReminderTime, "Reminder Time", goalReminderHour, goalReminderMinute);
    }

    private void showGoalTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            goalReminderHour = hourOfDay;
            goalReminderMinute = minute;
            updateTimeButtonText(btnGoalReminderTime, "Reminder Time", goalReminderHour, goalReminderMinute);
        }, goalReminderHour, goalReminderMinute, false);
        dialog.show();
    }

    private void saveGoalSettings() {
        boolean enabled = switchGoalReminder.isChecked();
        boolean remindSteps = cbRemindSteps.isChecked();
        boolean remindWorkout = cbRemindWorkout.isChecked();

        prefs.edit()
                .putBoolean("goal_reminder_enabled", enabled)
                .putInt("goal_reminder_hour", goalReminderHour)
                .putInt("goal_reminder_minute", goalReminderMinute)
                .putBoolean("goal_remind_steps", remindSteps)
                .putBoolean("goal_remind_workout", remindWorkout)
                .apply();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> map = new HashMap<>();
            map.put("enabled", enabled);
            map.put("hour", goalReminderHour);
            map.put("minute", goalReminderMinute);
            map.put("remindSteps", remindSteps);
            map.put("remindWorkout", remindWorkout);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("reminderSettings")
                    .document("dailyGoals")
                    .set(map, SetOptions.merge());
        }

        if (enabled) {
            GoalReminderScheduler.scheduleGoalReminder(this, goalReminderHour, goalReminderMinute, true);
            Toast.makeText(this, "Daily goal reminders saved and enabled!", Toast.LENGTH_SHORT).show();
        } else {
            GoalReminderScheduler.cancelGoalReminder(this);
            Toast.makeText(this, "Daily goal reminders saved (disabled).", Toast.LENGTH_SHORT).show();
        }
    }
}
