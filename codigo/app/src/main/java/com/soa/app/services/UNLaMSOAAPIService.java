package com.soa.app.services;

import com.soa.app.models.LoginRequest;
import com.soa.app.models.LoginResponse;
import com.soa.app.models.RefreshResponse;
import com.soa.app.models.RegisterResponse;
import com.soa.app.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface UNLaMSOAAPIService {

    @POST("api/api/register")
    Call<RegisterResponse> register(@Body RegisterRequest registerRequest);

    @POST("api/api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @PUT("api/api/refresh")
    Call<RefreshResponse> refresh();

}
