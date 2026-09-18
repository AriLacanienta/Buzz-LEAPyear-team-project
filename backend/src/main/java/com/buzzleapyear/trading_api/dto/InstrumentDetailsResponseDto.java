package com.buzzleapyear.trading_api.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstrumentDetailsResponseDto {
    private String instrumentName;
    private String instrumentSymbol;
    private String assetType;
    private String currencyCode;
}
