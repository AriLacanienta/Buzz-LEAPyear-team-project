package com.buzzleapyear.trading_api.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_event_outbox")
public class TradeEventOutbox {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected TradeEventOutbox() {}

    public TradeEventOutbox(
            UUID eventId,
            Long accountId,
            String eventType,
            String payload,
            LocalDateTime createdAt) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getEventId() { return eventId; }
    public Long getAccountId() { return accountId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
