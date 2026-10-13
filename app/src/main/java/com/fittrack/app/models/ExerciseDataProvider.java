package com.fittrack.app.models;

import java.util.ArrayList;
import java.util.List;

public class ExerciseDataProvider {

    public static List<Exercise> getAllExercises() {
        List<Exercise> exercises = new ArrayList<>();

        // Chest
        exercises.add(new Exercise("Barbell Bench Press", "Chest", 
            "A classic compound exercise that builds upper-body strength and muscle mass in the pectoral muscles.", 
            "Intermediate", "3-4 sets of 8-12 reps", 
            "Ensure your back is slightly arched and feet are firmly planted on the floor. Do not bounce the bar off your chest. Use a spotter if heavy."));
        exercises.add(new Exercise("Incline Dumbbell Fly", "Chest", 
            "An isolation movement targeting the upper pectoral muscles, promoting stretching and muscle definition.", 
            "Intermediate", "3 sets of 12-15 reps", 
            "Keep a slight bend in your elbows throughout the movement. Avoid over-stretching at the bottom to prevent shoulder strain."));
        exercises.add(new Exercise("Push-Ups", "Chest", 
            "A foundational bodyweight movement working the chest, shoulders, triceps, and core stability.", 
            "Beginner", "3 sets of 15-20 reps", 
            "Maintain a straight line from head to heels. Keep elbows at roughly a 45-degree angle to protect shoulder joints."));

        // Back
        exercises.add(new Exercise("Deadlift", "Back", 
            "A premier full-body compound movement that primarily targets the entire posterior chain including hamstrings, glutes, and lower back.", 
            "Advanced", "3-4 sets of 5-8 reps", 
            "Keep the bar close to your shins. Maintain a neutral spine; do not round your back under load. Engage your core fully."));
        exercises.add(new Exercise("Lat Pulldown", "Back", 
            "An excellent machine exercise focusing on the latissimus dorsi to build back width and upper body pull strength.", 
            "Beginner", "3 sets of 10-12 reps", 
            "Pull the bar down toward your upper chest, squeezing your shoulder blades. Avoid using momentum or leaning excessively back."));
        exercises.add(new Exercise("Bent-Over Barbell Row", "Back", 
            "A powerful horizontal pulling movement that develops depth and density throughout the middle and upper back.", 
            "Intermediate", "3 sets of 8-10 reps", 
            "Keep your torso almost parallel to the floor with knees slightly bent. Pull the bar to your lower rib cage, keeping elbows tucked."));

        // Shoulders
        exercises.add(new Exercise("Overhead Barbell Press", "Shoulders", 
            "A fundamental compound shoulder movement that builds mass and overhead vertical pushing power.", 
            "Intermediate", "3 sets of 8-10 reps", 
            "Squeeze your glutes and core to stabilize your spine. Press the bar straight up overhead, clear of your face."));
        exercises.add(new Exercise("Dumbbell Lateral Raise", "Shoulders", 
            "An isolation exercise designed to target the lateral head of the deltoids for shoulder width.", 
            "Beginner", "3 sets of 12-15 reps", 
            "Lead the movement with your elbows and keep your pinky fingers slightly higher at the peak. Do not swing."));

        // Arms
        exercises.add(new Exercise("Barbell Bicep Curl", "Arms", 
            "The quintessential isolation movement to build strength and mass in the biceps brachii.", 
            "Beginner", "3 sets of 10-12 reps", 
            "Keep elbows pinned to your sides. Avoid rocking your body or swinging the weight up. Control the lowering phase."));
        exercises.add(new Exercise("Tricep Overhead Extension", "Arms", 
            "An overhead isolation movement focusing on the long head of the triceps for full arm development.", 
            "Beginner", "3 sets of 12 reps", 
            "Keep your upper arms stationary and close to your ears. Extend fully at the top and lower under control."));

        // Abs
        exercises.add(new Exercise("Hanging Knee Raise", "Abs", 
            "An effective core movement that challenges abdominal and hip flexor strength while suspended from a bar.", 
            "Intermediate", "3 sets of 12-15 reps", 
            "Minimize swinging by engaging your core. Pull your knees all the way up to your chest and lower them slowly."));
        exercises.add(new Exercise("Plank", "Abs", 
            "An isometric core exercise that builds endurance and deep stabilization across the midsection.", 
            "Beginner", "3 sets of 30-60 seconds", 
            "Keep elbows directly under shoulders. Maintain a perfectly straight body alignment. Do not let hips sag or raise."));

        // Legs
        exercises.add(new Exercise("Barbell Back Squat", "Legs", 
            "The king of leg exercises, targeting quads, glutes, hamstrings, and lower back strength.", 
            "Intermediate", "4 sets of 8-10 reps", 
            "Keep knees tracking over toes. Descend until thighs are parallel to the floor or lower. Keep your chest up throughout."));
        exercises.add(new Exercise("Romanian Deadlift", "Legs", 
            "A variation focusing primarily on hamstrings and gluteus maximus through a hip-hinge mechanism.", 
            "Intermediate", "3 sets of 10-12 reps", 
            "Hinge at your hips while keeping a slight, fixed bend in your knees. Lower the weight until you feel a deep stretch in hamstrings."));

        // Cardio
        exercises.add(new Exercise("High-Intensity Interval Training (HIIT)", "Cardio", 
            "Alternating short periods of intense anaerobic exercise with less intense recovery periods.", 
            "Intermediate", "20-30 minutes", 
            "Warm up properly. Push yourself to maximum effort during intervals. Stay hydrated and monitor your heart rate."));
        exercises.add(new Exercise("Jump Rope", "Cardio", 
            "A high-tempo cardiovascular challenge that improves coordination, agility, and calf endurance.", 
            "Beginner", "5 rounds of 2 minutes", 
            "Land softly on the balls of your feet. Keep your elbows close to your torso and rotate the rope using your wrists."));

        // Full Body
        exercises.add(new Exercise("Burpees", "Full Body", 
            "A full body calisthenic exercise combining a squat, push-up, and explosive jump for ultimate conditioning.", 
            "Intermediate", "4 sets of 10-15 reps", 
            "Maintain form even when fatigued. land softly with bent knees during the jump phase to absorb impact."));
        exercises.add(new Exercise("Kettlebell Swing", "Full Body", 
            "An explosive full-body movement powered by a hip-hinge to build power, stamina, and core strength.", 
            "Intermediate", "3 sets of 15-20 reps", 
            "The power comes from your hips, not your arms. Snap your hips forward powerfully to drive the kettlebell upward."));

        return exercises;
    }

    public static List<Exercise> getExercisesByCategory(String category) {
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise exercise : getAllExercises()) {
            if (exercise.getCategory().equalsIgnoreCase(category)) {
                filtered.add(exercise);
            }
        }
        return filtered;
    }

    public static List<Exercise> searchExercises(String query) {
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise exercise : getAllExercises()) {
            if (exercise.getName().toLowerCase().contains(query.toLowerCase()) ||
                exercise.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                exercise.getDescription().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(exercise);
            }
        }
        return filtered;
    }
}
