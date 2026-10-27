package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"base_currency", "quote_currency"})
})
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_rate_id")
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
