
package com.fittrack.app.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseDataProvider {

    public static List<Exercise> getAllExercises() {
        List<Exercise> exercises = new ArrayList<>();

        // ==================== CHEST ====================

        exercises.add(new Exercise(
                "Barbell Bench Press", "Chest",
                "A compound exercise that builds chest strength and upper-body muscle.",
                "Intermediate", "3-4 sets of 8-12 reps",
                "Keep your feet planted and back stable. Avoid bouncing the bar off your chest. Use a spotter when needed."
        ));

        exercises.add(new Exercise(
                "Incline Dumbbell Fly", "Chest",
                "An isolation exercise targeting the upper chest muscles.",
                "Intermediate", "3 sets of 12-15 reps",
                "Keep a slight bend in your elbows and avoid overstretching at the bottom."
        ));

        exercises.add(new Exercise(
                "Push-Ups", "Chest",
                "A bodyweight exercise working the chest, shoulders, triceps, and core.",
                "Beginner", "3 sets of 15-20 reps",
                "Keep your body aligned from head to heels. Lower yourself with control."
        ));

        exercises.add(new Exercise(
                "Incline Barbell Bench Press", "Chest",
                "A compound pressing exercise that emphasizes the upper chest muscles.",
                "Intermediate", "3 sets of 8-12 reps",
                "Use a controlled range of motion, keep your feet planted, and use a spotter when needed."
        ));

        exercises.add(new Exercise(
                "Machine Chest Press", "Chest",
                "A machine-based pressing exercise that trains the chest, triceps, and front shoulders.",
                "Beginner", "3 sets of 10-12 reps",
                "Adjust the seat so the handles are around mid-chest level. Keep your back supported."
        ));

        exercises.add(new Exercise(
                "Cable Chest Fly", "Chest",
                "A chest isolation exercise that brings the arms together against cable resistance.",
                "Intermediate", "3 sets of 10-15 reps",
                "Keep a slight bend in your elbows and use a comfortable range of motion."
        ));

        // ==================== BACK ====================

        exercises.add(new Exercise(
                "Deadlift", "Back",
                "A compound movement that trains the posterior chain, including the back, glutes, and hamstrings.",
                "Advanced", "3-4 sets of 5-8 reps",
                "Keep the weight close to your body and maintain a neutral spine."
        ));

        exercises.add(new Exercise(
                "Lat Pulldown", "Back",
                "A machine exercise targeting the latissimus dorsi and upper back.",
                "Beginner", "3 sets of 10-12 reps",
                "Pull toward your upper chest and avoid excessive swinging."
        ));

        exercises.add(new Exercise(
                "Bent-Over Barbell Row", "Back",
                "A compound pulling movement for the middle and upper back.",
                "Intermediate", "3 sets of 8-10 reps",
                "Keep your back neutral, knees slightly bent, and movement controlled."
        ));

        exercises.add(new Exercise(
                "Seated Cable Row", "Back",
                "A pulling exercise that targets the middle back muscles.",
                "Beginner", "3 sets of 10-12 reps",
                "Sit upright, keep your back neutral, and pull the handle toward your torso without swinging."
        ));

        exercises.add(new Exercise(
                "Assisted Pull-Up", "Back",
                "An assisted vertical pulling exercise that trains the back and arms.",
                "Beginner", "3 sets of 6-10 reps",
                "Use appropriate assistance, keep your body controlled, and avoid swinging."
        ));

        exercises.add(new Exercise(
                "Straight-Arm Cable Pulldown", "Back",
                "A cable exercise that emphasizes the lats with relatively little elbow movement.",
                "Intermediate", "3 sets of 10-15 reps",
                "Keep a slight bend in your elbows, brace your torso, and use controlled movements."
        ));

        // ==================== SHOULDERS ====================

        exercises.add(new Exercise(
                "Overhead Barbell Press", "Shoulders",
                "A compound movement that develops shoulder strength.",
                "Intermediate", "3 sets of 8-10 reps",
                "Keep your core engaged and avoid excessive arching of your back."
        ));

        exercises.add(new Exercise(
                "Dumbbell Lateral Raise", "Shoulders",
                "An isolation exercise targeting the side deltoids.",
                "Beginner", "3 sets of 12-15 reps",
                "Use controlled movements and avoid swinging the weights."
        ));

        exercises.add(new Exercise(
                "Dumbbell Shoulder Press", "Shoulders",
                "A pressing exercise that develops shoulder strength and upper-body control.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your back stable and press the dumbbells with controlled movement."
        ));

        exercises.add(new Exercise(
                "Dumbbell Front Raise", "Shoulders",
                "An isolation exercise targeting the front deltoids.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your torso steady and raise and lower the dumbbells under control."
        ));

        exercises.add(new Exercise(
                "Dumbbell Rear Delt Fly", "Shoulders",
                "An exercise targeting the rear deltoids and upper back.",
                "Beginner", "3 sets of 10-12 reps",
                "Hinge forward with a neutral back and avoid swinging."
        ));

        // ==================== ARMS ====================

        exercises.add(new Exercise(
                "Barbell Bicep Curl", "Arms",
                "An isolation exercise targeting the biceps.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your elbows near your sides and avoid swinging your body."
        ));

        exercises.add(new Exercise(
                "Tricep Overhead Extension", "Arms",
                "An overhead exercise targeting the triceps.",
                "Beginner", "3 sets of 12 reps",
                "Keep your upper arms steady and lower the weight under control."
        ));

        exercises.add(new Exercise(
                "Dumbbell Bicep Curl", "Arms",
                "An arm exercise targeting the biceps using dumbbells.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your elbows close to your sides and avoid swinging."
        ));

        exercises.add(new Exercise(
                "Hammer Curl", "Arms",
                "A biceps and forearm exercise performed with a neutral grip.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your palms facing inward and lower the weights with control."
        ));

        exercises.add(new Exercise(
                "Concentration Curl", "Arms",
                "A focused biceps exercise performed one arm at a time.",
                "Beginner", "3 sets of 10-12 reps per arm",
                "Keep your upper arm stable and avoid using momentum."
        ));

        exercises.add(new Exercise(
                "Tricep Kickback", "Arms",
                "An isolation exercise targeting the triceps through elbow extension.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your back neutral, upper arm steady, and movement controlled."
        ));

        // ==================== ABS ====================

        exercises.add(new Exercise(
                "Hanging Knee Raise", "Abs",
                "A core exercise targeting the abdominal muscles and hip flexors.",
                "Intermediate", "3 sets of 12-15 reps",
                "Avoid swinging and raise and lower your knees with control."
        ));

        exercises.add(new Exercise(
                "Plank", "Abs",
                "An isometric exercise that builds core stability and endurance.",
                "Beginner", "3 sets of 30-60 seconds",
                "Keep your body aligned and avoid letting your hips sag."
        ));

        exercises.add(new Exercise(
                "Crunches", "Abs",
                "A bodyweight exercise that targets the abdominal muscles.",
                "Beginner", "3 sets of 12-15 reps",
                "Lift your shoulders gently, avoid pulling your neck, and move with control."
        ));

        exercises.add(new Exercise(
                "Bicycle Crunches", "Abs",
                "A core exercise combining trunk rotation and alternating knee movements.",
                "Beginner", "3 sets of 12 reps per side",
                "Move slowly, rotate your torso, and avoid pulling your head forward."
        ));

        exercises.add(new Exercise(
                "Reverse Crunches", "Abs",
                "An abdominal exercise that emphasizes controlled pelvic movement.",
                "Beginner", "3 sets of 10-15 reps",
                "Lift your hips gently and avoid swinging your legs."
        ));

        exercises.add(new Exercise(
                "Leg Raises", "Abs",
                "A core exercise that challenges the abdominal muscles while raising the legs.",
                "Intermediate", "3 sets of 10-12 reps",
                "Keep your lower back comfortable and lower your legs slowly."
        ));

        // ==================== LEGS ====================

        exercises.add(new Exercise(
                "Barbell Back Squat", "Legs",
                "A compound exercise targeting the quadriceps, glutes, and other lower-body muscles.",
                "Intermediate", "3 sets of 15 reps",
                "Keep your feet stable, knees tracking with your toes, and movement controlled."
        ));

        exercises.add(new Exercise(
                "Romanian Deadlift", "Legs",
                "A hip-hinge exercise targeting the hamstrings and glutes.",
                "Intermediate", "3 sets of 10-12 reps",
                "Maintain a neutral back and a slight bend in your knees."
        ));

        exercises.add(new Exercise(
                "Walking Lunges", "Legs",
                "A unilateral lower-body exercise targeting the quadriceps and glutes.",
                "Beginner", "3 sets of 10 reps per leg",
                "Take controlled steps, keep your torso upright, and maintain balance."
        ));

        exercises.add(new Exercise(
                "Leg Press", "Legs",
                "A machine-based exercise that trains the quadriceps and glutes.",
                "Beginner", "3 sets of 10-12 reps",
                "Keep your back supported and avoid locking your knees forcefully."
        ));

        exercises.add(new Exercise(
                "Leg Extension", "Legs",
                "An isolation exercise that targets the quadriceps.",
                "Beginner", "3 sets of 12-15 reps",
                "Adjust the machine correctly and extend and lower your legs under control."
        ));

        exercises.add(new Exercise(
                "Standing Calf Raises", "Legs",
                "An exercise targeting the calf muscles through ankle movement.",
                "Beginner", "3 sets of 12-15 reps",
                "Rise onto your toes slowly and lower your heels with control."
        ));

        // ==================== CARDIO ====================

        exercises.add(new Exercise(
                "High-Intensity Interval Training (HIIT)", "Cardio",
                "Alternating periods of higher-intensity exercise with recovery periods.",
                "Intermediate", "20-30 minutes",
                "Warm up, adjust intensity to your ability, and allow recovery periods."
        ));

        exercises.add(new Exercise(
                "Jump Rope", "Cardio",
                "A cardiovascular exercise that develops coordination and endurance.",
                "Beginner", "5 rounds of 2 minutes",
                "Land softly and rotate the rope using your wrists."
        ));

        exercises.add(new Exercise(
                "Jumping Jacks", "Cardio",
                "A full-body aerobic movement that increases heart rate and coordination.",
                "Beginner", "3 sets of 30 seconds",
                "Land softly, use a comfortable pace, and maintain balance."
        ));

        exercises.add(new Exercise(
                "Mountain Climbers", "Cardio",
                "A dynamic exercise combining a plank position with alternating knee drives.",
                "Intermediate", "3 sets of 20 reps",
                "Keep your hands stable, maintain a straight back, and move at a controlled pace."
        ));

        exercises.add(new Exercise(
                "High Knees", "Cardio",
                "A cardiovascular drill involving alternating knee lifts.",
                "Beginner", "3 sets of 30 seconds",
                "Stay balanced, land softly, and adjust your pace to your fitness level."
        ));

        exercises.add(new Exercise(
                "Jogging in Place", "Cardio",
                "A convenient aerobic exercise that can be performed in a small space.",
                "Beginner", "3 sets of 1 minute",
                "Use comfortable steps, maintain an upright posture, and land softly."
        ));

        // ==================== FULL BODY ====================

        exercises.add(new Exercise(
                "Burpees", "Full Body",
                "A full-body conditioning exercise combining squat, push-up, and jump movements.",
                "Intermediate", "4 sets of 10-15 reps",
                "Use controlled movements and land softly. Choose a step-back variation if needed."
        ));

        exercises.add(new Exercise(
                "Kettlebell Swing", "Full Body",
                "A hip-hinge movement developing power, stamina, and core strength.",
                "Intermediate", "3 sets of 15-20 reps",
                "Generate movement from your hips and maintain a neutral back."
        ));

        exercises.add(new Exercise(
                "Bodyweight Squats", "Full Body",
                "A bodyweight movement that trains the legs and engages the core.",
                "Beginner", "3 sets of 12-15 reps",
                "Keep your feet stable, knees aligned with your toes, and movement controlled."
        ));

        exercises.add(new Exercise(
                "Inchworms", "Full Body",
                "A bodyweight movement combining a forward fold and a walkout to a plank.",
                "Beginner", "3 sets of 8-10 reps",
                "Move slowly, keep your core engaged, and use a comfortable range of motion."
        ));

        exercises.add(new Exercise(
                "Bear Crawl", "Full Body",
                "A coordination and conditioning exercise that challenges the shoulders and core.",
                "Intermediate", "3 sets of 20 seconds",
                "Keep your movements controlled and your back stable."
        ));

        exercises.add(new Exercise(
                "Dumbbell Thrusters", "Full Body",
                "A compound exercise combining a squat and an overhead press.",
                "Intermediate", "3 sets of 8-12 reps",
                "Choose manageable weights, keep your torso stable, and press overhead with control."
        ));

        return exercises;
    }

    public static List<Exercise> getExercisesByCategory(String category) {
        List<Exercise> filtered = new ArrayList<>();

        if (category == null) {
            return filtered;
        }

        for (Exercise exercise : getAllExercises()) {
            if (exercise.getCategory() != null
                    && exercise.getCategory().equalsIgnoreCase(category.trim())) {
                filtered.add(exercise);
            }
        }

        return filtered;
    }

    public static List<Exercise> searchExercises(String query) {
        List<Exercise> filtered = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return getAllExercises();
        }

        String searchQuery = query.trim().toLowerCase(Locale.ROOT);

        for (Exercise exercise : getAllExercises()) {
            String name = exercise.getName() == null
                    ? "" : exercise.getName().toLowerCase(Locale.ROOT);

            String category = exercise.getCategory() == null
                    ? "" : exercise.getCategory().toLowerCase(Locale.ROOT);

            String description = exercise.getDescription() == null
                    ? "" : exercise.getDescription().toLowerCase(Locale.ROOT);

            if (name.contains(searchQuery)
                    || category.contains(searchQuery)
                    || description.contains(searchQuery)) {
                filtered.add(exercise);
            }
        }

        return filtered;
    }
}
