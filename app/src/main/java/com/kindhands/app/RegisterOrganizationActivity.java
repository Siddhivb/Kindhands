package com.kindhands.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.kindhands.app.model.Organization;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterOrganizationActivity extends AppCompatActivity {

    private EditText etName, etEmail, etContact, etAddress, etPincode, etDocument, etPassword;
    private Spinner spinnerOrgType;
    private Button btnRegister, btnUploadDocument;
    private TextView tvSelectedFileName;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_organization);

        // Initialize Views
        etName = findViewById(R.id.etOrgName);
        etEmail = findViewById(R.id.etOrgEmail);
        etContact = findViewById(R.id.etOrgContact);
        etAddress = findViewById(R.id.etOrgAddress);
        etPincode = findViewById(R.id.etOrgPincode);
        etDocument = findViewById(R.id.etOrgDocument);
        etPassword = findViewById(R.id.etOrgPassword);
        spinnerOrgType = findViewById(R.id.spinnerOrgType);
        btnRegister = findViewById(R.id.btnOrgRegister);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName);

        // Setup Spinner
        String[] orgTypes = {"Orphanage", "Old Age Home"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, orgTypes);
        spinnerOrgType.setAdapter(adapter);

        // File Picker Setup
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String path = uri.getPath();
                            String fileName = path != null ? path.substring(path.lastIndexOf("/") + 1) : "Unknown File";
                            
                            tvSelectedFileName.setText(fileName);
                            etDocument.setText(uri.toString());
                        }
                    }
                }
        );

        btnUploadDocument.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Allow all file types
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Certificate"));
        });

        btnRegister.setOnClickListener(v -> registerOrganization());
    }

    private void registerOrganization() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String pincode = etPincode.getText().toString().trim();
        String document = etDocument.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        String rawType = spinnerOrgType.getSelectedItem().toString();
        String type = "Orphanage".equals(rawType) ? "ORPHANAGE" : "OLD_AGE_HOME";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Organization org = new Organization(name, email, password, contact, type, address, pincode, document);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<Organization> call = apiService.registerOrganization(org);

        call.enqueue(new Callback<Organization>() {
            @Override
            public void onResponse(Call<Organization> call, Response<Organization> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterOrganizationActivity.this, "Organization Registered! Status: PENDING", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // SHOW DETAILED ERROR FROM BACKEND
                    String errorBody = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e("API_ERROR", "Error parsing error body", e);
                    }
                    Toast.makeText(RegisterOrganizationActivity.this, "Registration Failed: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    Log.e("API_ERROR", "Response Code: " + response.code() + " Body: " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<Organization> call, Throwable t) {
                // SHOW NETWORK FAILURE
                Toast.makeText(RegisterOrganizationActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API_FAILURE", t.getMessage(), t);
            }
        });
    }
}
