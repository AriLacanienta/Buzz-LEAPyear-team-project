package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;

public class TradeOrderResponseDTO {
    
    private Long orderId;
    private OrderStatus status;
    private String statusMessage;

    // Constructors
    public TradeOrderResponseDTO() {}
    
    public TradeOrderResponseDTO(Long orderId, OrderStatus status, String statusMessage) {
        this.orderId = orderId;
        this.status = status;
        this.statusMessage = statusMessage;
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}
