package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.AuthResponse;
import com.buzzleapyear.trading_api.dto.LoginRequest;
import com.buzzleapyear.trading_api.dto.RegistrationRequest;
<<<<<<< HEAD
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
=======
import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
>>>>>>> feature/kafka
import com.buzzleapyear.trading_api.repository.AccountRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

<<<<<<< HEAD
=======
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * This Authentication Controller handles login, registration, and token validation
 Endpoints:
 * POST /api/auth/login - Login with email/password, get JWT token
 * POST /api/auth/register - Register new user, get JWT token
 * GET /api/auth/validate - Validate JWT token
 */
>>>>>>> feature/kafka
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

<<<<<<< HEAD
    public AuthController(
        UserRepository userRepository,
        ClientRepository clientRepository,
        AccountRepository accountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

=======
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
>>>>>>> feature/kafka
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
<<<<<<< HEAD
=======

            // Checking if user exists
>>>>>>> feature/kafka
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
            }

            User user = userOptional.get();
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
            }

            String token = tokenProvider.generateToken(user.getUsername());
<<<<<<< HEAD
            return ResponseEntity.ok(new AuthResponse(
                token, user.getUsername(), user.getEmail(), getRoleNames(user)));
        } catch (Exception exception) {
=======

            // Returning token in response
                return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    getRoleNames(user)
                ));

        } catch (Exception e) {
            // If any unexpected error occurs, return 401 Unauthorized
>>>>>>> feature/kafka
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Authentication failed: " + exception.getMessage()));
        }
    }

<<<<<<< HEAD
=======
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
>>>>>>> feature/kafka
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest) {
        try {
            if (userRepository.findByUsername(registrationRequest.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username already exists"));
            }
            if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Email already exists"));
            }

            User newUser = new User();
            newUser.setUsername(registrationRequest.getUsername());
            newUser.setEmail(registrationRequest.getEmail());
            newUser.setFirstName(registrationRequest.getFirstName());
            newUser.setLastName(registrationRequest.getLastName());
            newUser.setPasswordHash(passwordEncoder.encode(registrationRequest.getPassword()));
<<<<<<< HEAD
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            Role clientRole = roleRepository.findByRoleName(Role.RoleName.CLIENT)
                .orElseThrow(() -> new IllegalStateException("CLIENT role is not configured"));
            newUser.getUserRoles().add(new UserRole(newUser, clientRole, "registration"));
=======

            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

                Role clientRole = roleRepository.findByRoleName(Role.RoleName.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("CLIENT role is not configured"));
                newUser.getUserRoles().add(new UserRole(newUser, clientRole, "registration"));

            // Saving user to ourdatabase
>>>>>>> feature/kafka
            User savedUser = userRepository.save(newUser);

            Client client = new Client();
            client.setUser(savedUser);
            Client savedClient = clientRepository.save(client);

            Account account = new Account();
            account.setClient(savedClient);
            account.setAccountName(savedUser.getUsername());
            account.setCashAvailable(BigDecimal.ZERO);
            accountRepository.save(account);

            String token = tokenProvider.generateToken(savedUser.getUsername());
<<<<<<< HEAD
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                token, savedUser.getUsername(), savedUser.getEmail(), getRoleNames(savedUser)));
        } catch (Exception exception) {
=======

            // Returning token with 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(
                        token,
                        savedUser.getUsername(),
                        savedUser.getEmail(),
                        getRoleNames(savedUser)
                    ));

        } catch (Exception e) {
            // If any error occurs during registration, return 400 Bad Request
>>>>>>> feature/kafka
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Registration failed: " + exception.getMessage()));
        }
    }

<<<<<<< HEAD
=======
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
>>>>>>> feature/kafka
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String bearerToken) {
        try {
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ValidationResponse(false, "Invalid token format"));
            }

            String token = bearerToken.substring(7);
<<<<<<< HEAD
=======

            // token validation
>>>>>>> feature/kafka
            if (tokenProvider.validateToken(token)) {
                return ResponseEntity.ok(new ValidationResponse(
                    true, tokenProvider.getUsernameFromJWT(token)));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ValidationResponse(false, "Token is invalid or expired"));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ValidationResponse(false, "Token validation failed"));
        }
    }

    private List<String> getRoleNames(User user) {
        return user.getUserRoles().stream()
<<<<<<< HEAD
            .map(userRole -> userRole.getRole().getRoleName().name())
            .toList();
    }

=======
                .map(userRole -> userRole.getRole().getRoleName().name())
                .toList();
    }

    //returning error messages
>>>>>>> feature/kafka
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
