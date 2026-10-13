package com.fittrack.app.models;

import java.io.Serializable;

public class FoodLogEntry implements Serializable {

    private String id;
    private String foodName;
    private String servingSize;
    private double servings;
    private int calories;
    private double proteinGrams;
    private double carbsGrams;
    private double fatGrams;
    private String mealType; // Breakfast, Lunch, Dinner, Snack
    private long loggedAtMillis;
    private String date; // yyyy-MM-dd
    private String dataSource;

    public FoodLogEntry() {
    }

    public FoodLogEntry(
            String id,
            String foodName,
            String servingSize,
            double servings,
            int calories,
            double proteinGrams,
            double carbsGrams,
            double fatGrams,
            String mealType,
            long loggedAtMillis,
            String date,
            String dataSource
    ) {
        this.id = id;
        this.foodName = foodName;
        this.servingSize = servingSize;
        this.servings = servings;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.mealType = mealType;
        this.loggedAtMillis = loggedAtMillis;
        this.date = date;
        this.dataSource = dataSource;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getServingSize() {
        return servingSize;
    }

    public void setServingSize(String servingSize) {
        this.servingSize = servingSize;
    }

    public double getServings() {
        return servings;
    }

    public void setServings(double servings) {
        this.servings = servings;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(double proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public double getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(double carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public double getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(double fatGrams) {
        this.fatGrams = fatGrams;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public long getLoggedAtMillis() {
        return loggedAtMillis;
    }

    public void setLoggedAtMillis(long loggedAtMillis) {
        this.loggedAtMillis = loggedAtMillis;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
