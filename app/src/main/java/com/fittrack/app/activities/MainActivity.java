package com.fittrack.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.health.connect.client.PermissionController;

import com.fittrack.app.R;
import com.fittrack.app.fragments.AnalysisFragment;
import com.fittrack.app.fragments.DietFragment;
import com.fittrack.app.fragments.HomeFragment;
import com.fittrack.app.fragments.ProfileFragment;
import com.fittrack.app.fragments.WorkoutFragment;
import com.fittrack.app.services.HealthConnectManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private final ActivityResultLauncher<Set<String>> healthPermissionLauncher =
            registerForActivityResult(
                    PermissionController.createRequestPermissionResultContract(),
                    grantedPermissions -> Toast.makeText(
                            this,
                            "Health Connect permission updated",
                            Toast.LENGTH_SHORT
                    ).show()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        String openTab = getIntent().getStringExtra("open_tab");

        if (savedInstanceState == null) {
            if ("workout".equals(openTab)) {
                loadFragment(new WorkoutFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_workout);
            } else if ("diet".equals(openTab)) {
                loadFragment(new DietFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_diet);
            } else {
                loadFragment(new HomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_analysis) {
                selectedFragment = new AnalysisFragment();
            } else if (itemId == R.id.nav_workout) {
                selectedFragment = new WorkoutFragment();
            } else if (itemId == R.id.nav_diet) {
                selectedFragment = new DietFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                selectedFragment = new HomeFragment();
            }

            loadFragment(selectedFragment);
            return true;
        });

        testFirestoreConnection();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openTab = intent.getStringExtra("open_tab");
        if ("workout".equals(openTab)) {
            loadFragment(new WorkoutFragment());
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_workout);
            }
        } else if ("diet".equals(openTab)) {
            loadFragment(new DietFragment());
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_diet);
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void testFirestoreConnection() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> testData = new HashMap<>();
        testData.put("project", "FitTrack");
        testData.put("status", "Connected");

        db.collection("test")
                .add(testData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(MainActivity.this, "Firestore Connected!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void requestHealthConnectPermissions() {
        HealthConnectManager healthConnectManager = new HealthConnectManager(this);

        if (!healthConnectManager.isAvailable()) {
            Toast.makeText(this, "Health Connect is not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        Set<String> permissions = healthConnectManager.getRequiredPermissions();

        if (permissions == null || permissions.isEmpty()) {
            Toast.makeText(this, "No Health Connect permissions configured", Toast.LENGTH_LONG).show();
            return;
        }

        healthPermissionLauncher.launch(permissions);
    }
}
