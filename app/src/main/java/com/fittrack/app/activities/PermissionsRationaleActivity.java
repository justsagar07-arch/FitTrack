package com.fittrack.app.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionsRationaleActivity
        extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        TextView textView =
                new TextView(this);

        textView.setText(
                "FitTrack uses Health Connect to read your daily step count. "
                        + "This information is used to display your activity "
                        + "and fitness progress inside FitTrack."
        );

        textView.setTextSize(18);

        textView.setPadding(
                48,
                48,
                48,
                48
        );

        textView.setGravity(
                Gravity.CENTER
        );

        setContentView(textView);
    }
}