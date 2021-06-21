package com.soa.app.models;

import com.google.gson.annotations.SerializedName;

public class RefreshResponse {

    private boolean success;
    private String token;
    @SerializedName("token_refresh")
    private String tokenRefresh;

    // getters
    public boolean getSuccess() {
        return success;
    }
    public String getToken() {
        return token;
    }
    public String getTokenRefresh() {
        return tokenRefresh;
    }

    // setters
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setTokenRefresh(String tokenRefresh) {
        this.tokenRefresh = tokenRefresh;
    }

}
