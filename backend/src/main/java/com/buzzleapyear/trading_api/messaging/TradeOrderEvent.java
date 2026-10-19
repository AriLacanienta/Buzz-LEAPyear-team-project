package com.buzzleapyear.trading_api.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeOrderEvent(
    UUID eventId,
    String eventType,
    Long orderId,
    Long accountId,
    Long clientId,
    Long instrumentId,
    String instrumentSymbol,
    String assetType,
    String side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal tradeValue,
    LocalDateTime orderDate,
    String status,
    LocalDateTime statusTime,
    String reason
) {}