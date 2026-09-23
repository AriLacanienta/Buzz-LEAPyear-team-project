package com.buzzleapyear.trading_api.dto;

import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class TradeOrderDTO {
    
    @NotNull(message = "Side is required (BUY or SELL)")
    private OrderSide side;
    
    @NotNull(message = "Instrument ID is required")
    private Long instrumentId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private BigDecimal quantity;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Account ID is required")
    private Long accountId;

    // Constructors
    public TradeOrderDTO() {}
    
    public TradeOrderDTO(OrderSide side, Long instrumentId, BigDecimal quantity, BigDecimal price, Long accountId) {
        this.side = side;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.price = price;
        this.accountId = accountId;
    }

    // Getters and Setters
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }
    
    public Long getInstrumentId() { return instrumentId; }
    public void setInstrumentId(Long instrumentId) { this.instrumentId = instrumentId; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
