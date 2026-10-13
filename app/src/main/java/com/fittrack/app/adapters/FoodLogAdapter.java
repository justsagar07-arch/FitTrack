package com.fittrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.models.FoodLogEntry;

import java.util.List;
import java.util.Locale;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {

    public interface OnFoodLogActionListener {
        void onDelete(FoodLogEntry entry);
    }

    private final List<FoodLogEntry> entries;
    private final OnFoodLogActionListener listener;

    public FoodLogAdapter(List<FoodLogEntry> entries, OnFoodLogActionListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodLogEntry entry = entries.get(position);

        holder.tvFoodName.setText(entry.getFoodName());
        holder.tvMealTypeTag.setText(entry.getMealType() == null ? "Meal" : entry.getMealType());

        holder.tvServingInfo.setText(
                String.format(Locale.getDefault(), "%.1fx %s", entry.getServings(), entry.getServingSize())
        );

        holder.tvCaloriesVal.setText(
                String.format(Locale.getDefault(), "%d kcal", entry.getCalories())
        );

        holder.tvMacroInfo.setText(
                String.format(
                        Locale.getDefault(),
                        "P: %.1fg | C: %.1fg | F: %.1fg",
                        entry.getProteinGrams(),
                        entry.getCarbsGrams(),
                        entry.getFatGrams()
                )
        );

        if (entry.getDataSource() != null && !entry.getDataSource().trim().isEmpty()) {
            holder.tvDataSource.setText("Source: " + entry.getDataSource());
            holder.tvDataSource.setVisibility(View.VISIBLE);
        } else {
            holder.tvDataSource.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName;
        TextView tvMealTypeTag;
        TextView tvServingInfo;
        TextView tvCaloriesVal;
        TextView tvMacroInfo;
        TextView tvDataSource;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvMealTypeTag = itemView.findViewById(R.id.tv_meal_type_tag);
            tvServingInfo = itemView.findViewById(R.id.tv_serving_info);
            tvCaloriesVal = itemView.findViewById(R.id.tv_calories_val);
            tvMacroInfo = itemView.findViewById(R.id.tv_macro_info);
            tvDataSource = itemView.findViewById(R.id.tv_data_source);
            btnDelete = itemView.findViewById(R.id.btn_delete_food_log);
        }
    }
}
