package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;

public record FundsRequestDto(
    BigDecimal amount, 
    String action
) {
}
