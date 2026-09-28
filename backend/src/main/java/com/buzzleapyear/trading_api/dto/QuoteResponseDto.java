package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteResponseDto(
    String symbol,
    BigDecimal price,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal openPrice,
    BigDecimal previousClosePrice,
    BigDecimal change,
    BigDecimal changePercent,
    long volume,
    BigDecimal marketCap,
    LocalDateTime timestamp
) {
}