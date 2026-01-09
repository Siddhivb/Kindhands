package com.kindhands.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kindhands.app.model.DonationRequest;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;
import com.kindhands.app.utils.SharedPrefManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddDonationActivity extends AppCompatActivity {

    private TextView tvRequirements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_donation);

        tvRequirements = findViewById(R.id.tvOrgRequirements);

        // Find CardViews
        View clothes = findViewById(R.id.cardClothes);
        View food = findViewById(R.id.cardFood);
        View books = findViewById(R.id.cardBooks);
        View medical = findViewById(R.id.cardMedical);
        View toys = findViewById(R.id.cardToys);
        View stationery = findViewById(R.id.cardStationery);

        // Set Click Listeners
        if (clothes != null) clothes.setOnClickListener(v -> openForm("clothes"));
        if (food != null) food.setOnClickListener(v -> openForm("food"));
        if (books != null) books.setOnClickListener(v -> openForm("books"));
        if (medical != null) medical.setOnClickListener(v -> openForm("medical"));
        if (toys != null) toys.setOnClickListener(v -> openForm("toys"));
        if (stationery != null) stationery.setOnClickListener(v -> openForm("stationery"));

        // Add Logout Button Logic
        Button btnLogout = findViewById(R.id.btnLogout); 
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPrefManager.getInstance(this).logout();
                Intent intent = new Intent(AddDonationActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
        
        // Fetch NGO Requirements
        fetchRequirements();
    }

    private void fetchRequirements() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<DonationRequest>> call = apiService.getOpenRequests();
        
        call.enqueue(new Callback<List<DonationRequest>>() {
            @Override
            public void onResponse(Call<List<DonationRequest>> call, Response<List<DonationRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StringBuilder reqText = new StringBuilder();
                    boolean found = false;
                    
                    for (DonationRequest req : response.body()) {
                        if ("REQUIREMENT".equalsIgnoreCase(req.getCategory())) {
                            found = true;
                            String orgName = req.getOtherDetails() != null ? req.getOtherDetails() : "Organization";
                            reqText.append("• ").append(orgName).append(": ").append(req.getDetails()).append("\n
");
                        }
                    }
                    
                    if (found && tvRequirements != null) {
                        tvRequirements.setText(reqText.toString());
                    } else if (tvRequirements != null) {
                        tvRequirements.setText("No current needs from organizations.");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<DonationRequest>> call, Throwable t) {
                 if (tvRequirements != null) {
                        tvRequirements.setText("Could not load organization needs.");
                 }
            }
        });
    }

    private void openForm(String category) {
        Intent intent = new Intent(this, DonationDetailsActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }
}
