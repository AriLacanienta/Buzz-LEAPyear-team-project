package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.dto.ErrorResponse;
import com.buzzleapyear.trading_api.dto.PasswordChangeRequest;
import com.buzzleapyear.trading_api.dto.UserSettingsRequest;
import com.buzzleapyear.trading_api.dto.UserSettingsResponse;
import com.buzzleapyear.trading_api.dto.UserProfileResponse;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.dto.UserResponseDto;
import com.buzzleapyear.trading_api.service.UserService;
import com.buzzleapyear.trading_api.util.SupportedCountries;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private static final Set<String> PROFILE_PICTURES = Set.of(
            "assets/images/space_avatar_astronaut.svg",
            "assets/images/space_avatar_planet.svg",
            "assets/images/space_avatar_rocket.svg",
            "assets/images/joanna.jpg",
            "assets/images/profile_photo.jpg",
            "assets/images/profile_avatar_3.svg"
    );


    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Logger logger = LoggerFactory.getLogger(UserController.class);

    /*
     * GET /api/v1/user/profile/{username}
     * Get user profile information by username
     * @param username The username of the user
     * @return UserProfileResponse with user details
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username, Authentication authentication) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(authentication.getName());

            if (!username.equals(authentication.getName()))
                logger.warn("{} attempted to get profile info for {}", authentication.getName(), username);

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

    @GetMapping("/me/settings")
    public ResponseEntity<UserSettingsResponse> getMySettings(Authentication authentication) {
        Optional<User> user = userRepository.findByUsername(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toSettingsResponse(user.get()));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<?> updateMySettings(
            @RequestBody UserSettingsRequest request,
            Authentication authentication) {
        if (request.phoneNumber() != null && request.phoneNumber().trim().length() > 32) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Phone number must be 32 characters or fewer"));
        }
        if (request.country() != null && request.country().trim().length() > 80) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Country must be 80 characters or fewer"));
        }
        if (request.country() != null && !request.country().isBlank()
                && !SupportedCountries.contains(request.country().trim())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Please select a supported country"));
        }
        Object profilePicture = request.preferences() == null ? null : request.preferences().get("profilePicture");
        if (profilePicture != null
                && (!(profilePicture instanceof String path) || !PROFILE_PICTURES.contains(path))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Please select one of the available profile pictures"));
        }

        Optional<User> userOptional = userRepository.findByUsername(authentication.getName());
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        user.setPhoneNumber(request.phoneNumber() == null || request.phoneNumber().isBlank()
                ? null : request.phoneNumber().trim());
        user.setCountry(request.country() == null || request.country().isBlank()
            ? null : request.country().trim());
        if (request.preferences() != null) {
            try {
                user.setPreferencesJson(objectMapper.writeValueAsString(request.preferences()));
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body(new ErrorResponse("Could not save preferences"));
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(toSettingsResponse(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(
            @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        if (request.currentPassword() == null || request.newPassword() == null
                || request.newPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Current password and a new password of at least 8 characters are required"));
        }

        Optional<User> userOptional = userRepository.findByUsername(authentication.getName());
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Current password is incorrect"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private UserSettingsResponse toSettingsResponse(User user) {
        try {
            var preferences = objectMapper.readValue(
                    user.getPreferencesJson(), new TypeReference<java.util.Map<String, Object>>() {});
            return new UserSettingsResponse(user.getPhoneNumber(), user.getCountry(), preferences);
        } catch (Exception e) {
            return new UserSettingsResponse(user.getPhoneNumber(), user.getCountry(), java.util.Map.of());
        }
    }


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
