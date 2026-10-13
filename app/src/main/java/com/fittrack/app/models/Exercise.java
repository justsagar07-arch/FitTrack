package com.fittrack.app.models;

import java.io.Serializable;

public class Exercise implements Serializable {
    private String name;
    private String category;
    private String description;
    private String difficulty;
    private String durationOrReps;
    private String safetyTips;

    public Exercise(String name, String category, String description, String difficulty, String durationOrReps, String safetyTips) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.difficulty = difficulty;
        this.durationOrReps = durationOrReps;
        this.safetyTips = safetyTips;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getDurationOrReps() {
        return durationOrReps;
    }

    public String getSafetyTips() {
        return safetyTips;
    }
}
