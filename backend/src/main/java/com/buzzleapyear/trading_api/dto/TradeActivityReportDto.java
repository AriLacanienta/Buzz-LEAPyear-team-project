package com.buzzleapyear.trading_api.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeActivityReportDto(
    Long orderId,
    String instrumentSymbol,
    String side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal tradeValue,
    LocalDateTime orderDate,
    String status,
    LocalDateTime statusTime
) {}
