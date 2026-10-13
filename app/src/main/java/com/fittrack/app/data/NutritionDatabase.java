package com.fittrack.app.data;

import com.fittrack.app.models.FoodItem;

import java.util.ArrayList;
import java.util.List;

public class NutritionDatabase {

    private static final List<FoodItem> VERIFIED_FOODS = new ArrayList<>();

    static {
        // USDA FoodData Central Verified Common Foods
        VERIFIED_FOODS.add(new FoodItem(
                "Oatmeal (Cooked)", "1 cup (234g)", 158, 6.0, 27.0, 3.0, "Breakfast", "USDA FDC ID: 173900"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Grilled Chicken Breast", "100g", 165, 31.0, 0.0, 3.6, "Lunch", "USDA FDC ID: 171077"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Large Whole Egg", "1 egg (50g)", 72, 6.3, 0.4, 4.8, "Breakfast", "USDA FDC ID: 171287"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Egg White", "1 large (33g)", 17, 3.6, 0.2, 0.1, "Breakfast", "USDA FDC ID: 172183"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Banana", "1 medium (118g)", 105, 1.3, 27.0, 0.3, "Snack", "USDA FDC ID: 173944"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Apple (Raw with skin)", "1 medium (182g)", 95, 0.5, 25.0, 0.3, "Snack", "USDA FDC ID: 171688"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Whole Wheat Bread", "1 slice (35g)", 80, 4.0, 14.0, 1.0, "Breakfast", "USDA FDC ID: 172688"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Greek Yogurt (Plain Nonfat)", "1 container (170g)", 100, 17.0, 6.0, 0.7, "Snack", "USDA FDC ID: 170903"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Brown Rice (Cooked)", "1 cup (195g)", 218, 4.5, 46.0, 1.6, "Dinner", "USDA FDC ID: 169704"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "White Rice (Cooked)", "1 cup (158g)", 205, 4.2, 45.0, 0.4, "Dinner", "USDA FDC ID: 168880"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Salmon Fillet (Baked/Grilled)", "100g", 206, 22.0, 0.0, 12.0, "Dinner", "USDA FDC ID: 175168"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Almonds (Raw)", "1 oz / 23 nuts (28g)", 164, 6.0, 6.1, 14.0, "Snack", "USDA FDC ID: 170567"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Whey Protein Powder", "1 scoop (30g)", 120, 24.0, 3.0, 1.5, "Snack", "Standard Whey Nutrition Label"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Peanut Butter", "2 tbsp (32g)", 188, 8.0, 7.0, 16.0, "Snack", "USDA FDC ID: 173957"
        ));
        VERIFIED_FOODS.add(new FoodItem(
                "Broccoli (Steamed)", "1 cup (156g)", 55, 3.7, 11.0, 0.6, "Dinner", "USDA FDC ID: 169967"
        ));
    }

    public static List<FoodItem> getAllVerifiedFoods() {
        return new ArrayList<>(VERIFIED_FOODS);
    }

    public static List<FoodItem> searchFoods(String query) {
        List<FoodItem> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllVerifiedFoods();
        }
        String lower = query.toLowerCase().trim();
        for (FoodItem item : VERIFIED_FOODS) {
            if (item.getName().toLowerCase().contains(lower)
                    || item.getMealCategory().toLowerCase().contains(lower)) {
                results.add(item);
            }
        }
        return results;
    }
}
