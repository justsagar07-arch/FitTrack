package com.fittrack.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.adapters.FoodLogAdapter;
import com.fittrack.app.data.NutritionDatabase;
import com.fittrack.app.models.FoodItem;
import com.fittrack.app.models.FoodLogEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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

public class NutritionTrackerActivity extends AppCompatActivity implements FoodLogAdapter.OnFoodLogActionListener {

    private ProgressBar progressLoading;
    private TextView tvCurrentDateDisplay;
    private TextView tvTotalCaloriesDisplay;
    private LinearProgressIndicator progressCaloriesDaily;
    private TextView tvTotalProtein;
    private TextView tvTotalCarbs;
    private TextView tvTotalFat;
    private RecyclerView rvFoodLogEntries;
    private TextView tvNoFoodEntries;

    private FoodLogAdapter foodLogAdapter;
    private final List<FoodLogEntry> foodLogEntries = new ArrayList<>();

    private Calendar selectedDate;

    private static final SimpleDateFormat DATE_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_tracker);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_nutrition);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressLoading = findViewById(R.id.progress_loading);
        tvCurrentDateDisplay = findViewById(R.id.tv_current_date_display);
        tvTotalCaloriesDisplay = findViewById(R.id.tv_total_calories_display);
        progressCaloriesDaily = findViewById(R.id.progress_calories_daily);
        tvTotalProtein = findViewById(R.id.tv_total_protein);
        tvTotalCarbs = findViewById(R.id.tv_total_carbs);
        tvTotalFat = findViewById(R.id.tv_total_fat);
        rvFoodLogEntries = findViewById(R.id.rv_food_log_entries);
        tvNoFoodEntries = findViewById(R.id.tv_no_food_entries);

        ImageButton btnPrevDate = findViewById(R.id.btn_prev_date);
        ImageButton btnNextDate = findViewById(R.id.btn_next_date);
        MaterialButton btnAddFoodItem = findViewById(R.id.btn_add_food_item);

        selectedDate = Calendar.getInstance();
        updateDateDisplay();

        setupRecyclerView();

        btnPrevDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, -1);
            updateDateDisplay();
            loadFoodLogsForSelectedDate();
        });

        btnNextDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, 1);
            updateDateDisplay();
            loadFoodLogsForSelectedDate();
        });

        btnAddFoodItem.setOnClickListener(v -> showLogFoodDialog());

        loadFoodLogsForSelectedDate();
    }

    private void setupRecyclerView() {
        foodLogAdapter = new FoodLogAdapter(foodLogEntries, this);
        rvFoodLogEntries.setLayoutManager(new LinearLayoutManager(this));
        rvFoodLogEntries.setAdapter(foodLogAdapter);
    }

    private void updateDateDisplay() {
        Calendar today = Calendar.getInstance();

        if (isSameDay(selectedDate, today)) {
            tvCurrentDateDisplay.setText("Today, " + DISPLAY_DATE_FORMAT.format(selectedDate.getTime()));
        } else {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(selectedDate, yesterday)) {
                tvCurrentDateDisplay.setText("Yesterday, " + DISPLAY_DATE_FORMAT.format(selectedDate.getTime()));
            } else {
                tvCurrentDateDisplay.setText(DISPLAY_DATE_FORMAT.format(selectedDate.getTime()));
            }
        }
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadFoodLogsForSelectedDate() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Please sign in to view food logs.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dateKey = DATE_KEY_FORMAT.format(selectedDate.getTime());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("foodLogs")
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressLoading.setVisibility(View.GONE);
                    foodLogEntries.clear();

                    int totalCalories = 0;
                    double totalProtein = 0.0;
                    double totalCarbs = 0.0;
                    double totalFat = 0.0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        FoodLogEntry entry = doc.toObject(FoodLogEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            foodLogEntries.add(entry);

                            totalCalories += entry.getCalories();
                            totalProtein += entry.getProteinGrams();
                            totalCarbs += entry.getCarbsGrams();
                            totalFat += entry.getFatGrams();
                        }
                    }

                    updateTotalsAndListUI(totalCalories, totalProtein, totalCarbs, totalFat);
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(NutritionTrackerActivity.this, "Could not load food logs.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTotalsAndListUI(int totalCalories, double totalProtein, double totalCarbs, double totalFat) {
        tvTotalCaloriesDisplay.setText(String.format(Locale.getDefault(), "%d kcal", totalCalories));

        int targetCalories = 2000;
        int calPercent = (int) Math.min(100, ((float) totalCalories / targetCalories) * 100);
        progressCaloriesDaily.setProgress(calPercent);

        tvTotalProtein.setText(String.format(Locale.getDefault(), "%.1f g", totalProtein));
        tvTotalCarbs.setText(String.format(Locale.getDefault(), "%.1f g", totalCarbs));
        tvTotalFat.setText(String.format(Locale.getDefault(), "%.1f g", totalFat));

        if (foodLogEntries.isEmpty()) {
            rvFoodLogEntries.setVisibility(View.GONE);
            tvNoFoodEntries.setVisibility(View.VISIBLE);
        } else {
            tvNoFoodEntries.setVisibility(View.GONE);
            rvFoodLogEntries.setVisibility(View.VISIBLE);
            foodLogAdapter.notifyDataSetChanged();
        }
    }

    private void showLogFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Food Item");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(48, 32, 48, 16);

        // Food Item Selector
        TextView tvSelectFoodLabel = new TextView(this);
        tvSelectFoodLabel.setText("Select Food (USDA Database or Custom):");
        tvSelectFoodLabel.setTextSize(14);
        tvSelectFoodLabel.setPadding(0, 0, 0, 8);
        dialogLayout.addView(tvSelectFoodLabel);

        List<FoodItem> verifiedFoods = NutritionDatabase.getAllVerifiedFoods();
        List<String> foodNames = new ArrayList<>();
        for (FoodItem item : verifiedFoods) {
            foodNames.add(item.getName() + " (" + item.getServingSize() + ")");
        }

        AutoCompleteTextView acFood = new AutoCompleteTextView(this);
        acFood.setHint("Search or type food name");
        ArrayAdapter<String> foodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, foodNames);
        acFood.setAdapter(foodAdapter);
        acFood.setThreshold(1);
        dialogLayout.addView(acFood);

        // Meal Category Spinner
        TextView tvMealTypeLabel = new TextView(this);
        tvMealTypeLabel.setText("Meal Type:");
        tvMealTypeLabel.setTextSize(14);
        tvMealTypeLabel.setPadding(0, 16, 0, 8);
        dialogLayout.addView(tvMealTypeLabel);

        Spinner spinnerMealType = new Spinner(this);
        String[] mealCategories = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mealCategories);
        spinnerMealType.setAdapter(mealAdapter);
        dialogLayout.addView(spinnerMealType);

        // Servings Input
        TextView tvServingsLabel = new TextView(this);
        tvServingsLabel.setText("Servings:");
        tvServingsLabel.setTextSize(14);
        tvServingsLabel.setPadding(0, 16, 0, 8);
        dialogLayout.addView(tvServingsLabel);

        EditText etServings = new EditText(this);
        etServings.setHint("1.0");
        etServings.setText("1.0");
        etServings.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialogLayout.addView(etServings);

        // Nutrition Details Preview
        TextView tvPreview = new TextView(this);
        tvPreview.setText("Source: USDA FoodData Central");
        tvPreview.setTextSize(12);
        tvPreview.setPadding(0, 16, 0, 8);
        tvPreview.setTextColor(getColor(R.color.text_secondary_light));
        dialogLayout.addView(tvPreview);

        builder.setView(dialogLayout);

        builder.setPositiveButton("Log Entry", (dialog, which) -> {
            String selectedFoodText = acFood.getText().toString().trim();
            if (selectedFoodText.isEmpty()) {
                Toast.makeText(this, "Please select or enter a food item.", Toast.LENGTH_SHORT).show();
                return;
            }

            String servingsStr = etServings.getText().toString().trim();
            double servings = 1.0;
            if (!servingsStr.isEmpty()) {
                try {
                    servings = Double.parseDouble(servingsStr);
                } catch (NumberFormatException ignored) {
                }
            }

            String mealType = (String) spinnerMealType.getSelectedItem();

            // Match from USDA database if available, otherwise custom
            FoodItem matchedItem = null;
            for (FoodItem item : verifiedFoods) {
                if (selectedFoodText.startsWith(item.getName())) {
                    matchedItem = item;
                    break;
                }
            }

            String foodName;
            String servingSize;
            int calories;
            double protein;
            double carbs;
            double fat;
            String dataSource;

            if (matchedItem != null) {
                foodName = matchedItem.getName();
                servingSize = matchedItem.getServingSize();
                calories = (int) Math.round(matchedItem.getCalories() * servings);
                protein = matchedItem.getProteinGrams() * servings;
                carbs = matchedItem.getCarbsGrams() * servings;
                fat = matchedItem.getFatGrams() * servings;
                dataSource = matchedItem.getDataSource();
            } else {
                foodName = selectedFoodText;
                servingSize = "1 serving";
                calories = (int) Math.round(150 * servings); // Default estimate for custom food
                protein = 5.0 * servings;
                carbs = 20.0 * servings;
                fat = 3.0 * servings;
                dataSource = "Custom User Log";
            }

            saveFoodLogEntry(foodName, servingSize, servings, calories, protein, carbs, fat, mealType, dataSource);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveFoodLogEntry(
            String foodName,
            String servingSize,
            double servings,
            int calories,
            double protein,
            double carbs,
            double fat,
            String mealType,
            String dataSource
    ) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to log food.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dateKey = DATE_KEY_FORMAT.format(selectedDate.getTime());
        String entryId = UUID.randomUUID().toString();

        FoodLogEntry entry = new FoodLogEntry(
                entryId,
                foodName,
                servingSize,
                servings,
                calories,
                protein,
                carbs,
                fat,
                mealType,
                System.currentTimeMillis(),
                dateKey,
                dataSource
        );

        progressLoading.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("foodLogs")
                .document(entryId)
                .set(entry)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(NutritionTrackerActivity.this, "Food logged successfully!", Toast.LENGTH_SHORT).show();
                    loadFoodLogsForSelectedDate();
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(NutritionTrackerActivity.this, "Could not save food log.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDelete(FoodLogEntry entry) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || entry.getId() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressLoading.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("foodLogs")
                .document(entry.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(NutritionTrackerActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    loadFoodLogsForSelectedDate();
                })
                .addOnFailureListener(error -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(NutritionTrackerActivity.this, "Could not delete entry.", Toast.LENGTH_SHORT).show();
                });
    }
}
