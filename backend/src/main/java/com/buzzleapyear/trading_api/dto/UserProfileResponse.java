package com.buzzleapyear.trading_api.dto;

public class UserProfileResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public UserProfileResponse(String username, String email, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}