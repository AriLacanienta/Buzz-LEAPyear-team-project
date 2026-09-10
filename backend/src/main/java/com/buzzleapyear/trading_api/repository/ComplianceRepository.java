package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Compliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplianceRepository extends JpaRepository<Compliance, Long> {
    Optional<Compliance> findByUserId(Long userId);
}
