package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "portfolio_holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"portfolio_id", "instrument_id"})
})
public class PortfolioHolding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holdings_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private ModelPortfolio modelPortfolio;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
    
    @Column(name = "holding_weight", nullable = false)
    private BigDecimal holdingWeight;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ModelPortfolio getModelPortfolio() { return modelPortfolio; }
    public void setModelPortfolio(ModelPortfolio modelPortfolio) { this.modelPortfolio = modelPortfolio; }
    
    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    
    public BigDecimal getHoldingWeight() { return holdingWeight; }
    public void setHoldingWeight(BigDecimal holdingWeight) { this.holdingWeight = holdingWeight; }
}
