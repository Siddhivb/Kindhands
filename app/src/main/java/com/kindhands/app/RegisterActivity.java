package com.kindhands.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kindhands.app.model.User;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etAddress, etPincode, etPassword, etConfirmPassword;
    private Spinner spinnerGender;
    private Button btnRegister;
    private TextView tvGoToLogin, tvGoToOrgRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        // Initialize Views from the XML layout
        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPhone = findViewById(R.id.etRegisterPhone);
        etAddress = findViewById(R.id.etRegisterAddress);
        etPincode = findViewById(R.id.etRegisterPincode);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToOrgRegister = findViewById(R.id.tvGoToOrgRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Setup Spinner for Gender selection
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGender.setAdapter(adapter);

        // Set listener to navigate to Organization Registration screen
        tvGoToOrgRegister.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, RegisterOrganizationActivity.class);
            startActivity(intent);
        });

        // Set listener for the main Register Button
        btnRegister.setOnClickListener(v -> {
            // First, validate all user inputs
            if (validateInputs()) {
                // If validation passes, create a User object
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String mobile = etPhone.getText().toString().trim(); // Use "mobile" to match backend entity
                String address = etAddress.getText().toString().trim();
                String pincode = etPincode.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String gender = spinnerGender.getSelectedItem().toString();

                // Create the User object to send to the backend.
                // The role will be set to "DONOR" by the AuthController on the backend.
                // We now pass "DONOR" as the 8th argument to match the constructor in User.java
                User newUser = new User(name, email, password, mobile, address, pincode, gender, "DONOR");

                // ================== API CALL ==================
                // Get the ApiService instance from RetrofitClient
                ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

                // Use the correct `registerUser` method which points to "/api/auth/register"
                Call<Object> call = apiService.registerUser(newUser);

                // Execute the network call asynchronously
                call.enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        // Check if the HTTP response is successful (e.g., 200 OK)
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                            // Navigate to the Login screen, clearing the back stack
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish(); // Finish this activity
                        } else {
                            // Handle errors from the backend (e.g., 400 Bad Request if email exists)
                            String errorMessage = "Registration Failed";
                            try {
                                if (response.errorBody() != null) {
                                    // Read the error message string from the response
                                    errorMessage = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                Log.e("REGISTER_ERROR", "Error parsing error body", e);
                            }
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        // This block handles network failures (e.g., no internet, wrong server IP)
                        Toast.makeText(RegisterActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("REGISTER_ERROR", "Network request failed", t);
                    }
                });
            }
        });

        // Set listener to go back to the Login screen
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    /**
     * Validates all the input fields on the registration form.
     * @return true if all inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        if (phone.length() < 10) {
            etPhone.setError("Please enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        // All validations passed
        return true;
    }
}
