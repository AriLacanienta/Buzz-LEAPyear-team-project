package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;

public record HoldingResponseDto(
    String instrumentName,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal currentPrice,
    String currencyCode,
    BigDecimal totalValue,
    BigDecimal usdValue,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal totalCost
) {}