package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;

public record PortfolioSummaryResponseDto(
    BigDecimal portfolioValue,
    BigDecimal totalCostBasis,
    BigDecimal todayChange,
    BigDecimal todayChangePercent,
    BigDecimal allTimeChange,
    BigDecimal allTimeChangePercent
) {}