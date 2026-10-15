package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import java.time.LocalDateTime;

public class OrderStatusDTO {
    
    private Long orderId;
    private Long accountId;
    private OrderStatus status;
    private String reasonText;
    private LocalDateTime timeUpdated;

    // Constructors
    public OrderStatusDTO() {}
    
    public OrderStatusDTO(Long orderId, Long accountId, OrderStatus status, String reasonText, LocalDateTime timeUpdated) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.status = status;
        this.reasonText = reasonText;
        this.timeUpdated = timeUpdated;
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    
    public LocalDateTime getTimeUpdated() { return timeUpdated; }
    public void setTimeUpdated(LocalDateTime timeUpdated) { this.timeUpdated = timeUpdated; }
}
