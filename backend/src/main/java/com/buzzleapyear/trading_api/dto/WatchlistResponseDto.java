package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistResponseDto(
    Long id,
    String symbol,
    String instrumentName,
    String assetType,
    String currencyCode,
    BigDecimal price,
    BigDecimal change,
    BigDecimal changePercent,
    LocalDateTime timestamp
) {
}
