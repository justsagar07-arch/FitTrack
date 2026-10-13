package com.fittrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fittrack.app.R;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    private List<String> categories;
    private OnCategoryClickListener listener;

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvCategoryName.setText(category);
        
        // Give cards different vibrant colored background accents for a premium modern feel
        int colorRes;
        switch (category) {
            case "Chest": colorRes = R.color.card_steps; break;
            case "Back": colorRes = R.color.card_calories; break;
            case "Shoulders": colorRes = R.color.card_heart; break;
            case "Arms": colorRes = R.color.card_analysis; break;
            case "Abs": colorRes = R.color.card_ai; break;
            case "Legs": colorRes = R.color.card_workout; break;
            case "Cardio": colorRes = R.color.card_diet; break;
            default: colorRes = R.color.card_reminders; break;
        }
        holder.iconContainer.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(colorRes));

        holder.cardCategory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCategory;
        MaterialCardView iconContainer;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.card_category);
            iconContainer = itemView.findViewById(R.id.icon_container);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}
