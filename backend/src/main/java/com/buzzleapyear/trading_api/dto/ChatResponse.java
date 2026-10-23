package com.buzzleapyear.trading_api.dto;

public record ChatResponse(
    String intent,
    String message,
    Object data,
    String action
) {}