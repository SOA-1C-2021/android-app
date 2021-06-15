package com.soa.app.services;

import com.soa.app.models.RegisterResponse;
import com.soa.app.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UNLaMSOAAPIService {

    @POST("api/api/register")
    Call<RegisterResponse> createUser(@Body RegisterRequest registerRequest);

}
