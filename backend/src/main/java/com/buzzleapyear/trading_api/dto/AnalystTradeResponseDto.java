package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.Instrument.AssetType;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnalystTradeResponseDto(
    Long orderId,
    Long accountId,
    String accountName,
    String clientName,
    String instrumentSymbol,
    AssetType assetType,
    String currencyCode,
    OrderSide side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal tradeValue,
    LocalDateTime orderDate,
    OrderStatus status,
    LocalDateTime timeUpdated,
    String reasonText
) {}