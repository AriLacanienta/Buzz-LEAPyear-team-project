package com.buzzleapyear.trading_api.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public record CreateDisputeRequest(
    @NotNull(message = "Trade order ID is required") Long tradeOrderId,
    @Length(max = 2000) String message
) {}
