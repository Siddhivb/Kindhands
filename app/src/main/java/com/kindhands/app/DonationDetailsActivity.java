package com.kindhands.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.kindhands.app.model.DonationRequest;
import com.kindhands.app.network.ApiService;
import com.kindhands.app.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonationDetailsActivity extends AppCompatActivity {

    private String category;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_details);

        category = getIntent().getStringExtra("category");
        container = findViewById(R.id.formContainer);

        if (category == null) {
            Toast.makeText(this, "Error: Category not specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFormForCategory();
    }

    private void loadFormForCategory() {
        int layoutId = getLayoutIdForCategory(category);
        if (layoutId == -1) {
            Toast.makeText(this, "Error: Invalid donation category.", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        // The final reference to formView is the key to making this reliable.
        final View formView = inflater.inflate(layoutId, container, false);

        // --- Set up all views and listeners for this specific form ---

        // 1. Setup Form-Specific Spinners or Checkboxes
        setupSpecificFormElements(formView);

        // 2. Setup Common Spinners (Organization Type)
        setupCommonSpinners(formView);

        // 3. Find the button and set its listener with all logic inside
        Button btnSubmit = formView.findViewById(R.id.btnSubmitDonation);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                // The entire submission process is now self-contained here,
                // using the reliable 'formView' reference.
                
                Toast.makeText(DonationDetailsActivity.this, "Submitting...", Toast.LENGTH_SHORT).show();

                String details;
                String otherDetails = "";
                int quantity;

                EditText etQuantity = formView.findViewById(R.id.etQuantity);
                try {
                    quantity = Integer.parseInt(etQuantity.getText().toString());
                    if (quantity <= 0) {
                        etQuantity.setError("Quantity must be positive.");
                        etQuantity.requestFocus();
                        return;
                    }
                } catch (Exception e) {
                    etQuantity.setError("Please enter a valid quantity.");
                    etQuantity.requestFocus();
                    return;
                }

                details = getDonationDetails(formView);

                Spinner orgTypeSpinnerInternal = formView.findViewById(R.id.spinnerOrgType);
                EditText etOrgName = formView.findViewById(R.id.etOrgName);
                String orgType = orgTypeSpinnerInternal.getSelectedItem().toString();
                String orgName = etOrgName.getText().toString().trim();

                if (!orgName.isEmpty()) {
                    otherDetails = "For: " + orgName + " (" + orgType + ")";
                }

                DonationRequest request = new DonationRequest(category, details, quantity, otherDetails);

                ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                Call<DonationRequest> call = apiService.createRequest(request);

                call.enqueue(new Callback<DonationRequest>() {
                    @Override
                    public void onResponse(Call<DonationRequest> call, Response<DonationRequest> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(DonationDetailsActivity.this, "Donation Submitted Successfully!", Toast.LENGTH_LONG).show();
                            finish(); // This returns to the donation dashboard
                        } else {
                            Toast.makeText(DonationDetailsActivity.this, "Submission Failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<DonationRequest> call, Throwable t) {
                        Toast.makeText(DonationDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("API_ERROR", "Donation submission failed", t);
                    }
                });
            });
        } else {
            Log.e("FormSetup", "CRITICAL: Submit button not found in layout for " + category);
        }

        // Finally, add the fully prepared view to the screen
        container.removeAllViews();
        container.addView(formView);
    }

    private int getLayoutIdForCategory(String category) {
        switch (category) {
            case "clothes": return R.layout.form_clothes;
            case "books": return R.layout.form_books;
            case "food": return R.layout.form_food;
            case "toys": return R.layout.form_toys;
            case "medical": return R.layout.form_medical;
            case "stationery": return R.layout.form_stationery;
            default: return -1;
        }
    }
    
    private String getDonationDetails(View formView) {
        switch (category) {
            case "food":
                return "Food Type: " + ((EditText) formView.findViewById(R.id.etFoodType)).getText().toString() + ", Expires: " + ((EditText) formView.findViewById(R.id.etExpiryDate)).getText().toString();
            case "clothes":
                return "Type: " + ((Spinner) formView.findViewById(R.id.spinnerClothingType)).getSelectedItem().toString();
            case "books":
                return "Book: " + ((EditText) formView.findViewById(R.id.etBookName)).getText().toString();
            case "medical":
                return "Kit Type: " + ((EditText) formView.findViewById(R.id.etKitType)).getText().toString();
            case "stationery":
                return "Item: " + ((EditText) formView.findViewById(R.id.etItemName)).getText().toString();
            case "toys":
                return "Toy: " + ((EditText) formView.findViewById(R.id.etToyName)).getText().toString();
            default:
                return "Unknown donation item";
        }
    }

    private void setupSpecificFormElements(View formView) {
        if (category.equals("clothes")) {
            Spinner spinner = formView.findViewById(R.id.spinnerClothingType);
            if (spinner != null) {
                String[] clothingTypes = {"Shirt", "Pants", "Saree", "Jacket", "Other"};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clothingTypes);
                spinner.setAdapter(adapter);
            }
        } else if (category.equals("books")) {
            CheckBox cbOtherBooks = formView.findViewById(R.id.cbOtherBooks);
            EditText etOtherBooks = formView.findViewById(R.id.etOtherBooks);
            if (cbOtherBooks != null && etOtherBooks != null) {
                cbOtherBooks.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    etOtherBooks.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                });
            }
        }
    }

    private void setupCommonSpinners(View formView) {
        Spinner orgTypeSpinner = formView.findViewById(R.id.spinnerOrgType);
        if (orgTypeSpinner != null) {
            String[] orgTypes = {"Orphanage", "Old Age Home"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, orgTypes);
            orgTypeSpinner.setAdapter(adapter);
        }
    }
}
