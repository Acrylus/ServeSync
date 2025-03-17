package com.example.servesync;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.servesync.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private TextView titleTextView, fullNameTextView, idNumberTextView, emailTextView;
    private String userId, userType, firstName, lastName, mi, idNumber, email;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView burgerIcon;
    private FirebaseFirestore firestore;

    private final Map<Integer, String> fragmentTitles = new HashMap<Integer, String>() {{
        put(R.id.home, "Home");
        put(R.id.room, "Rooms");
        put(R.id.report, "Reports");
        put(R.id.event, "Events");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        titleTextView = findViewById(R.id.titleTextView);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        burgerIcon = findViewById(R.id.burger);

        View headerView = navigationView.getHeaderView(0);
        fullNameTextView = headerView.findViewById(R.id.userFullName);
        idNumberTextView = headerView.findViewById(R.id.idNumber);
        emailTextView = headerView.findViewById(R.id.userEmail);

        burgerIcon.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.openDrawer(navigationView);
            }
        });

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        });

        fetchUserDetails(userId);

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.room) {
                selectedFragment = new RoomFragment();
            } else if (itemId == R.id.report) {
                selectedFragment = new ReportFragment();
            } else if (itemId == R.id.event) {
                selectedFragment = new EventFragment();
            }

            if (selectedFragment != null) {
                openFragment(selectedFragment, itemId);
            }
            return true;
        });

        binding.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.faceRegister) {
                Intent intent = new Intent(MainActivity.this, FaceRegister.class);
                intent.putExtra("uid", userId);
                startActivity(intent);
                finish();

            } else if (id == R.id.notification) {
                //
            } else if (id == R.id.about) {
                //
            } else if (id == R.id.logout) {
                logout(); // ✅ Calls logout function
            }

            binding.drawerLayout.closeDrawer(binding.navigationView);
            return true;
        });
    }

    private void openFragment(Fragment fragment, int title) {
        Bundle bundle = new Bundle();

        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("firstName", firstName);
        userMap.put("middleName", mi);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("idNumber", idNumber);
        userMap.put("userType", userType);

        Log.d("USERMAP", "DEBUG: userMap = " + userMap.toString());

        bundle.putSerializable("userMap", userMap);

        fragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();

        if (titleTextView != null && fragmentTitles.containsKey(title)) {
            titleTextView.setText(fragmentTitles.get(title));
        }
    }

    private void fetchUserDetails(String userId) {
        String collectionPath = "";

        if (userType.equalsIgnoreCase("admin")) {
            collectionPath = "administrator";
        } else if (userType.equalsIgnoreCase("teacher")) {
            collectionPath = "teachers";
        } else if (userType.equalsIgnoreCase("student")) {
            collectionPath = "students";
        }

        FirebaseFirestore.getInstance().collection(collectionPath)
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        firstName = document.getString("firstName");
                        lastName = document.getString("lastName");
                        mi = document.getString("middleName");
                        idNumber = document.getString("idNumber");
                        email = document.getString("email");

                        fullNameTextView.setText(firstName + " " + mi + " " + lastName);
                        idNumberTextView.setText(idNumber);
                        emailTextView.setText(email);

                        openFragment(new HomeFragment(), R.id.home);
                    }
                })
                .addOnFailureListener(e -> {

                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }
}
