package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.DisputeSeverity;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DisputeSeverityPolicy {
    public static final BigDecimal CRITICAL_TRADE_VALUE = new BigDecimal("10000.00");

    private DisputeSeverityPolicy() {}

    public static DisputeSeverity severity(BigDecimal quantity, BigDecimal price) {
        BigDecimal executionValue = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
        return executionValue.compareTo(CRITICAL_TRADE_VALUE) >= 0
            ? DisputeSeverity.CRITICAL : DisputeSeverity.NORMAL;
    }
}
