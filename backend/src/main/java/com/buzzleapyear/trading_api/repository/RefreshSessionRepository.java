package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :revokedAt "
            + "where s.username = :username and s.revokedAt is null")
    void revokeAllByUsername(@Param("username") String username, @Param("revokedAt") LocalDateTime revokedAt);
}
