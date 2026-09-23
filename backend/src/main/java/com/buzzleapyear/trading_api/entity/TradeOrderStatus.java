package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_order_status")
public class TradeOrderStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_order_id", nullable = false)
    private TradeOrder tradeOrder;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "time_updated", nullable = false)
    private LocalDateTime timeUpdated;
    
    @Column(name = "reason_text", length = 500)
    private String reasonText;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TradeOrder getTradeOrder() { return tradeOrder; }
    public void setTradeOrder(TradeOrder tradeOrder) { this.tradeOrder = tradeOrder; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus statusName) { this.status = statusName; }
    
    public LocalDateTime getTimeUpdated() { return timeUpdated; }
    public void setTimeUpdated(LocalDateTime timeUpdated) { this.timeUpdated = timeUpdated; }
    
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    
    public enum OrderStatus {
        SUBMITTED, VALIDATED, FILLED, REJECTED
    }
}
