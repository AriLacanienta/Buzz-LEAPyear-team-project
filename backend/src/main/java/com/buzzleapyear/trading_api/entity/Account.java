package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;
    
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(name = "risk_profile", nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskProfile riskProfile = RiskProfile.MODERATE;
    
    @Column(name = "cash_available", nullable = false)
    private BigDecimal cashAvailable = BigDecimal.ZERO;
    
    @Column(name = "cash_reserved", nullable = false)
    private BigDecimal cashReserved = BigDecimal.ZERO;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeOrder> tradeOrders;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditLog> auditLogs;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    
    public RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    
    public BigDecimal getCashAvailable() { return cashAvailable; }
    public void setCashAvailable(BigDecimal cashAvailable) { this.cashAvailable = cashAvailable; }
    
    public BigDecimal getCashReserved() { return cashReserved; }
    public void setCashReserved(BigDecimal cashReserved) { this.cashReserved = cashReserved; }
    
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public void setSubscriptions(List<Subscription> subscriptions) { this.subscriptions = subscriptions; }
    
    public List<TradeOrder> getTradeOrders() { return tradeOrders; }
    public void setTradeOrders(List<TradeOrder> tradeOrders) { this.tradeOrders = tradeOrders; }
    
    public List<Holding> getHoldings() { return holdings; }
    public void setHoldings(List<Holding> holdings) { this.holdings = holdings; }
    
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AuditLog> auditLogs) { this.auditLogs = auditLogs; }
    
    public enum RiskProfile {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }
}
