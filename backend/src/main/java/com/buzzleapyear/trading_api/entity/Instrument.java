package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.util.List;

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
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeOrder> tradeOrders;
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings;
    
    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioHolding> portfolioHoldings;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
}
