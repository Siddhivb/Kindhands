package com.kindhands.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kindhands.app.model.Organization;
import com.kindhands.app.model.OrganizationLoginRequest;
import com.kindhands.app.model.User;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;
import com.kindhands.app.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_DEBUG";
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            navigateToDashboard();
            return;
        }

        setContentView(R.layout.login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvGoToRegister);

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordPhoneActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("admin@kindhands.com".equalsIgnoreCase(email) && "admin123".equals(password)) {
                SharedPrefManager.getInstance(this).saveUser("Admin", email, "ADMIN");
                navigateToDashboard();
                return;
            }

            // TRY DONOR LOGIN
            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            User loginUser = new User(email, password);
            Call<User> callDonor = apiService.loginDonor(loginUser);

            callDonor.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        SharedPrefManager.getInstance(LoginActivity.this).saveUser(user.getName(), user.getEmail(), "DONOR");
                        navigateToDashboard();
                    } else {
                        // IF DONOR LOGIN FAILS, TRY ORGANIZATION LOGIN
                        tryOrganizationLogin(email, password);
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.e(TAG, "Donor Login Network Failure: " + t.getMessage());
                    tryOrganizationLogin(email, password);
                }
            });
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
        });
    }

    private void tryOrganizationLogin(String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        OrganizationLoginRequest loginRequest = new OrganizationLoginRequest(email, password);
        Call<Organization> callOrg = apiService.loginOrganization(loginRequest);

        callOrg.enqueue(new Callback<Organization>() {
            @Override
            public void onResponse(Call<Organization> call, Response<Organization> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Organization org = response.body();
                    SharedPrefManager.getInstance(LoginActivity.this).saveUser(org.getName(), org.getEmail(), "ORGANIZATION");
                    navigateToDashboard();
                } else {
                    Toast.makeText(LoginActivity.this, "Login Failed: Invalid Credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Organization> call, Throwable t) {
                Log.e(TAG, "Org Login Network Failure: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToDashboard() {
        String type = SharedPrefManager.getInstance(this).getUserType();
        Intent intent;
        if ("ORGANIZATION".equals(type)) {
            intent = new Intent(this, OrganizationDashboardActivity.class);
        } else if ("ADMIN".equals(type)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, AddDonationActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
