package com.fittrack.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fittrack.app.R;
import com.fittrack.app.activities.NutritionTrackerActivity;
import com.fittrack.app.models.Meal;
import com.fittrack.app.services.DietPlanManager;

import java.util.List;
import java.util.Locale;

public class DietFragment extends BaseFragment {

    private TextView tvRemainingValue;
    private TextView tvTargetValue;
    private TextView tvConsumedValue;
    private TextView tvBurnedValue;
    private TextView tvWaterProgress;

    private ProgressBar calorieProgressBar;

    private LinearLayout mealsContainer;

    private DietPlanManager dietPlanManager;


    // =========================================================
    // ON CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view =
                inflater.inflate(
                        R.layout.fragment_diet,
                        container,
                        false
                );


        View cardNutritionTracker = view.findViewById(R.id.card_nutrition_tracker);
        if (cardNutritionTracker != null) {
            cardNutritionTracker.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NutritionTrackerActivity.class);
                startActivity(intent);
            });
        }

        /*
         * =====================================================
         * DIET PLAN MANAGER
         * =====================================================
         */

        dietPlanManager =
                DietPlanManager.getInstance();


        /*
         * =====================================================
         * INITIALIZE VIEWS
         * =====================================================
         */

        tvRemainingValue =
                view.findViewById(
                        R.id.tvRemainingValue
                );

        tvTargetValue =
                view.findViewById(
                        R.id.tvTargetValue
                );

        tvConsumedValue =
                view.findViewById(
                        R.id.tvConsumedValue
                );

        tvBurnedValue =
                view.findViewById(
                        R.id.tvBurnedValue
                );

        tvWaterProgress =
                view.findViewById(
                        R.id.tvWaterProgress
                );

        calorieProgressBar =
                view.findViewById(
                        R.id.calorieProgressBar
                );

        mealsContainer =
                view.findViewById(
                        R.id.mealsContainer
                );


        /*
         * =====================================================
         * ADD WATER
         * =====================================================
         */

        Button btnAddWater =
                view.findViewById(
                        R.id.btnAddWater
                );


        btnAddWater.setOnClickListener(
                v -> {

                    int previousWater =
                            dietPlanManager
                                    .getWaterConsumed();


                    dietPlanManager.addWater();


                    int currentWater =
                            dietPlanManager
                                    .getWaterConsumed();


                    updateWaterUI();


                    if (
                            currentWater
                                    > previousWater
                    ) {

                        Toast.makeText(
                                requireContext(),
                                "Water added",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );


        /*
         * =====================================================
         * INITIAL UI
         * =====================================================
         */

        updateSummaryUI();

        updateWaterUI();

        populateMeals(
                inflater
        );


        /*
         * =====================================================
         * LOAD FIRESTORE DATA
         * =====================================================
         */

        loadDietPlanFromFirestore();


        return view;
    }


    // =========================================================
    // LOAD DIET PLAN FROM FIRESTORE
    // =========================================================

    private void loadDietPlanFromFirestore() {

        dietPlanManager.loadFromFirestore(
                new DietPlanManager.LoadCallback() {

                    @Override
                    public void onLoaded(
                            boolean dataFound
                    ) {

                        if (!isAdded()) {
                            return;
                        }


                        requireActivity()
                                .runOnUiThread(
                                        () -> {

                                            /*
                                             * Firestore se:
                                             *
                                             * Daily calorie goal
                                             * Calories burned
                                             * Water
                                             * Meal status
                                             *
                                             * load hone ke baad
                                             * complete UI refresh.
                                             */

                                            updateSummaryUI();

                                            updateWaterUI();

                                            populateMeals(
                                                    getLayoutInflater()
                                            );
                                        }
                                );
                    }


                    @Override
                    public void onError(
                            Exception error
                    ) {

                        if (!isAdded()) {
                            return;
                        }


                        requireActivity()
                                .runOnUiThread(
                                        () ->
                                                Toast.makeText(
                                                        requireContext(),
                                                        "Could not load Diet Plan",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                );
                    }
                }
        );
    }


    // =========================================================
    // POPULATE MEALS
    // =========================================================

    private void populateMeals(
            LayoutInflater inflater
    ) {

        mealsContainer.removeAllViews();


        List<Meal> meals =
                dietPlanManager.getMeals();


        for (
                int mealIndex = 0;
                mealIndex < meals.size();
                mealIndex++
        ) {

            Meal meal =
                    meals.get(
                            mealIndex
                    );


            View mealView =
                    inflater.inflate(
                            R.layout.item_meal,
                            mealsContainer,
                            false
                    );


            /*
             * =================================================
             * MEAL VIEWS
             * =================================================
             */

            ImageView mealIcon =
                    mealView.findViewById(
                            R.id.mealIcon
                    );

            TextView mealName =
                    mealView.findViewById(
                            R.id.mealName
                    );

            TextView mealDescription =
                    mealView.findViewById(
                            R.id.mealDescription
                    );

            TextView mealCalories =
                    mealView.findViewById(
                            R.id.mealCalories
                    );

            Button btnLog =
                    mealView.findViewById(
                            R.id.btnLog
                    );


            /*
             * =================================================
             * MEAL DATA
             * =================================================
             */

            mealIcon.setImageResource(
                    meal.getIconResId()
            );


            mealName.setText(
                    meal.getName()
            );


            mealDescription.setText(
                    meal.getDescription()
            );


            mealCalories.setText(
                    getString(
                            R.string.calories_value_format,
                            meal.getCalories()
                    )
            );


            /*
             * =================================================
             * BUTTON STATE
             * =================================================
             */

            updateMealButton(
                    btnLog,
                    meal.isLogged()
            );


            /*
             * =================================================
             * LOG MEAL
             * =================================================
             */

            final int currentMealIndex =
                    mealIndex;


            btnLog.setOnClickListener(
                    v -> {

                        boolean newLoggedState =
                                !meal.isLogged();


                        /*
                         * Manager:
                         *
                         * 1. State update
                         * 2. Firestore save
                         */

                        dietPlanManager.setMealLogged(
                                currentMealIndex,
                                newLoggedState
                        );


                        /*
                         * Update current UI.
                         */

                        updateMealButton(
                                btnLog,
                                meal.isLogged()
                        );


                        updateSummaryUI();
                    }
            );


            mealsContainer.addView(
                    mealView
            );
        }
    }


    // =========================================================
    // UPDATE MEAL BUTTON
    // =========================================================

    private void updateMealButton(
            Button btnLog,
            boolean isLogged
    ) {

        if (isLogged) {

            btnLog.setText(
                    getString(
                            R.string.logged_button
                    )
            );


            btnLog.setAlpha(
                    0.5f
            );


            btnLog.setEnabled(
                    false
            );

        } else {

            btnLog.setText(
                    getString(
                            R.string.log_button
                    )
            );


            btnLog.setAlpha(
                    1.0f
            );


            btnLog.setEnabled(
                    true
            );
        }
    }


    // =========================================================
    // UPDATE CALORIE SUMMARY
    // =========================================================

    private void updateSummaryUI() {

        /*
         * Actual values:
         *
         * target = user's dailyCalorieGoal
         * burned = today's activity calories
         */

        int target =
                dietPlanManager.getDailyTarget();


        int consumed =
                dietPlanManager.getCaloriesConsumed();


        int burned =
                dietPlanManager.getCaloriesBurned();


        int remaining =
                dietPlanManager.getRemainingCalories();


        /*
         * Target
         */

        tvTargetValue.setText(
                String.format(
                        Locale.getDefault(),
                        "%,d",
                        target
                )
        );


        /*
         * Consumed
         */

        tvConsumedValue.setText(
                String.format(
                        Locale.getDefault(),
                        "%,d",
                        consumed
                )
        );


        /*
         * Burned
         */

        tvBurnedValue.setText(
                String.format(
                        Locale.getDefault(),
                        "%,d",
                        burned
                )
        );


        /*
         * Remaining
         */

        tvRemainingValue.setText(
                String.format(
                        Locale.getDefault(),
                        "%,d",
                        remaining
                )
        );


        /*
         * =====================================================
         * PROGRESS BAR
         * =====================================================
         */

        int totalTarget =
                target + burned;


        if (totalTarget > 0) {

            int progress =
                    (int)
                            (
                                    (
                                            (float) consumed
                                                    / totalTarget
                                    )
                                            * 100
                            );


            /*
             * Keep progress between 0 and 100.
             */

            if (progress < 0) {

                progress = 0;
            }


            if (progress > 100) {

                progress = 100;
            }


            calorieProgressBar.setProgress(
                    progress
            );

        } else {

            calorieProgressBar.setProgress(
                    0
            );
        }
    }


    // =========================================================
    // UPDATE WATER UI
    // =========================================================

    private void updateWaterUI() {

        int consumed =
                dietPlanManager.getWaterConsumed();


        int target =
                dietPlanManager.getWaterTarget();


        tvWaterProgress.setText(
                getString(
                        R.string.water_progress_format,
                        consumed,
                        target
                )
        );
    }
}