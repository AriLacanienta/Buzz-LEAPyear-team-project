package com.buzzleapyear.trading_api.dto;

import java.util.List;

public record TopMoversResponseDto(
    List<QuoteResponseDto> topGainers,
    List<QuoteResponseDto> topLosers
) {}