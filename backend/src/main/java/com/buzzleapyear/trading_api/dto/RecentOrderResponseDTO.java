package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecentOrderResponseDTO {
    
    private Long tradeId;
    private LocalDateTime tradeDate;
    private Long clientId;
    private String clientName;
    private String instrument;
    private String assetClass;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private String currency;
    private BigDecimal value;

    // Constructors
    public RecentOrderResponseDTO() {}
    
    public RecentOrderResponseDTO(Long tradeId, LocalDateTime tradeDate, Long clientId, String clientName,
                                   String instrument, String assetClass, String side, BigDecimal quantity,
                                   BigDecimal price, String currency, BigDecimal value) {
        this.tradeId = tradeId;
        this.tradeDate = tradeDate;
        this.clientId = clientId;
        this.clientName = clientName;
        this.instrument = instrument;
        this.assetClass = assetClass;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
        this.value = value;
    }

    // Getters and Setters
    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
    
    public LocalDateTime getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDateTime tradeDate) { this.tradeDate = tradeDate; }
    
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
    
    public String getAssetClass() { return assetClass; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
