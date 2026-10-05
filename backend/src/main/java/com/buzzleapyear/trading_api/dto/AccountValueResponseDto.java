package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;

public record AccountValueResponseDto(
    BigDecimal cashAvailable,
    BigDecimal cashReserved,
    BigDecimal portfolioValue
) {}
