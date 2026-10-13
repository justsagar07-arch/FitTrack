package com.fittrack.app.services;

import android.util.Log;

import com.fittrack.app.R;
import com.fittrack.app.models.Meal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DietPlanManager {

    private static final String TAG = "FitTrackDiet";

    private static DietPlanManager instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;

    private List<Meal> meals;

    private int dailyTarget = 2500;
    private int caloriesBurned = 600;

    private int waterTarget = 8;
    private int waterConsumed = 0;

    private String currentDate;


    private DietPlanManager() {

        firebaseAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        currentDate =
                getTodayDate();

        meals =
                new ArrayList<>();

        meals.add(
                new Meal(
                        "Breakfast",
                        450,
                        "Oatmeal with berries and nuts",
                        R.drawable.ic_breakfast
                )
        );

        meals.add(
                new Meal(
                        "Lunch",
                        700,
                        "Grilled chicken salad with avocado",
                        R.drawable.ic_lunch
                )
        );

        meals.add(
                new Meal(
                        "Dinner",
                        600,
                        "Baked salmon with quinoa and broccoli",
                        R.drawable.ic_dinner
                )
        );

        meals.add(
                new Meal(
                        "Snacks",
                        250,
                        "Greek yogurt or a handful of almonds",
                        R.drawable.ic_snack
                )
        );
    }


    public static synchronized DietPlanManager getInstance() {

        if (instance == null) {
            instance =
                    new DietPlanManager();
        }

        return instance;
    }


    // =========================================================
    // TODAY'S DATE
    // =========================================================

    private String getTodayDate() {

        return LocalDate.now()
                .toString();
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public List<Meal> getMeals() {
        return meals;
    }

    public int getDailyTarget() {
        return dailyTarget;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public int getWaterTarget() {
        return waterTarget;
    }

    public int getWaterConsumed() {
        return waterConsumed;
    }


    // =========================================================
    // CALORIES CONSUMED
    // =========================================================

    public int getCaloriesConsumed() {

        int consumed = 0;

        for (Meal meal : meals) {

            if (meal.isLogged()) {

                consumed +=
                        meal.getCalories();
            }
        }

        return consumed;
    }


    // =========================================================
    // REMAINING CALORIES
    // =========================================================

    public int getRemainingCalories() {

        return
                (dailyTarget + caloriesBurned)
                        - getCaloriesConsumed();
    }


    // =========================================================
    // ADD WATER
    // =========================================================

    public void addWater() {

        if (waterConsumed < waterTarget) {

            waterConsumed++;

            saveToFirestore();
        }
    }


    // =========================================================
    // LOG / UNLOG MEAL
    // =========================================================

    public void setMealLogged(
            int mealIndex,
            boolean logged
    ) {

        if (
                mealIndex < 0
                        || mealIndex >= meals.size()
        ) {
            return;
        }

        meals.get(mealIndex)
                .setLogged(logged);

        saveToFirestore();
    }


    // =========================================================
    // LOAD FROM FIRESTORE
    // =========================================================

    public void loadFromFirestore(
            LoadCallback callback
    ) {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Log.e(
                    TAG,
                    "Cannot load diet plan: user not logged in"
            );

            if (callback != null) {
                callback.onLoaded(false);
            }

            return;
        }

        String userId =
                user.getUid();

        db.collection("users")
                .document(userId)
                .collection("dietPlan")
                .document("current")
                .get()
                .addOnSuccessListener(
                        dietDocument -> {

                            boolean dataFound =
                                    dietDocument.exists();

                            String today =
                                    getTodayDate();

                            /*
                             * =================================================
                             * CHECK SAVED DATE
                             * =================================================
                             */

                            String savedDate =
                                    dietDocument.getString(
                                            "date"
                                    );


                            if (
                                    dataFound
                                            && today.equals(savedDate)
                            ) {

                                /*
                                 * Same day:
                                 * existing meal/water state load karo.
                                 */

                                loadDietDocumentData(
                                        dietDocument
                                );

                                currentDate =
                                        today;

                                Log.d(
                                        TAG,
                                        "Today's diet plan loaded"
                                );

                            } else {

                                /*
                                 * New day:
                                 *
                                 * Meal logging reset.
                                 * Water reset.
                                 */

                                resetDailyData();

                                currentDate =
                                        today;

                                Log.d(
                                        TAG,
                                        "New day detected. Daily diet data reset."
                                );
                            }


                            /*
                             * Actual profile + activity data.
                             */

                            loadActualUserData(
                                    userId,
                                    dataFound,
                                    savedDate,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            Log.e(
                                    TAG,
                                    "Failed to load diet plan",
                                    error
                            );

                            resetDailyData();

                            loadActualUserData(
                                    userId,
                                    false,
                                    null,
                                    callback
                            );
                        }
                );
    }


    // =========================================================
    // RESET DAILY DATA
    // =========================================================

    private void resetDailyData() {

        /*
         * Reset water.
         */

        waterConsumed = 0;


        /*
         * Reset all meal logging.
         */

        for (Meal meal : meals) {

            meal.setLogged(false);
        }


        Log.d(
                TAG,
                "Daily meals and water reset"
        );
    }


    // =========================================================
    // LOAD ACTUAL USER DATA
    // =========================================================

    private void loadActualUserData(
            String userId,
            boolean dietDataFound,
            String savedDate,
            LoadCallback callback
    ) {

        /*
         * User profile
         */

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        userDocument -> {

                            loadDailyCalorieGoal(
                                    userDocument
                            );

                            loadTodayBurnedCalories(
                                    userId,
                                    dietDataFound,
                                    savedDate,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            Log.e(
                                    TAG,
                                    "Failed to load user profile",
                                    error
                            );

                            loadTodayBurnedCalories(
                                    userId,
                                    dietDataFound,
                                    savedDate,
                                    callback
                            );
                        }
                );
    }


    // =========================================================
    // DAILY CALORIE GOAL
    // =========================================================

    private void loadDailyCalorieGoal(
            DocumentSnapshot userDocument
    ) {

        if (
                userDocument == null
                        || !userDocument.exists()
        ) {
            return;
        }

        Long calorieGoal =
                userDocument.getLong(
                        "dailyCalorieGoal"
                );

        if (calorieGoal != null) {

            dailyTarget =
                    calorieGoal.intValue();

            Log.d(
                    TAG,
                    "Daily Calorie Goal = "
                            + dailyTarget
            );
        }
    }


    // =========================================================
    // TODAY'S BURNED CALORIES
    // =========================================================

    private void loadTodayBurnedCalories(
            String userId,
            boolean dietDataFound,
            String savedDate,
            LoadCallback callback
    ) {

        db.collection("users")
                .document(userId)
                .collection("activity")
                .document("today")
                .get()
                .addOnSuccessListener(
                        activityDocument -> {

                            if (
                                    activityDocument != null
                                            && activityDocument.exists()
                            ) {

                                Long calories =
                                        activityDocument.getLong(
                                                "calories"
                                        );

                                if (calories != null) {

                                    caloriesBurned =
                                            calories.intValue();

                                    Log.d(
                                            TAG,
                                            "Calories Burned = "
                                                    + caloriesBurned
                                    );
                                }
                            }


                            /*
                             * Agar new day hai, reset data ko
                             * today's date ke saath Firestore mein
                             * save kar do.
                             */

                            String today =
                                    getTodayDate();

                            if (
                                    savedDate == null
                                            || !today.equals(savedDate)
                            ) {

                                saveToFirestore();
                            }


                            if (callback != null) {

                                callback.onLoaded(
                                        dietDataFound
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        error -> {

                            Log.e(
                                    TAG,
                                    "Failed to load today's activity",
                                    error
                            );


                            String today =
                                    getTodayDate();

                            if (
                                    savedDate == null
                                            || !today.equals(savedDate)
                            ) {

                                saveToFirestore();
                            }


                            if (callback != null) {

                                callback.onLoaded(
                                        dietDataFound
                                );
                            }
                        }
                );
    }


    // =========================================================
    // LOAD SAVED DIET DOCUMENT
    // =========================================================

    private void loadDietDocumentData(
            DocumentSnapshot document
    ) {

        /*
         * Water target
         */

        Long savedWaterTarget =
                document.getLong(
                        "waterTarget"
                );

        if (savedWaterTarget != null) {

            waterTarget =
                    savedWaterTarget.intValue();
        }


        /*
         * Water consumed
         */

        Long savedWaterConsumed =
                document.getLong(
                        "waterConsumed"
                );

        if (savedWaterConsumed != null) {

            waterConsumed =
                    savedWaterConsumed.intValue();
        }


        /*
         * Breakfast
         */

        Boolean breakfastLogged =
                document.getBoolean(
                        "breakfastLogged"
                );

        if (
                breakfastLogged != null
                        && meals.size() > 0
        ) {

            meals.get(0)
                    .setLogged(
                            breakfastLogged
                    );
        }


        /*
         * Lunch
         */

        Boolean lunchLogged =
                document.getBoolean(
                        "lunchLogged"
                );

        if (
                lunchLogged != null
                        && meals.size() > 1
        ) {

            meals.get(1)
                    .setLogged(
                            lunchLogged
                    );
        }


        /*
         * Dinner
         */

        Boolean dinnerLogged =
                document.getBoolean(
                        "dinnerLogged"
                );

        if (
                dinnerLogged != null
                        && meals.size() > 2
        ) {

            meals.get(2)
                    .setLogged(
                            dinnerLogged
                    );
        }


        /*
         * Snacks
         */

        Boolean snacksLogged =
                document.getBoolean(
                        "snacksLogged"
                );

        if (
                snacksLogged != null
                        && meals.size() > 3
        ) {

            meals.get(3)
                    .setLogged(
                            snacksLogged
                    );
        }
    }


    // =========================================================
    // SAVE TO FIRESTORE
    // =========================================================

    public void saveToFirestore() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Log.e(
                    TAG,
                    "Cannot save diet plan: user not logged in"
            );

            return;
        }

        String userId =
                user.getUid();


        String today =
                getTodayDate();


        currentDate =
                today;


        Map<String, Object> dietData =
                new HashMap<>();


        /*
         * =====================================================
         * DATE
         * =====================================================
         */

        dietData.put(
                "date",
                today
        );


        /*
         * =====================================================
         * ACTUAL CALORIE VALUES
         * =====================================================
         */

        dietData.put(
                "dailyTarget",
                dailyTarget
        );

        dietData.put(
                "caloriesBurned",
                caloriesBurned
        );


        /*
         * =====================================================
         * WATER
         * =====================================================
         */

        dietData.put(
                "waterTarget",
                waterTarget
        );

        dietData.put(
                "waterConsumed",
                waterConsumed
        );


        /*
         * =====================================================
         * MEALS
         * =====================================================
         */

        if (meals.size() > 0) {

            dietData.put(
                    "breakfastLogged",
                    meals.get(0).isLogged()
            );
        }


        if (meals.size() > 1) {

            dietData.put(
                    "lunchLogged",
                    meals.get(1).isLogged()
            );
        }


        if (meals.size() > 2) {

            dietData.put(
                    "dinnerLogged",
                    meals.get(2).isLogged()
            );
        }


        if (meals.size() > 3) {

            dietData.put(
                    "snacksLogged",
                    meals.get(3).isLogged()
            );
        }


        /*
         * =====================================================
         * USER / PROJECT
         * =====================================================
         */

        dietData.put(
                "userId",
                userId
        );

        dietData.put(
                "project",
                "FitTrack"
        );

        dietData.put(
                "timestamp",
                System.currentTimeMillis()
        );


        /*
         * =====================================================
         * FIRESTORE
         * =====================================================
         */

        db.collection("users")
                .document(userId)
                .collection("dietPlan")
                .document("current")
                .set(
                        dietData,
                        SetOptions.merge()
                )
                .addOnSuccessListener(
                        unused ->
                                Log.d(
                                        TAG,
                                        "Diet plan saved for "
                                                + today
                                )
                )
                .addOnFailureListener(
                        error ->
                                Log.e(
                                        TAG,
                                        "Failed to save diet plan",
                                        error
                                )
                );
    }


    // =========================================================
    // CALLBACK
    // =========================================================

    public interface LoadCallback {

        void onLoaded(
                boolean dataFound
        );

        default void onError(
                Exception error
        ) {
        }
    }
}