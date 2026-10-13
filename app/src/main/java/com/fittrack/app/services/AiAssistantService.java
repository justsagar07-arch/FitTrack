package com.fittrack.app.services;

import android.os.Handler;
import android.os.Looper;

public class AiAssistantService {

    public interface AiResponseCallback {
        void onResponse(String response);
    }

    public void getResponse(String userInput, AiResponseCallback callback) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String response = generateResponse(userInput.toLowerCase());
            callback.onResponse(response);
        }, 1500);
    }

    private String generateResponse(String input) {
        if (input.contains("hello") || input.contains("hi")) {
            return "Hello! I'm your FitTrack AI assistant. How can I help you reach your fitness goals today?";
        } else if (input.contains("workout") || input.contains("exercise")) {
            return "Looking for a workout? I recommend a mix of strength training and cardio. Would you like a specific routine for today?";
        } else if (input.contains("diet") || input.contains("eat") || input.contains("nutrition")) {
            return "Nutrition is key! Focus on whole foods, lean proteins, and plenty of vegetables. Remember to stay hydrated!";
        } else if (input.contains("calories")) {
            return "Tracking calories helps you stay on top of your energy balance. You can log your meals in the Diet section of FitTrack!";
        } else {
            return "That's interesting! Keep pushing towards your fitness goals. Is there anything specific about your training or diet you'd like to discuss?";
        }
    }
}
