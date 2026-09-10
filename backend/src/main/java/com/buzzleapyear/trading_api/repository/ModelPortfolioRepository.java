package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.ModelPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelPortfolioRepository extends JpaRepository<ModelPortfolio, Long> {
    Optional<ModelPortfolio> findByPortfolioName(String portfolioName);
}
