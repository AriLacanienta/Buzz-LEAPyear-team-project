package com.buzzleapyear.trading_api.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes", indexes = {
    @Index(name = "idx_quote_timestamp", columnList = "instrument_id, timestamp DESC")
})  
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "high_price_of_day", nullable = false)
    private BigDecimal highPriceOfDay;

    @Column(name = "low_price_of_day", nullable = false)
    private BigDecimal lowPriceOfDay;

    @Column(name = "open_price_of_day", nullable = false)
    private BigDecimal openPriceOfDay;

    @Column(name = "previous_close_price_of_day", nullable = false)
    private BigDecimal previousClosePriceOfDay;

    @Column(name = "change_amount", nullable = false)
    private BigDecimal change;

    @Column(name = "change_percent", nullable = false)
    private BigDecimal changePercent;

    @Column(name = "volume", nullable = false)
    private long volume;

    @Column(name = "market_cap", nullable = false)
    private BigDecimal marketCap;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
