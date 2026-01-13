package com.kindhands.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kindhands.app.model.Organization;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;
import com.kindhands.app.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PendingOrgsAdapter adapter;
    List<Organization> list = new ArrayList<>();
    ApiService apiService;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        recyclerView = findViewById(R.id.rvPendingOrgs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnLogout = findViewById(R.id.btnAdminLogout);
        btnLogout.setOnClickListener(v -> {
            SharedPrefManager.getInstance(this).logout();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);

        loadPending();
    }

    private void loadPending() {
        apiService.getPendingOrganizations().enqueue(new Callback<List<Organization>>() {
            @Override
            public void onResponse(Call<List<Organization>> call, Response<List<Organization>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    list = response.body();
                    adapter = new PendingOrgsAdapter(list);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            "No pending requests",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Organization>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= ADAPTER =================
    class PendingOrgsAdapter extends RecyclerView.Adapter<PendingOrgsAdapter.VH> {

        List<Organization> orgs;

        PendingOrgsAdapter(List<Organization> orgs) {
            this.orgs = orgs;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_organization, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Organization o = orgs.get(pos);

            h.tvName.setText(o.getName());
            h.tvDetails.setText(o.getEmail() + " | " + o.getContact());

            h.btnView.setOnClickListener(v -> {
                String baseUrl = RetrofitClient.getClient().baseUrl().toString();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String documentUrl = baseUrl + "/api/organizations/admin/document/" + o.getId();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(documentUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });

            h.btnApprove.setOnClickListener(v -> updateStatus(o.getId(), true));
            h.btnReject.setOnClickListener(v -> updateStatus(o.getId(), false));
        }

        private void updateStatus(Long id, boolean approve) {
            Call<Void> call = approve
                    ? apiService.approveOrg(id)
                    : apiService.rejectOrg(id);

            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminDashboardActivity.this,
                                approve ? "Approved" : "Rejected",
                                Toast.LENGTH_SHORT).show();
                        loadPending();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this,
                                "Update failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Error: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return orgs.size();
        }

        class VH extends RecyclerView.ViewHolder {

            TextView tvName, tvDetails;
            Button btnView, btnApprove, btnReject;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvOrgName);
                tvDetails = v.findViewById(R.id.tvOrgDetails);
                btnView = v.findViewById(R.id.btnViewCertificate);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnReject = v.findViewById(R.id.btnReject);
            }
        }
    }
}
