package com.kindhands.app.network;

import com.kindhands.app.model.*;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ===================== AUTH / USER =====================

    @POST("api/auth/register")
    Call<Map<String, String>> registerUser(@Body User user);

    @POST("api/auth/login")
    Call<User> loginUser(@Body User user);

    // ===================== DONOR =====================

    @POST("api/donors/register")
    Call<User> registerDonor(@Body User user);

    @POST("api/donors/login")
    Call<User> loginDonor(@Body User user);

    // ===================== ORGANIZATION =====================

    @Multipart
    @POST("api/organizations/register")
    Call<String> registerOrganization(
            @Part("name") RequestBody name,
            @Part("email") RequestBody email,
            @Part("password") RequestBody password,
            @Part("contact") RequestBody contact,
            @Part("type") RequestBody type,
            @Part("address") RequestBody address,
            @Part("pincode") RequestBody pincode,
            @Part("userId") RequestBody userId,
            @Part MultipartBody.Part document
    );


    @POST("api/organizations/login")
    Call<Organization> loginOrganization(@Body OrganizationLoginRequest loginRequest);

    // ===================== ADMIN =====================

    @GET("api/organizations/admin/pending")
    Call<List<Organization>> getPendingOrganizations();

    @PUT("api/organizations/admin/{id}/approve")
    Call<Void> approveOrg(@Path("id") Long id);

    @PUT("api/organizations/admin/{id}/reject")
    Call<Void> rejectOrg(@Path("id") Long id);

    @GET("api/organizations/admin/document/{id}")
    Call<ResponseBody> viewDoc(@Path("id") Long id);

    // ===================== DONATION REQUESTS =====================

    @GET("requests/open")
    Call<List<DonationRequest>> getOpenRequests();

    @POST("requests/create")
    Call<DonationRequest> createRequest(@Body DonationRequest request);
}
