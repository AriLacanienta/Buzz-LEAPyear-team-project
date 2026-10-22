package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.DisputeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DisputeResponse(
    Long id,
    Long tradeOrderId,
    Long accountId,
    BigDecimal tradeValue,
    String message,
    DisputeStatus status,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
