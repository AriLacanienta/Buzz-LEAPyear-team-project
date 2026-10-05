package com.buzzleapyear.trading_api.dto;

/**
 * LoginRequest DTO
 * Used when user submits login credentials
 * 
 * Expected JSON:
 * {
 *   "email": "john@example.com",
 *   "password": "pass123"
 * }
 */
public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
