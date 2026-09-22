package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trade_orders")
public class TradeOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_order_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
    
    @Column(name = "side", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderSide side;
    
    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
        
    @Column(name = "trade_value", nullable = false)
    private BigDecimal tradeValue;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @OneToMany(mappedBy = "tradeOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeOrderStatus> statuses;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide orderSide) { this.side = orderSide; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getValue() { return tradeValue; }
    public void setValue(BigDecimal value) { this.tradeValue = value; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    
    public List<TradeOrderStatus> getStatuses() { return statuses; }
    public void setStatuses(List<TradeOrderStatus> statuses) { this.statuses = statuses; }
    public void addStatus(TradeOrderStatus newStatus) { this.statuses.add(newStatus); }
    
    public enum OrderSide {
        BUY, SELL
    }
}
