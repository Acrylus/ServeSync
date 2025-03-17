package com.example.servesync;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.Calendar;

public class UserDetailsFragment extends Fragment {
    private EditText emailField, firstNameField, middleNameField, lastNameField, idNumberField, passwordField, confirmPasswordField;

    public UserDetailsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
    }

    private void initializeViews(View view) {
        emailField = view.findViewById(R.id.emailField);
        firstNameField = view.findViewById(R.id.firstNameField);
        middleNameField = view.findViewById(R.id.middleNameField);
        lastNameField = view.findViewById(R.id.lastNameField);
        idNumberField = view.findViewById(R.id.idNumberField);
        passwordField = view.findViewById(R.id.passwordField);
        confirmPasswordField = view.findViewById(R.id.confirmPasswordField);
    }
}