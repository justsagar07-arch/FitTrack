package com.fittrack.app.models;

public class Meal {
    private String name;
    private int calories;
    private String description;
    private int iconResId;
    private boolean isLogged;

    public Meal(String name, int calories, String description, int iconResId) {
        this.name = name;
        this.calories = calories;
        this.description = description;
        this.iconResId = iconResId;
        this.isLogged = false;
    }

    public String getName() { return name; }
    public int getCalories() { return calories; }
    public String getDescription() { return description; }
    public int getIconResId() { return iconResId; }
    public boolean isLogged() { return isLogged; }
    public void setLogged(boolean logged) { isLogged = logged; }
}