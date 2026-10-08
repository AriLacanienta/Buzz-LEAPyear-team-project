package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import java.math.BigDecimal;

public record TradeOrderPreviewResponseDto(
    String instrumentSymbol,
    OrderSide side,
    BigDecimal quantity,
    BigDecimal livePrice,
    BigDecimal estimatedValue,
    BigDecimal cashAvailable,
    BigDecimal cashAfter,
    BigDecimal holdingQuantity,
    BigDecimal holdingQuantityAfter,
    BigDecimal averageCostAfter,
    BigDecimal realizedPnL,
    boolean valid,
    String reason
){}