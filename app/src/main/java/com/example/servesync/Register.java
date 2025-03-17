package com.example.servesync;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText emailField, firstNameField, middleNameField, lastNameField, idNumberField, passwordField, confirmPasswordField;
    private EditText courseYearField, dobField, homeAddressField, cityAddressField, contactNumberField;
    private Spinner genderSpinner;
    private Button signUpButton;
    private TextView welcomeText, subtitleText;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private TextView alreadyHaveAccount;
    private boolean isNextClicked = false;
    private Fragment userDetailsFragment, studentDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        userDetailsFragment = new UserDetailsFragment();
        studentDetailsFragment = new StudentDetailsFragment();

        loadFragment(userDetailsFragment);

        initializeViews();

        String userType = getIntent().getStringExtra("userType");

        subtitleText.setText("Register as " + capitalizeFirstLetter(userType));

        signUpButton.setText(userType.equalsIgnoreCase("student") ? "NEXT" : "SIGN UP");

        signUpButton.setOnClickListener(v -> {
            Log.d("DEBUG", "📌 Sign Up Button Clicked - User Type: " + userType);

            if (userType.equalsIgnoreCase("teacher")) {
                if (validateInitialInputs()) {
                    Log.d("DEBUG", "✅ Registering Teacher...");
                    registerTeacher();
                }
            } else { // Student Registration Flow
                if (!isNextClicked) { // First click → Switch Fragment
                    if (validateInitialInputs()) {
                        Log.d("DEBUG", "🚀 Switching to StudentDetailsFragment...");
                        loadFragment(studentDetailsFragment);
                        welcomeText.setText("Student Information");
                        isNextClicked = true;
                        signUpButton.setText("SIGN UP"); // Change button text after switching
                    }
                } else if (validateStudentInputs()) { // Second click → Register student
                    Log.d("DEBUG", "✅ Registering Student...");
                    registerStudent();
                }
            }
        });




        alreadyHaveAccount.setOnClickListener(v -> navigateToLogin());
    }

    private void initializeViews() {
        emailField = findViewById(R.id.emailField);
        firstNameField = findViewById(R.id.firstNameField);
        middleNameField = findViewById(R.id.middleNameField);
        lastNameField = findViewById(R.id.lastNameField);
        idNumberField = findViewById(R.id.idNumberField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        courseYearField = findViewById(R.id.courseYearField);
        dobField = findViewById(R.id.dobPicker);
        homeAddressField = findViewById(R.id.homeAddressField);
        cityAddressField = findViewById(R.id.cityAddressField);
        contactNumberField = findViewById(R.id.contactNumberField);
        genderSpinner = findViewById(R.id.genderSpinner);
        signUpButton = findViewById(R.id.signUpButton);
        welcomeText = findViewById(R.id.welcomeText);
        subtitleText = findViewById(R.id.subtitleText);
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Log.d("DEBUG", "🔄 Replacing Fragment with: " + fragment.getClass().getSimpleName());

        transaction.replace(R.id.frameDetails, fragment);
        transaction.addToBackStack(null); // Enable back navigation
        transaction.commit();
    }



    private boolean validateInitialInputs() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        String idNumber = idNumberField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!email.endsWith("@cit.edu") || !email.endsWith("@gmail.com")) {
            Toast.makeText(this, "Email must be @cit.edu domain!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidIdNumber(idNumber)) {
            Toast.makeText(this, "Invalid ID Number! Format must be 00-0000-000.", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 8 characters, include 1 uppercase, 1 number, and 1 special character.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }

    private boolean isValidIdNumber(String idNumber) {
        String idNumberPattern = "^\\d{2}-\\d{4}-\\d{3}$";
        return idNumber.matches(idNumberPattern);
    }

    private boolean validateStudentInputs() {
        String contactNumber = contactNumberField.getText().toString().trim();

        if (TextUtils.isEmpty(courseYearField.getText().toString()) ||
                TextUtils.isEmpty(homeAddressField.getText().toString()) ||
                TextUtils.isEmpty(cityAddressField.getText().toString()) ||
                TextUtils.isEmpty(contactNumberField.getText().toString())) {
            Toast.makeText(this, "All student fields are required!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidContactNumber(contactNumber)) {
            Toast.makeText(this, "Invalid Contact Number! Format must be +639XXXXXXXXXX.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean isValidContactNumber(String contactNumber) {
        String contactPattern = "^\\+63\\d{10}$";
        return contactNumber.matches(contactPattern);
    }

    private void registerTeacher() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendEmailVerification(() -> saveTeacherToFirestore());
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveTeacherToFirestore() {
        String uid = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("email", emailField.getText().toString());
        teacherData.put("firstName", firstNameField.getText().toString());
        teacherData.put("middleName", middleNameField.getText().toString());
        teacherData.put("lastName", lastNameField.getText().toString());
        teacherData.put("idNumber", idNumberField.getText().toString());
        teacherData.put("isConfirmedByAdmin", true);

        firestore.collection("teachers").document(uid).set(teacherData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Teacher registered. Verification email sent.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void registerStudent() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendEmailVerification(() -> saveStudentToFirestore());
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification(Runnable onSuccess) {
        firebaseAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveStudentToFirestore() {
        String uid = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("email", emailField.getText().toString());
        studentData.put("firstName", firstNameField.getText().toString());
        studentData.put("middleName", middleNameField.getText().toString());
        studentData.put("lastName", lastNameField.getText().toString());
        studentData.put("idNumber", idNumberField.getText().toString());

        firestore.collection("students").document(uid).set(studentData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student registered successfully.", Toast.LENGTH_LONG).show();
                    finish(); // Go back to the previous screen or close registration
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, Login.class));
        finish();
    }

    private String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
