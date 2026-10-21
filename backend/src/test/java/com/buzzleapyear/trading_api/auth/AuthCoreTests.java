package com.buzzleapyear.trading_api.auth;

import com.buzzleapyear.trading_api.controller.AuthController;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.LoginAttemptRepository;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.RefreshSessionRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCoreTests {

    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ClientRepository clientRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshSessionRepository refreshSessionRepository;
    @Mock LoginAttemptRepository loginAttemptRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock HttpServletRequest request;
    @InjectMocks AuthController controller;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void registerAndLoginFlowWorks() {
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(controller, "refreshSessionRepository", refreshSessionRepository);
        when(userRepository.findByUsername(any())).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(java.util.Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByRoleName(Role.RoleName.CLIENT))
            .thenReturn(java.util.Optional.of(new Role(Role.RoleName.CLIENT, "Client")));
        when(tokenProvider.generateAccessToken(eq("alice"), eq(0L))).thenReturn("access-token");
        when(request.isSecure()).thenReturn(false);
        when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);

        RegistrationRequest registration = new RegistrationRequest(
                "alice", "alice@example.com", "Alice", "Client", "Password123");
        assertEquals(201, controller.register(registration, request).getStatusCode().value());

        User savedUser = verifyAndCaptureSavedUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(savedUser));
        assertEquals(200, controller.login(
                new LoginRequest("alice@example.com", "Password123"), request).getStatusCode().value());
    }

    @Test
    void passwordIsHashed() {
        String hash = passwordEncoder.encode("Password123");
        assertNotEquals("Password123", hash);
        assertTrue(passwordEncoder.matches("Password123", hash));
    }

    @Test
    void jwtValidatesAndRejectsTamperedToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "0123456789012345678901234567890123456789012345678901234567890123");

        String token = tokenProvider.generateAccessToken("alice", 0);

        assertTrue(tokenProvider.validateToken(token));
        assertFalse(tokenProvider.validateToken(token + "tampered"));
    }

    private User verifyAndCaptureSavedUser() {
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
