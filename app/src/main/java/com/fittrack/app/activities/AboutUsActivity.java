package com.fittrack.app.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fittrack.app.R;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
