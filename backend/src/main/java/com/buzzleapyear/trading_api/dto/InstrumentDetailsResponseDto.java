package com.buzzleapyear.trading_api.dto;

public record InstrumentDetailsResponseDto(
    String instrumentName,
    String instrumentSymbol,
    String assetType,
    String currencyCode
) {
}
