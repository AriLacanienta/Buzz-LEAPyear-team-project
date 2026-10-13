package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/*
 * UserController handles user profile related endpoints
 * Endpoints:
 * GET /api/v1/user/profile/{username}: Get user profile by username
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /*
     * GET /api/v1/user/profile/{username}
     * Get user profile information by username
     * @param username The username of the user
     * @return UserProfileResponse with user details
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }

            User user = userOptional.get();

            //Returns user profile data
            return ResponseEntity.ok(new UserProfileResponse(
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching user profile: " + e.getMessage()));
        }
    }

    //Response DTO for user profile
    public static class UserProfileResponse {
        private String username;
        private String email;
        private String firstName;
        private String lastName;

        public UserProfileResponse(String username, String email, String firstName, String lastName) {
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        //Getters
        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }

   //Error response DTO
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
}
