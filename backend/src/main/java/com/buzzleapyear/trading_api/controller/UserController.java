package com.buzzleapyear.trading_api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.dto.UserResponseDto;
import com.buzzleapyear.trading_api.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/allUsers")
    public List<UserResponseDto> getAllUsers(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getAllUsers();
    }

    @PutMapping ("/{userId}")
    public void assignRole(@PathVariable("userId") int userID, @RequestParam("role") String role){
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
