package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.RefreshSession;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.RefreshSessionRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminTokenRevocationService {
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    public AdminTokenRevocationService(UserRepository userRepository,
        RefreshSessionRepository refreshSessionRepository) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
    }

    //finds distinct users who currently have an unrevoked refresh session.
    public List<User> getActiveUsers(Long userId) {
        Set<String> usernames = refreshSessionRepository
                .findByRevokedAtIsNullAndExpiresAtAfter(LocalDateTime.now()).stream()
                .map(RefreshSession::getUsername)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return usernames.stream()
                .map(userRepository::findByUsername)
                .flatMap(Optional::stream)
                .filter(user -> userId == null || user.getId().equals(userId))
                .toList();
    }

    //revokes refresh sessions and invalidates all existing access tokens for one user.
    @Transactional
    public void revokeUserTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.revokeAccessTokens();
        userRepository.save(user);
        refreshSessionRepository.revokeAllByUsername(user.getUsername(), LocalDateTime.now());
    }
}
