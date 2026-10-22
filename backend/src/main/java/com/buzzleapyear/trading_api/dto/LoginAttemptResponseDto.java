package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.LoginAttempt;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginAttemptResponseDto {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime attemptedAt;
    private boolean successful;
    private String failureReason;

    public LoginAttemptResponseDto() {
    }

    public LoginAttemptResponseDto(LoginAttempt attempt) {
        this.id = attempt.getId();
        this.username = attempt.getUsername();
        this.email = attempt.getEmail();
        this.attemptedAt = attempt.getAttemptedAt();
        this.successful = attempt.isSuccessful();
        this.failureReason = attempt.getFailureReason();
    }
}