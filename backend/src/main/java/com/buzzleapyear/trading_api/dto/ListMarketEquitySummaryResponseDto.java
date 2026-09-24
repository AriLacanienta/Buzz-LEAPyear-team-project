package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;

public record ListMarketEquitySummaryResponseDto(
    String instrumentName,
    String instrumentSymbol,
    BigDecimal currentPrice,
    BigDecimal change,
    BigDecimal changePercent,
    long volume,
    BigDecimal marketCap
) {}
