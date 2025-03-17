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

public class StudentDetailsFragment extends Fragment {
    private EditText courseYearField, dobField, homeAddressField, cityAddressField, contactNumberField;

    public StudentDetailsFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_student_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);

        dobField.setOnClickListener(v -> showDatePicker());

        contactNumberField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && TextUtils.isEmpty(contactNumberField.getText().toString())) {
                contactNumberField.setText("+63");
                contactNumberField.setSelection(contactNumberField.getText().length());
            }
        });
    }

    private void initializeViews(View view) {
        courseYearField = view.findViewById(R.id.courseYearField);
        dobField = view.findViewById(R.id.dobPicker);
        homeAddressField = view.findViewById(R.id.homeAddressField);
        cityAddressField = view.findViewById(R.id.cityAddressField);
        contactNumberField = view.findViewById(R.id.contactNumberField);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year1, month1, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            dobField.setText(date);
        }, year, month, day);

        datePickerDialog.show();
    }
}