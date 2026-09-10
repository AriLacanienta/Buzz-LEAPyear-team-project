package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Analyst;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalystRepository extends JpaRepository<Analyst, Long> {
    Optional<Analyst> findByUserId(Long userId);
}
