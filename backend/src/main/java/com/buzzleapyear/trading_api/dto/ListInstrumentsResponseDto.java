package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;

public record ListInstrumentsResponseDto(
    String instrumentName,
    String instrumentSymbol,
    BigDecimal currentPrice,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal volume,
    BigDecimal marketCap
) {
}
