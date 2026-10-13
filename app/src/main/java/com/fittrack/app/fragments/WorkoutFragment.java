
package com.fittrack.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.activities.ExerciseDetailActivity;
import com.fittrack.app.adapters.CategoryAdapter;
import com.fittrack.app.adapters.ExerciseAdapter;
import com.fittrack.app.data.ExerciseRepository;
import com.fittrack.app.models.Exercise;
import com.fittrack.app.models.ExerciseDataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WorkoutFragment extends BaseFragment
        implements CategoryAdapter.OnCategoryClickListener,
        ExerciseAdapter.OnExerciseClickListener {

    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageButton btnBackToCategories;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private RecyclerView rvCategories;
    private RecyclerView rvExercises;
    private LinearLayout layoutNoResults;

    private CategoryAdapter categoryAdapter;
    private ExerciseAdapter exerciseAdapter;

    private boolean isViewingCategory = false;
    private String selectedCategory = "";

    private final List<Exercise> allExercises = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_workout,
                container,
                false
        );

        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        btnBackToCategories = view.findViewById(R.id.btn_back_to_categories);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        rvCategories = view.findViewById(R.id.rv_categories);
        rvExercises = view.findViewById(R.id.rv_exercises);
        layoutNoResults = view.findViewById(R.id.layout_no_results);

        setupCategoriesRecyclerView();
        setupExercisesRecyclerView();
        setupSearchLogic();

        btnBackToCategories.setOnClickListener(v -> {
            etSearch.setText("");
            showCategoriesGrid();
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        loadExercisesFromFirestore();

        return view;
    }

    private void setupCategoriesRecyclerView() {
        List<String> categories = Arrays.asList(
                "Chest", "Back", "Shoulders", "Arms",
                "Abs", "Legs", "Cardio", "Full Body"
        );

        categoryAdapter = new CategoryAdapter(categories, this);

        rvCategories.setLayoutManager(
                new GridLayoutManager(getContext(), 2)
        );
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupExercisesRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(new ArrayList<>(), this);

        rvExercises.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        rvExercises.setAdapter(exerciseAdapter);
    }

    private void loadExercisesFromFirestore() {
        ExerciseRepository.loadExercises((exercises, fromFirestore) -> {
            if (!isAdded()) {
                return;
            }

            allExercises.clear();
            allExercises.addAll(exercises);

            String query = etSearch.getText().toString().trim();

            if (!query.isEmpty()) {
                performSearch(query);
            } else if (isViewingCategory) {
                showExercisesList(selectedCategory);
            } else {
                showCategoriesGrid();
            }

            if (!fromFirestore) {
                Toast.makeText(
                        requireContext(),
                        "Using saved local exercise list.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void setupSearchLogic() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);

                    if (isViewingCategory) {
                        showExercisesList(selectedCategory);
                    } else {
                        showCategoriesGrid();
                    }
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void performSearch(String query) {
        List<Exercise> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (Exercise exercise : allExercises) {
            if (exercise.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || exercise.getCategory().toLowerCase(Locale.ROOT).contains(lowerQuery)
                    || exercise.getDescription().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(exercise);
            }
        }

        isViewingCategory = false;

        tvTitle.setText("Search Results");
        tvSubtitle.setText("Showing results for \"" + query + "\"");
        btnBackToCategories.setVisibility(View.VISIBLE);
        rvCategories.setVisibility(View.GONE);

        if (results.isEmpty()) {
            rvExercises.setVisibility(View.GONE);
            layoutNoResults.setVisibility(View.VISIBLE);
        } else {
            layoutNoResults.setVisibility(View.GONE);
            rvExercises.setVisibility(View.VISIBLE);
            exerciseAdapter.updateList(results);
        }
    }

    private void showCategoriesGrid() {
        isViewingCategory = false;
        selectedCategory = "";

        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("Exercise Library");
        tvSubtitle.setText("Find your perfect workout routine");
        btnBackToCategories.setVisibility(View.GONE);

        rvCategories.setVisibility(View.VISIBLE);
        rvExercises.setVisibility(View.GONE);
        layoutNoResults.setVisibility(View.GONE);
    }

    private void showExercisesList(String category) {
        isViewingCategory = true;
        selectedCategory = category;

        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText(category);
        tvSubtitle.setText(
                "Best exercises targeting " + category.toLowerCase(Locale.ROOT)
        );
        btnBackToCategories.setVisibility(View.VISIBLE);

        rvCategories.setVisibility(View.GONE);

        List<Exercise> filteredList = new ArrayList<>();

        for (Exercise exercise : allExercises) {
            if (exercise.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(exercise);
            }
        }

        if (filteredList.isEmpty()) {
            rvExercises.setVisibility(View.GONE);
            layoutNoResults.setVisibility(View.VISIBLE);
        } else {
            layoutNoResults.setVisibility(View.GONE);
            rvExercises.setVisibility(View.VISIBLE);
            exerciseAdapter.updateList(filteredList);
        }
    }

    @Override
    public void onCategoryClick(String categoryName) {
        showExercisesList(categoryName);
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        Intent intent = new Intent(
                getActivity(),
                ExerciseDetailActivity.class
        );
        intent.putExtra("exercise", exercise);
        startActivity(intent);
    }
}
