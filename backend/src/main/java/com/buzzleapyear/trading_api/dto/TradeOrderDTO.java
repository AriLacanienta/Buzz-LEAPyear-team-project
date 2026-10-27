package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrder.QuantityMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import jakarta.validation.constraints.AssertTrue;

public record TradeOrderDTO(
    
    @NotNull(message = "Side is required (BUY or SELL)")
    OrderSide side,
    
    @NotBlank(message = "Symbol is required")
    String instrumentSymbol,

    QuantityMode quantityMode,

    @Positive(message = "Quantity must be greater than 0")
    BigDecimal quantity,

    @Positive(message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    BigDecimal price,
    
    @NotNull(message = "Account ID is required") 
    Long accountId
) {
    public QuantityMode mode() {
        return quantityMode == null ? QuantityMode.SHARES : quantityMode;
    }

    @AssertTrue(message = "Quantity is required for share orders; amount is required for dollar-amount orders")
    public boolean isQuantityOrAmountProvided() {
        return mode() == QuantityMode.AMOUNT ? amount != null : quantity != null;
    }
}
