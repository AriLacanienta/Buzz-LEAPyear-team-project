package com.buzzleapyear.trading_api.dto;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TradeOrderPreviewRequestDto(
    @NotNull OrderSide side,
    @NotBlank String instrumentSymbol,
    @NotNull @Positive BigDecimal quantity,
    @NotNull Long accountId
){}
