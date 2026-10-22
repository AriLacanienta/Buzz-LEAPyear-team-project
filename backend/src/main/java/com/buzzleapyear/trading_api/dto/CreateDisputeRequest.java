package com.buzzleapyear.trading_api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDisputeRequest(
    @NotNull(message = "Trade order ID is required") Long tradeOrderId,
    String message
) {}
