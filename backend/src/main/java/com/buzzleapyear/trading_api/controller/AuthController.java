package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.AuthResponse;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.entity.RefreshSession;
import com.buzzleapyear.trading_api.repository.RefreshSessionRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import com.buzzleapyear.trading_api.repository.LoginAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.LoginAttempt;

import java.math.BigDecimal;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This Authentication Controller handles login, registration, and token validation
 Endpoints:
 * POST /api/auth/login - Login with email/password, get JWT token
 * POST /api/auth/register - Register new user, get JWT token
 * POST /api/auth/refresh - Exchange refresh token for new tokens
 * GET /api/auth/validate - Validate JWT token
 */
@RestController
@RequestMapping("/api/v1/auth")
//how long the browser should cache the preflight response (seconds)
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    private ClientRepository clientRepository;

    private AccountRepository accountRepository;

    private RoleRepository roleRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;

    private LoginAttemptRepository loginAttemptRepository;

    public AuthController(
        AccountRepository accountRepository,
        ClientRepository clientRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider,
        LoginAttemptRepository loginAttemptRepository
    ) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptRepository = loginAttemptRepository;
    }
/**
     * POST /api/auth/login
     *
     * Login endpoint - Authenticate user with email and password
     *
     * Request body:
     * {
     *   "email": "john@example.com",
     *   "password": "pass123"
     * }
     *
     * Response (200 OK):
     * {
     *   "token": "eyJ...",
     *   "type": "Bearer",
     *   "username": "john",
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // Finding user by email in database
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

            // Checking if user exists
            if (userOptional.isEmpty()) {
                recordLoginAttempt(null, null, loginRequest.getEmail(), false, "USER_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }

            User user = userOptional.get();

            if (user.isAccountLocked()) {
                recordLoginAttempt(user, false, "ACCOUNT_LOCKED");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Account locked after 3 failed login attempts. Contact an administrator to unlock your account."));
            }

            // Verifying password
            // passwordEncoder.matches() takes raw password and compares with hashed password from DB
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 3) {
                    user.setAccountLocked(true);
                }
                userRepository.save(user);

                if (user.isAccountLocked()) {
                    recordLoginAttempt(user, false, "ACCOUNT_LOCKED_AFTER_THREE_FAILURES");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorResponse("Account locked after 3 failed login attempts. Contact an administrator to unlock your account."));
                }
                recordLoginAttempt(user, false, "INVALID_PASSWORD");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }

            // Generate short-lived access token and long-lived refresh token
                String accessToken = tokenProvider.generateAccessToken(
                    user.getUsername(), user.getTokenVersion());
            String refreshToken = createRefreshSession(user.getUsername());
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }

            recordLoginAttempt(user, true, null);

                //returning access token in body and refresh token in secure HttpOnly cookie
                return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, request).toString())
                    .body(createAuthResponse(accessToken, user));

        } catch (Exception e) {
            // If any unexpected error occurs, return 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        }
    }

    private void recordLoginAttempt(User user, boolean successful, String failureReason) {
        recordLoginAttempt(user == null ? null : user.getId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getEmail(), successful, failureReason);
    }

    private void recordLoginAttempt(Long userId, String username, String email, boolean successful,
                                    String failureReason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setUsername(username);
        attempt.setEmail(email);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setSuccessful(successful);
        attempt.setFailureReason(failureReason);
        loginAttemptRepository.save(attempt);
    }

    /**
     * POST /api/auth/register
     *
     * Registration endpoint - Create new user account
     *
     * Request body:
     * {
     *   "username": "johndoe",
     *   "email": "john@example.com",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "password": "pass123"
     * }
     *
     * Response (201 Created):
     * {
     *   "token": "eyJ...",
     *   "type": "Bearer",
     *   "username": "johndoe",
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest, HttpServletRequest request) {
        try {
            //REGISTERING
            // Check if username already exists
            if (userRepository.findByUsername(registrationRequest.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Username already exists"));
            }

            // Checking if email already exists
            if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Email already exists"));
            }

            // Creating new User entity
            User newUser = new User();
            newUser.setUsername(registrationRequest.getUsername());
            newUser.setEmail(registrationRequest.getEmail());
            newUser.setFirstName(registrationRequest.getFirstName());
            newUser.setLastName(registrationRequest.getLastName());

            // IMPORTANT!!!!!!! we hash the password before it is saved to our database
            // We are not storing raw passwords in our database. We are using passwordEncoder.encode()
            newUser.setPasswordHash(passwordEncoder.encode(registrationRequest.getPassword()));

            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

                Role clientRole = roleRepository.findByRoleName(Role.RoleName.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("CLIENT role is not configured"));
                newUser.getUserRoles().add(new UserRole(newUser, clientRole, "registration"));

            // Saving user to ourdatabase
            User savedUser = userRepository.save(newUser);

            // Create a client and an account
            Client client = new Client();
            client.setUser(savedUser);
            clientRepository.save(client);

            Account account = new Account();
            account.setClient(client);
            account.setAccountName(savedUser.getFirstName());
            account.setCashAvailable(new BigDecimal("10000.00"));
            accountRepository.save(account);

            // Generating JWT token
            // Generate short-lived access token and long-lived refresh token
            // User is automatically logged in after registration
                String accessToken = tokenProvider.generateAccessToken(
                    savedUser.getUsername(), savedUser.getTokenVersion());
            String refreshToken = createRefreshSession(savedUser.getUsername());

                //returning access token in body and refresh token in secure HttpOnly cookie
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, request).toString())
                    .body(createAuthResponse(accessToken, savedUser));

        } catch (Exception e) {
            // If any error occurs during registration, return 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    //new refresh endpoint
    //POST /api/auth/refresh
    //Exchange a valid refresh cookie for a new access token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                          HttpServletRequest request) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Refresh token is required"));
            }

                Optional<RefreshSession> sessionOptional = refreshSessionRepository
                    .findByTokenHash(hashToken(refreshToken));
                if (sessionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Refresh token is invalid or revoked"));
                }

                RefreshSession session = sessionOptional.get();
                if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Refresh token is expired or revoked"));
                }

            String username = session.getUsername();
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("User not found for refresh token"));
            }

            User user = userOptional.get();
            session.setRevokedAt(LocalDateTime.now());
            refreshSessionRepository.save(session);
            String newAccessToken = tokenProvider.generateAccessToken(
                    user.getUsername(), user.getTokenVersion());
            String newRefreshToken = createRefreshSession(user.getUsername());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(newRefreshToken, request).toString())
                    .body(createAuthResponse(newAccessToken, user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Refresh failed: " + e.getMessage()));
        }
    }

    private AuthResponse createAuthResponse(String accessToken, User user) {
        return new AuthResponse(accessToken, user.getUsername(), user.getEmail(),
                user.getUserRoles().stream()
                        .map(userRole -> userRole.getRole().getRoleName().name())
                        .toList());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring("Bearer ".length());
            if (tokenProvider.validateToken(accessToken)) {
                userRepository.findByUsername(tokenProvider.getUsernameFromJWT(accessToken))
                        .ifPresent(user -> {
                            user.revokeAccessTokens();
                            userRepository.save(user);
                        });
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshSessionRepository.findByTokenHash(hashToken(refreshToken)).ifPresent(session -> {
                session.setRevokedAt(LocalDateTime.now());
                refreshSessionRepository.save(session);
            });
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie(request).toString())
                .body(new ValidationResponse(true, "Logged out"));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, HttpServletRequest request) {
        boolean secure = isSecureRequest(request);
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
                .build();
    }

    private String createRefreshSession(String username) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshSession session = new RefreshSession();
        session.setTokenHash(hashToken(rawToken));
        session.setUsername(username);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_MAX_AGE_SECONDS));
        refreshSessionRepository.save(session);
        return rawToken;
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token", exception);
        }
    }

    private ResponseCookie clearRefreshCookie(HttpServletRequest request) {
        boolean secure = isSecureRequest(request);
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

    /**
     * GET /api/auth/validate
     *
     * Token validation endpoint Check if JWT token is valid
     *
     * Request header:
     * Authorization: Bearer eyJ...
     *
     * Response (200 OK):
     * {
     *   "valid": true,
     *   "data": "john"
     * }
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String bearerToken) {
        try {
            // Checking if Authorization header exists and has correct format
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ValidationResponse(false, "Invalid token format"));
            }

            //Bearer tells the server the token that follows is a JWT token used for authentication
            String accessToken = bearerToken.substring(7);
            
            // token validation
            if (tokenProvider.validateToken(accessToken)) {
                // If valid, extract username from token
                String username = tokenProvider.getUsernameFromJWT(accessToken);
                return ResponseEntity.ok(new ValidationResponse(true, username));
            } else {
                // If invalid (expired, signature mismatch, etc)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ValidationResponse(false, "Token is invalid or expired"));
            }
        } catch (Exception e) {
            // If any error occurs, return 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ValidationResponse(false, "Token validation failed"));
        }
    }
    //returning error messages
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // ValidationResponse is used for token validation responses
    public static class ValidationResponse {
        private boolean valid;
        private String data;

        public ValidationResponse(boolean valid, String data) {
            this.valid = valid;
            this.data = data;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
