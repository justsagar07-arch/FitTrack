package com.fittrack.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WorkoutSchedule implements Serializable {

    private String id;
    private String title;
    private int hour;
    private int minute;
    private List<Integer> daysOfWeek; // Calendar.SUNDAY(1) .. Calendar.SATURDAY(7)
    private boolean enabled;

    public WorkoutSchedule() {
        this.daysOfWeek = new ArrayList<>();
        this.enabled = true;
    }

    public WorkoutSchedule(String id, String title, int hour, int minute, List<Integer> daysOfWeek, boolean enabled) {
        this.id = id;
        this.title = title;
        this.hour = hour;
        this.minute = minute;
        this.daysOfWeek = daysOfWeek == null ? new ArrayList<>() : daysOfWeek;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFormattedTime() {
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm);
    }

    public String getFormattedDays() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return "No days selected";
        }
        if (daysOfWeek.size() == 7) {
            return "Every day";
        }

        StringBuilder sb = new StringBuilder();
        String[] shortNames = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 0; i < daysOfWeek.size(); i++) {
            int day = daysOfWeek.get(i);
            if (day >= 1 && day <= 7) {
                sb.append(shortNames[day]);
                if (i < daysOfWeek.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    public int getStableRequestCode() {
        if (id == null) {
            return 3000;
        }
        return 3000 + Math.abs(id.hashCode() % 5000);
    }
}
