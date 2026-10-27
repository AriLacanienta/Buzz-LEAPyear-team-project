package com.buzzleapyear.trading_api.dto;

public record StockComparisonResponseDto(
    QuoteResponseDto first,
    QuoteResponseDto second
) {}