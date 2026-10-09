package com.buzzleapyear.trading_api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * UserResponseDto
 */
@Getter 
@Setter 
public class UserResponseDto {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;

    public UserResponseDto(){}

    public UserResponseDto(
            String username, 
            String firstName, 
            String lastName, 
            String email, 
            List<String> roles
        ) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
    }

}
