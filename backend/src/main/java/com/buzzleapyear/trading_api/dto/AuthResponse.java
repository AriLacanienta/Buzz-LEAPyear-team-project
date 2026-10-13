package com.buzzleapyear.trading_api.dto;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String accessToken;
    private String type = "Bearer";
    private String username;
    private String email;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, String username, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.token = accessToken;
        this.username = username;
        this.email = email;
    }
    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
