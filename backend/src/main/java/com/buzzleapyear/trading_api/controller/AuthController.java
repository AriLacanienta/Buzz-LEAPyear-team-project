package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.AuthResponseDto;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * This Authentication Controller handles login, registration, and token validation
 Endpoints:
 * POST /api/auth/login - Login with email/password, get JWT token
 * POST /api/auth/register - Register new user, get JWT token
 * GET /api/auth/validate - Validate JWT token
 */
@RestController
@RequestMapping("/api/v1/auth")
//how long the browser should cache the preflight response (seconds)
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;
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

            // Generating JWT token
            // The token contains the username and will expire in 24 hours
            String token = tokenProvider.generateToken(user.getUsername());

            // Returning token in response
            return ResponseEntity.ok(new AuthResponseDto(token, user.getUsername(), user.getEmail()));

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

            // Saving user to ourdatabase
            User savedUser = userRepository.save(newUser);

            // Generating JWT token
            // User is automatically logged in after registration
            String token = tokenProvider.generateToken(savedUser.getUsername());

            // Returning token with 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponseDto(token, savedUser.getUsername(), savedUser.getEmail()));

        } catch (Exception e) {
            // If any error occurs during registration, return 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
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
            String token = bearerToken.substring(7);
            
            // token validation
            if (tokenProvider.validateToken(token)) {
                // If valid, extract username from token
                String username = tokenProvider.getUsernameFromJWT(token);
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