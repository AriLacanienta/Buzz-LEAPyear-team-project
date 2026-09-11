package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_order_status")
public class TradeOrderStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_order_id", nullable = false)
    private TradeOrder tradeOrder;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "status_date", nullable = false)
    private LocalDateTime statusDate;
    
    @Column(name = "status_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusName statusName;
    
    @Column(name = "time_updated", nullable = false)
    private LocalDateTime timeUpdated;

    // Getters and Setters
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    
    public TradeOrder getTradeOrder() { return tradeOrder; }
    public void setTradeOrder(TradeOrder tradeOrder) { this.tradeOrder = tradeOrder; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getStatusDate() { return statusDate; }
    public void setStatusDate(LocalDateTime statusDate) { this.statusDate = statusDate; }
    
    public StatusName getStatusName() { return statusName; }
    public void setStatusName(StatusName statusName) { this.statusName = statusName; }
    
    public LocalDateTime getTimeUpdated() { return timeUpdated; }
    public void setTimeUpdated(LocalDateTime timeUpdated) { this.timeUpdated = timeUpdated; }
    
    public enum StatusName {
        PENDING, SUCCESS, FAILED
    }
}
