package com.buzzleapyear.trading_api.admin;

import com.buzzleapyear.trading_api.entity.RefreshSession;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.RefreshSessionRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.service.AdminTokenRevocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTokenRevocationTests {

    @Mock UserRepository userRepository;
    @Mock RefreshSessionRepository refreshSessionRepository;

    //confirms the manager list only includes users with active refresh sessions.
    @Test
    void listsUsersWithActiveRefreshSessions() {
        User user = user(7L, "alice");
        RefreshSession session = session("alice");
        AdminTokenRevocationService service = service();

        when(refreshSessionRepository.findByRevokedAtIsNullAndExpiresAtAfter(any()))
                .thenReturn(List.of(session));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertEquals(List.of(user), service.getActiveUsers(null));
    }

    //confirms revocation invalidates access tokens and refresh sessions together.
    @Test
    void revokesAccessAndRefreshTokensForUser() {
        User user = user(7L, "alice");
        AdminTokenRevocationService service = service();

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.revokeUserTokens(7L);

        assertEquals(1, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(refreshSessionRepository).revokeAllByUsername(eq("alice"), any(LocalDateTime.class));
    }

    //confirms revocation returns an error when the requested user does not exist.
    @Test
    void rejectsUnknownUser() {
        AdminTokenRevocationService service = service();
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.revokeUserTokens(7L));
    }

    private AdminTokenRevocationService service() {
        return new AdminTokenRevocationService(userRepository, refreshSessionRepository);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private RefreshSession session(String username) {
        RefreshSession session = new RefreshSession();
        session.setUsername(username);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        return session;
    }
}
