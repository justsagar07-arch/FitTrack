package com.fittrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fittrack.app.R;
import com.fittrack.app.models.WorkoutSchedule;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class WorkoutScheduleAdapter extends RecyclerView.Adapter<WorkoutScheduleAdapter.ViewHolder> {

    public interface OnScheduleActionListener {
        void onToggle(WorkoutSchedule schedule, boolean enabled);
        void onEdit(WorkoutSchedule schedule);
        void onDelete(WorkoutSchedule schedule);
    }

    private final List<WorkoutSchedule> schedules;
    private final OnScheduleActionListener listener;

    public WorkoutScheduleAdapter(List<WorkoutSchedule> schedules, OnScheduleActionListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutSchedule schedule = schedules.get(position);

        holder.tvTitle.setText(schedule.getTitle() == null || schedule.getTitle().trim().isEmpty() ? "Workout Schedule" : schedule.getTitle());
        holder.tvTime.setText(schedule.getFormattedTime());
        holder.tvDays.setText(schedule.getFormattedDays());

        holder.switchEnabled.setOnCheckedChangeListener(null);
        holder.switchEnabled.setChecked(schedule.isEnabled());

        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggle(schedule, isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(schedule);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(schedule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvTime;
        TextView tvDays;
        MaterialSwitch switchEnabled;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvDays = itemView.findViewById(R.id.tv_schedule_days);
            switchEnabled = itemView.findViewById(R.id.switch_schedule_enabled);
            btnEdit = itemView.findViewById(R.id.btn_edit_schedule);
            btnDelete = itemView.findViewById(R.id.btn_delete_schedule);
        }
    }
}
