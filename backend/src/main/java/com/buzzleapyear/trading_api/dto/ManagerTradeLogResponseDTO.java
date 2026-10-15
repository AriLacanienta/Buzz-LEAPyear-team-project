package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ManagerTradeLogResponseDTO(
    Long tradeId,
    LocalDateTime tradeDate,
    Long accountId,
    String accountName,
    Long clientId,
    String clientName,
    String clientEmail,
    String instrumentName,
    String instrumentSymbol,
    String assetType,
    String currency,
    String side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal value,
    OrderStatus status,
    LocalDateTime statusUpdatedAt,
    String statusReason
) {}