package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.LoginAttemptResponseDto;
import com.buzzleapyear.trading_api.entity.LoginAttempt;
import com.buzzleapyear.trading_api.repository.LoginAttemptRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//manager dashboard endpoint for reviewing authentication activity
//GET /api/v1/admin/login-attempts
//GET /api/v1/admin/login-attempts?email=user@example.com
@RestController
@RequestMapping("/api/v1/admin/login-attempts")
public class LoginAttemptController {

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptController(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    //returns login attempts newest first. Access is restricted to MANAGER by
    //the /api/v1/admin/** security rule in SecurityConfig
    @GetMapping
    public List<LoginAttemptResponseDto> getLoginAttempts(
            @RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            return toResponseDtos(loginAttemptRepository.findAllByOrderByAttemptedAtDesc());
        }
        return toResponseDtos(loginAttemptRepository.findByEmailIgnoreCaseOrderByAttemptedAtDesc(email.trim()));
    }

    private List<LoginAttemptResponseDto> toResponseDtos(List<LoginAttempt> attempts) {
        return attempts.stream()
                .map(LoginAttemptResponseDto::new)
                .toList();
    }
}