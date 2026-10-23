package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.ExchangeRate;
import com.buzzleapyear.trading_api.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExchangeRateService {
    private static final String USD = "USD";
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public BigDecimal getToUsdRate(String currency) {
        String normalized = normalize(currency);
        if (USD.equals(normalized)) {
            return BigDecimal.ONE;
        }
        ExchangeRate rate = exchangeRateRepository
            .findByBaseCurrencyAndQuoteCurrencyAndActiveTrue(normalized, USD)
            .orElseThrow(() -> new IllegalStateException("No active USD exchange rate for " + normalized));
        return rate.getRate();
    }

    public BigDecimal toUsd(BigDecimal amount, String currency) {
        return amount.multiply(getToUsdRate(currency)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal fromUsd(BigDecimal amount, String currency) {
        return amount.divide(getToUsdRate(currency), 8, RoundingMode.HALF_UP);
    }

    private String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        return currency.toUpperCase();
    }
}
