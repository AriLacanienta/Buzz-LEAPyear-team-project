package com.buzzleapyear.trading_api.dto;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrder.QuantityMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;

public record TradeOrderPreviewRequestDto(
    @NotNull OrderSide side,
    @NotBlank String instrumentSymbol,
    QuantityMode quantityMode,
    @Positive BigDecimal quantity,
    @Positive BigDecimal amount,
    @NotNull Long accountId
){
    public QuantityMode mode() {
        return quantityMode == null ? QuantityMode.SHARES : quantityMode;
    }

    @AssertTrue(message = "Quantity is required for share orders; amount is required for dollar-amount orders")
    public boolean isQuantityOrAmountProvided() {
        return mode() == QuantityMode.AMOUNT ? amount != null : quantity != null;
    }
}
