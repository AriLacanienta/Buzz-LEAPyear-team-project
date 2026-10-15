package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing")
public class Pricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
    
    @Column(name = "price", nullable = false)
    private BigDecimal price;
    
    @Column(name = "price_date", nullable = false)
    private LocalDateTime priceDate;

    // Constructors
    public Pricing() {}
    
    public Pricing(Instrument instrument, BigDecimal price, LocalDateTime priceDate) {
        this.instrument = instrument;
        this.price = price;
        this.priceDate = priceDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public LocalDateTime getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDateTime priceDate) { this.priceDate = priceDate; }
}
