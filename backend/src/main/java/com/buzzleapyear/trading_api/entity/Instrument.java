package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "instruments", uniqueConstraints = {
    @UniqueConstraint(columnNames = "instrument_symbol")
})
public class Instrument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instrument_id")
    private Long id;
    
    @Column(name = "instrument_name", nullable = false)
    private String instrumentName;
    
    @Column(name = "instrument_symbol", nullable = false, unique = true)
    private String instrumentSymbol;
    
    @Column(name = "asset_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "trading_session", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradingSession tradingSession;

    @Column(name = "whole_shares_allowed", nullable = false)
    private Boolean wholeSharesAllowed;

    @Column(name = "fractional_allowed", nullable = false)
    private Boolean fractionalAllowed;

    @Column(name = "quantity_precision", nullable = false)
    private Integer quantityPrecision;

    @Column(name = "min_order_amount", nullable = false)
    private BigDecimal minOrderAmount;
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeOrder> tradeOrders;
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings;
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioHolding> portfolioHoldings;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TradingSession getTradingSession() { return tradingSession; }
    public void setTradingSession(TradingSession tradingSession) { this.tradingSession = tradingSession; }

    public Boolean getWholeSharesAllowed() { return wholeSharesAllowed; }
    public void setWholeSharesAllowed(Boolean wholeSharesAllowed) { this.wholeSharesAllowed = wholeSharesAllowed; }

    public Boolean getFractionalAllowed() { return fractionalAllowed; }
    public void setFractionalAllowed(Boolean fractionalAllowed) { this.fractionalAllowed = fractionalAllowed; }

    public Integer getQuantityPrecision() { return quantityPrecision; }
    public void setQuantityPrecision(Integer quantityPrecision) { this.quantityPrecision = quantityPrecision; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    
    public String getInstrumentName() { return instrumentName; }
    public void setInstrumentName(String instrumentName) { this.instrumentName = instrumentName; }
    
    public String getInstrumentSymbol() { return instrumentSymbol; }
    public void setInstrumentSymbol(String instrumentSymbol) { this.instrumentSymbol = instrumentSymbol; }
    
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    
    public List<TradeOrder> getTradeOrders() { return tradeOrders; }
    public void setTradeOrders(List<TradeOrder> tradeOrders) { this.tradeOrders = tradeOrders; }
    
    public List<Holding> getHoldings() { return holdings; }
    public void setHoldings(List<Holding> holdings) { this.holdings = holdings; }
    
    public List<PortfolioHolding> getPortfolioHoldings() { return portfolioHoldings; }
    public void setPortfolioHoldings(List<PortfolioHolding> portfolioHoldings) { this.portfolioHoldings = portfolioHoldings; }
    
    public enum AssetType {
        EQUITY, BOND, FUND, CASH, CRYPTO, ETF
    }

    public enum TradingSession {
        US_MARKET, UK_MARKET, INDIA_MARKET, ALWAYS_OPEN
    }

    @PrePersist
    public void applyDefaultTradingRules() {
        boolean crypto = assetType == AssetType.CRYPTO;
        boolean bond = assetType == AssetType.BOND;
        if (tradingSession == null) {
            tradingSession = crypto ? TradingSession.ALWAYS_OPEN : switch (String.valueOf(currencyCode)) {
                case "GBP" -> TradingSession.UK_MARKET;
                case "INR" -> TradingSession.INDIA_MARKET;
                default -> TradingSession.US_MARKET;
            };
        }
        if (wholeSharesAllowed == null) wholeSharesAllowed = !crypto;
        if (fractionalAllowed == null) fractionalAllowed = !bond;
        if (quantityPrecision == null) quantityPrecision = crypto ? 0 : 6;
        if (minOrderAmount == null) minOrderAmount = BigDecimal.ONE;
    }

    public String getMarket() {
    if (currencyCode == null) {
        return "UNKNOWN";
    }
    return switch (currencyCode) {
        case "USD" -> "US";
        case "GBP" -> "UK";
        case "INR" -> "India";
        default -> currencyCode;
    };
}
}
