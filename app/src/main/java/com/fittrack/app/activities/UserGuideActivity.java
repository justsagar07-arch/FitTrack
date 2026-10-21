package com.fittrack.app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fittrack.app.R;
import com.google.android.material.appbar.MaterialToolbar;

public class UserGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_user_guide);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
}
