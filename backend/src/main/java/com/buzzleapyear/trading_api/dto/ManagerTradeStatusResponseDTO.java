package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import java.time.LocalDateTime;

public record ManagerTradeStatusResponseDTO(
    OrderStatus status,
    LocalDateTime timeUpdated,
    String reason
) {}