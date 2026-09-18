package com.buzzleapyear.trading_api.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListInstrumentsResponseDto {
    private String instrumentName;
    private String instrumentSymbol;
    private BigDecimal currentPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal volume;
    private BigDecimal marketCap;
    
}
