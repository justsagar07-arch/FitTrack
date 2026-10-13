package com.fittrack.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fittrack.app.R;
import com.fittrack.app.activities.BodyAnalysisActivity;
import com.fittrack.app.activities.PostureDetectionActivity;
import com.fittrack.app.activities.WeeklyProgressActivity;

public class AnalysisFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnWeeklyProgress = view.findViewById(R.id.btn_view_weekly_progress);
        if (btnWeeklyProgress != null) {
            btnWeeklyProgress.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WeeklyProgressActivity.class);
                startActivity(intent);
            });
        }

        View btnBodyAnalysis = view.findViewById(R.id.btn_start_body_analysis);
        if (btnBodyAnalysis != null) {
            btnBodyAnalysis.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), BodyAnalysisActivity.class);
                startActivity(intent);
            });
        }

        View btnPosture = view.findViewById(R.id.btn_exercise_posture_detection);
        if (btnPosture != null) {
            btnPosture.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), PostureDetectionActivity.class);
                startActivity(intent);
            });
        }
    }
}
