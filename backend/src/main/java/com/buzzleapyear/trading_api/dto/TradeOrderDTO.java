package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record TradeOrderDTO(
    
    @NotNull(message = "Side is required (BUY or SELL)")
    OrderSide side,
    
    @NotBlank(message = "Symbol is required")
    String instrumentSymbol,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    BigDecimal quantity,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    BigDecimal price,
    
    @NotNull(message = "Account ID is required") 
    Long accountId
) {}
