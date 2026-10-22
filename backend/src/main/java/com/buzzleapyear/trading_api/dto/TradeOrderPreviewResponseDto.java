package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrder.QuantityMode;
import java.math.BigDecimal;

public record TradeOrderPreviewResponseDto(
    String instrumentSymbol,
    OrderSide side,
    QuantityMode quantityMode,
    BigDecimal quantity,
    BigDecimal requestedAmount,
    BigDecimal livePrice,
    BigDecimal estimatedValue,
    BigDecimal cashAvailable,
    BigDecimal cashAfter,
    BigDecimal holdingQuantity,
    BigDecimal holdingQuantityAfter,
    BigDecimal averageCostAfter,
    BigDecimal realizedPnL,
    boolean valid,
    String reason,
    String instrumentCurrency,
    BigDecimal exchangeRate,
    BigDecimal estimatedNativeValue
){}