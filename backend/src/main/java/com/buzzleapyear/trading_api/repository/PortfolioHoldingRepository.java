package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
    List<PortfolioHolding> findByModelPortfolioId(Long portfolioId);
    Optional<PortfolioHolding> findByModelPortfolioIdAndInstrumentId(Long portfolioId, Long instrumentId);
}
