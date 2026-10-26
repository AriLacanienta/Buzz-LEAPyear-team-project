package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.DisputeStatus;
import com.buzzleapyear.trading_api.entity.DisputeSeverity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * DisputeResponse
 * @param id
 * @param tradeOrderId
 * @param clientName
 * @param accountId
 * @param tradeValue
 * @param message
 * @param status
 * @param severity
 * @param createdAt
 * @param resolvedAt
 * @param resolutionMessage
 */
public record DisputeResponse(
    Long id,
    Long tradeOrderId,
    String clientName,
    Long accountId,
    BigDecimal tradeValue,
    String message,
    DisputeStatus status,
    DisputeSeverity severity,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt,
    String resolutionMessage
) {}
