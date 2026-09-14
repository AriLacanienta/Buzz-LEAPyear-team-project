package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "model_portfolio", uniqueConstraints = {
    @UniqueConstraint(columnNames = "portfolio_name")
})
public class ModelPortfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;
    
    @Column(name = "portfolio_name", nullable = false, unique = true)
    private String portfolioName;
    
    @OneToMany(mappedBy = "modelPortfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions;
    
    @OneToMany(mappedBy = "modelPortfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioHolding> holdings;

    // Getters and Setters
    public Long getPortfolioId() { return id; }
    public void setPortfolioId(Long portfolioId) { this.id = portfolioId; }
    
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public void setSubscriptions(List<Subscription> subscriptions) { this.subscriptions = subscriptions; }
    
    public List<PortfolioHolding> getHoldings() { return holdings; }
    public void setHoldings(List<PortfolioHolding> holdings) { this.holdings = holdings; }
}
