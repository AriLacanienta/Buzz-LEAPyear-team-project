package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.AuthResponse;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RefreshTokenRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

    private UserRepository userRepository;

    private ClientRepository clientRepository;

    private AccountRepository accountRepository;

    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;

    public AuthController(
        AccountRepository accountRepository,
        ClientRepository clientRepository,
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider
    ) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Finding user by email in database
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

            // Checking if user exists
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }

            User user = userOptional.get();

            // Verifying password
            // passwordEncoder.matches() takes raw password and compares with hashed password from DB
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid email or password"));
            }

            // Generate short-lived access token and long-lived refresh token
            String accessToken = tokenProvider.generateAccessToken(user.getUsername());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            // Returning token in response
            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getEmail()));

        } catch (Exception e) {
            // If any unexpected error occurs, return 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed: " + e.getMessage()));
        }
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
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest) {
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
            account.setAccountName(savedUser.getUsername());
            account.setCashAvailable(new BigDecimal("10000.00"));
            accountRepository.save(account);

            // Generating JWT token
            // Generate short-lived access token and long-lived refresh token
            // User is automatically logged in after registration
            String accessToken = tokenProvider.generateAccessToken(savedUser.getUsername());
            String refreshToken = tokenProvider.generateRefreshToken(savedUser.getUsername());

            // Returning token with 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(accessToken, refreshToken, savedUser.getUsername(), savedUser.getEmail()));

        } catch (Exception e) {
            // If any error occurs during registration, return 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    //new refresh endpoint
    //POST /api/auth/refresh
    //Exchange a valid refresh token for a new access token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            if (refreshTokenRequest == null || refreshTokenRequest.getRefreshToken() == null
                    || refreshTokenRequest.getRefreshToken().isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Refresh token is required"));
            }

            String refreshToken = refreshTokenRequest.getRefreshToken();
            if (!tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Refresh token is invalid or expired"));
            }

            String username = tokenProvider.getUsernameFromJWT(refreshToken);
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("User not found for refresh token"));
            }

            User user = userOptional.get();
            String newAccessToken = tokenProvider.generateAccessToken(user.getUsername());
            String newRefreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            return ResponseEntity.ok(
                    new AuthResponse(newAccessToken, newRefreshToken, user.getUsername(), user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Refresh failed: " + e.getMessage()));
        }
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
