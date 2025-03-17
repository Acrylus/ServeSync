package com.example.servesync;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Login extends AppCompatActivity {
    private EditText idNumberField;
    private EditText passwordField;
    private Button signInButton;
    private TextView forgotPassword;
    private TextView registerLink;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            Log.d("LOGIN", "User still logged in: " + firebaseAuth.getCurrentUser().getEmail() + ", UID: " + userId);

            if (!userId.isEmpty()) {
                redirectToDashboard(userId);
                return;
            }
        }

        setContentView(R.layout.activity_login);
        initializeViews();
        onClickSetUp();
    }

    private void redirectToDashboard(String userId) {
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String userType = getUserType(document);
                Intent intent;
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("userType", userType);
                startActivity(intent);
                finish();
            } else {
                firebaseAuth.signOut();
                Log.d("LOGIN", "User data not found. Signing out.");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error fetching user data.", Toast.LENGTH_SHORT).show();
            firebaseAuth.signOut();
        });
    }

    private void initializeViews() {
        idNumberField = findViewById(R.id.idNumberField);
        passwordField = findViewById(R.id.passwordField);
        signInButton = findViewById(R.id.signInButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        registerLink = findViewById(R.id.registerLink);
    }

    private void onClickSetUp() {
        signInButton.setOnClickListener(v -> {
            String idNumber = idNumberField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (validateInputs(idNumber, password)) {
                signInUser(idNumber, password);
            } else {
                Toast.makeText(Login.this, "Please enter both ID Number and password.", Toast.LENGTH_SHORT).show();
            }
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, ForgotPassword.class);
            startActivity(intent);
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            intent.putExtra("userType", "student"); // Always register as a student
            startActivity(intent);
        });
    }

    private boolean validateInputs(String idNumber, String password) {
        return !idNumber.isEmpty() && !password.isEmpty();
    }

    private void signInUser(String idNumber, String password) {
        firestore.collection("students")
                .whereEqualTo("idNumber", idNumber)
                .get()
                .addOnCompleteListener(studentTask -> {
                    if (studentTask.isSuccessful() && !studentTask.getResult().isEmpty()) {
                        DocumentSnapshot studentDoc = studentTask.getResult().getDocuments().get(0);
                        verifyLogin(studentDoc, "students", password);
                    } else {
                        firestore.collection("teachers")
                                .whereEqualTo("idNumber", idNumber)
                                .get()
                                .addOnCompleteListener(teacherTask -> {
                                    if (teacherTask.isSuccessful() && !teacherTask.getResult().isEmpty()) {
                                        DocumentSnapshot teacherDoc = teacherTask.getResult().getDocuments().get(0);
                                        verifyLogin(teacherDoc, "teachers", password);
                                    } else {
                                        firestore.collection("administrator") // Check administrator collection
                                                .whereEqualTo("idNumber", idNumber)
                                                .get()
                                                .addOnCompleteListener(adminTask -> {
                                                    if (adminTask.isSuccessful() && !adminTask.getResult().isEmpty()) {
                                                        DocumentSnapshot adminDoc = adminTask.getResult().getDocuments().get(0);
                                                        verifyLogin(adminDoc, "administrator", password);
                                                    } else {
                                                        Toast.makeText(Login.this, "Invalid ID Number or password.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void verifyLogin(DocumentSnapshot document, String collection, String password) {
        String email = document.getString("email");

        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email found for this account.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Authenticating: " + email, Toast.LENGTH_SHORT).show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Objects.requireNonNull(firebaseAuth.getCurrentUser()).reload().addOnCompleteListener(reloadTask -> {
                            if (reloadTask.isSuccessful()) {
                                boolean emailVerified = firebaseAuth.getCurrentUser().isEmailVerified();

                                if (!emailVerified) {
                                    Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                    firebaseAuth.signOut();
                                    return;
                                }

                                if (collection.equals("administrator")) {
                                    Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_LONG).show();
                                    navigateToDashboard(document);
                                    return;
                                }

                                Toast.makeText(this, "Login successful!", Toast.LENGTH_LONG).show();
                                navigateToDashboard(document);
                            } else {
                                Toast.makeText(this, "Error verifying email status. Try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Login Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard(DocumentSnapshot document) {
        String userType = getUserType(document);
        Log.d("Login", "User Type: " + userType);
        String userId = document.getId();

        Intent intent;
        intent = new Intent(this, MainActivity.class);

        intent.putExtra("userId", userId);
        intent.putExtra("userType", userType);
        startActivity(intent);
        finish();
    }

    private String getUserType(DocumentSnapshot document) {
        String userType = document.getString("userType");

        if (userType == null || userType.isEmpty()) {
            if (document.getReference().getPath().contains("teachers")) {
                userType = "Teacher";
            } else if (document.getReference().getPath().contains("students")) {
                userType = "Student";
            } else if (document.getReference().getPath().contains("administrator")) {
                userType = "admin";
            }
        }

        return userType != null ? userType : "Student";
    }
}
