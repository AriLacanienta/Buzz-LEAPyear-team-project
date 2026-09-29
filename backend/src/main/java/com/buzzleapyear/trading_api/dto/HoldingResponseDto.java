package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;

public record HoldingResponseDto(
    String instrumentName,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal currentPrice,
    BigDecimal totalValue,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal totalCost
) {}