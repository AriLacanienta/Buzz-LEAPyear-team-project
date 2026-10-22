package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByBaseCurrencyAndQuoteCurrencyAndActiveTrue(
        String baseCurrency, String quoteCurrency);
}
