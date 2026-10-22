package com.buzzleapyear.trading_api.dto;
//Dto that is used to encapsulate the refresh token in the request body for the refresh endpoint.
public class RefreshTokenRequest {
    private String refreshToken;

    public RefreshTokenRequest() {}

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
