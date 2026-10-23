package com.buzzleapyear.trading_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank String message
) {}