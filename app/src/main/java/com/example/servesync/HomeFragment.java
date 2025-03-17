package com.example.servesync;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final String TAG = "HOME";
    private TextView dashboardMessage;
    private TextView locationTextView;
    private boolean locationDisplayed = false;
    private LocationManager locationManager;
    private LinearLayout roomsLayout;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationTextView = view.findViewById(R.id.locationTextView);
        dashboardMessage = view.findViewById(R.id.dashboardMessage);
        roomsLayout = view.findViewById(R.id.roomsLayout);

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        HashMap<String, String> userMap = (HashMap<String, String>) getArguments().getSerializable("userMap");

        dashboardMessage.setText("Welcome! " + userMap.get("userType") + " " + userMap.get("firstName") + " " + userMap.get("lastName") + "!");

        fetchRoomsByTeacher(userMap.get("userId"));
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationTextView.setText("Scanning your location...");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (!locationDisplayed) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                        locationDisplayed = true;
                    }
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            });
        }
    }


    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                locationTextView.setText("Location: " + fullAddress);
            } else {
                locationTextView.setText("Unable to fetch address.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            locationTextView.setText("Error fetching address.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                locationTextView.setText("Permission Denied");
            }
        }
    }

    private void fetchRoomsByTeacher(String uid) {
        String today = getTodayDayName();
        View view = getView();
        if (view == null) return;

        TextView classTitle = view.findViewById(R.id.classTitle);

        FirebaseFirestore.getInstance().collection("rooms")
                .whereEqualTo("teacherId", uid) // 🔥 Only fetch rooms where the teacher is assigned
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Map<String, String>> roomDetails = new HashMap<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String roomId = document.getId();
                            String subjectName = document.getString("subjectName");
                            String section = document.getString("section");
                            String endDate = document.getString("endDate");
                            List<String> schedule = (List<String>) document.get("schedule");

                            Timestamp startTimeStamp = document.getTimestamp("startTime");
                            Timestamp endTimeStamp = document.getTimestamp("endTime");

                            String startTime = formatTimestamp(startTimeStamp);
                            String endTime = formatTimestamp(endTimeStamp);

                            boolean isActive = checkIfActive(endDate);

                            if (isActive && schedule != null && schedule.contains(today) && subjectName != null && section != null) {
                                classTitle.setVisibility(View.VISIBLE);
                                classTitle.setText("Class for Today");

                                // Store details in a nested map
                                Map<String, String> detailsMap = new HashMap<>();
                                detailsMap.put("subjectName", subjectName);
                                detailsMap.put("section", section);
                                detailsMap.put("startTime", startTime);
                                detailsMap.put("endTime", endTime);

                                roomDetails.put(roomId, detailsMap);
                            }
                        }

                        if (!roomDetails.isEmpty()) {
                            classTitle.setVisibility(View.VISIBLE);
                            classTitle.setText("Class for Today");
                            roomsCard(roomDetails);
                        } else {
                            Log.d(TAG, "❌ No rooms scheduled for today.");
                            classTitle.setVisibility(View.VISIBLE);
                            classTitle.setText("No class for today");
                        }
                    } else {
                        Toast.makeText(getContext(), "No rooms scheduled for today.", Toast.LENGTH_SHORT).show();
                        classTitle.setVisibility(View.VISIBLE);
                        classTitle.setText("No class for today");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    classTitle.setVisibility(View.VISIBLE);
                    classTitle.setText("Failed to load class.");
                });
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp != null) {
            Date date = timestamp.toDate();
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date); // Convert to "7:30 AM"
        }
        return "Unknown Time"; // Default value if timestamp is null
    }

    private boolean checkIfActive(String endDate) {
        if (endDate == null || endDate.isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date end = sdf.parse(endDate);
            return end != null && new Date().before(end);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getTodayDayName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private void roomsCard(Map<String, Map<String, String>> roomDetails) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, Map<String, String>> entry : roomDetails.entrySet()) {
            Map<String, String> details = entry.getValue();

            View roomCard = inflater.inflate(R.layout.room_card, null, false);

            TextView txtIcon = roomCard.findViewById(R.id.txtIcon);
            TextView txtSubject = roomCard.findViewById(R.id.txtSubject);
            TextView txtSection = roomCard.findViewById(R.id.txtSection);
            TextView txtTime = roomCard.findViewById(R.id.txtTime);

            String subjectName = details.get("subjectName");
            String iconLetter = (subjectName != null && !subjectName.isEmpty()) ? subjectName.substring(0, 1) : "?";

            txtIcon.setText(iconLetter.toUpperCase());
            txtSubject.setText(subjectName);
            txtSection.setText(details.get("section"));
            txtTime.setText(details.get("startTime") + " - " + details.get("endTime"));

            roomsLayout.addView(roomCard);
        }
    }


}