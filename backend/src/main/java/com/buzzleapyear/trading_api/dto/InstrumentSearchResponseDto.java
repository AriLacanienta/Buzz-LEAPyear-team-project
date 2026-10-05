package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;

public record InstrumentSearchResponseDto(
    String instrumentSymbol,
    String instrumentName,
    String assetType,
    String currencyCode,
    BigDecimal currentPrice,
    BigDecimal percentChange
) {
}
