package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.LoginAttemptRepository;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private AuthController authController;

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setPasswordHash("hashed-password");
        loginRequest = new LoginRequest("user@example.com", "wrong-password");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void locksUserOnThirdFailedAttemptAndPersistsLock() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        authController.login(loginRequest);
        authController.login(loginRequest);
        ResponseEntity<?> thirdAttempt = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, thirdAttempt.getStatusCode());
        assertEquals(3, user.getFailedLoginAttempts());
        assertEquals(true, user.isAccountLocked());
        verify(userRepository, org.mockito.Mockito.times(3)).save(user);
    }

    @Test
    void lockedUserCannotLoginWithCorrectPassword() {
        user.setFailedLoginAttempts(3);
        user.setAccountLocked(true);

        ResponseEntity<?> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(
                "Account locked after 3 failed login attempts. Contact an administrator to unlock your account.",
                messageOf(response)
        );
    }

    @Test
    void successfulLoginResetsPersistedFailedAttempts() {
        user.setFailedLoginAttempts(2);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProvider.generateToken("user")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(new LoginRequest("user@example.com", "correct-password"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(false, user.isAccountLocked());
        verify(userRepository).save(user);
    }

    private String messageOf(ResponseEntity<?> response) {
        return ((AuthController.ErrorResponse) response.getBody()).getMessage();
    }
}