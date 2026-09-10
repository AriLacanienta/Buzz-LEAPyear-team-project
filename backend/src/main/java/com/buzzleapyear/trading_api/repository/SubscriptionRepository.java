package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByAccountId(Long accountId);
    List<Subscription> findByModelPortfolioId(Long portfolioId);
    Optional<Subscription> findByAccountIdAndModelPortfolioId(Long accountId, Long portfolioId);
}
