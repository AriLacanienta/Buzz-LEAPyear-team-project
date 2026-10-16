package com.buzzleapyear.trading_api.controller;

import com.buzzleapyear.trading_api.service.AdminTokenRevocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sessions")
public class AdminTokenRevocationController {
    private final AdminTokenRevocationService revocationService;

    public AdminTokenRevocationController(AdminTokenRevocationService revocationService) {
        this.revocationService = revocationService;
    }

    //returns active users, filtered by user id for manager search
    //frontend connection here:GET /api/v1/admin/sessions?userId=<id>
    //TODO: Manager UI should provide a user-ID search field and revoke button.
    @GetMapping
    public List<ActiveUserResponse> getActiveUsers(
            @RequestParam(required = false) Long userId) {
        return revocationService.getActiveUsers(userId).stream()
                .map(user -> new ActiveUserResponse(user.getId(), user.getUsername()))
                .toList();
    }

    //revokes every token for the selected user.
    //frontend connection here: DELETE /api/v1/admin/sessions/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revokeUserTokens(@PathVariable Long userId) {
        revocationService.revokeUserTokens(userId);
        return ResponseEntity.noContent().build();
    }

    public record ActiveUserResponse(Long userId, String username) {
    }
}
