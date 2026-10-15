package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;

public record RecentTradeOrderResponseDto(
    Long orderId,
    String instrumentSymbol,
    OrderSide side,
    BigDecimal quantity,
    BigDecimal price,
    LocalDateTime orderDate,
    OrderStatus status,
    LocalDateTime timeUpdated,
    String reasonText
){}