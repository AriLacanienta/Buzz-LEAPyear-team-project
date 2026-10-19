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

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;

    public UserResponseDto(){}

    public UserResponseDto(
            Long id,
            String username,
            String firstName,
            String lastName,
            String email,
            List<String> roles
        ) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
    }

    public UserResponseDto(
            String username,
            String firstName,
            String lastName,
            String email,
            List<String> roles
    ) {
        this(null, username, firstName, lastName, email, roles);
    }

}
