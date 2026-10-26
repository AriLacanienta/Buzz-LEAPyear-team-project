package com.buzzleapyear.trading_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ManagerDashboardSummary(
    LocalDate businessDate,
    String timeZone,
    Instant asOf,
    long ordersToday,
    long ordersYesterday,
    long ordersDifference,
    BigDecimal ordersChangePercent,
    long filledOrdersToday,
    BigDecimal fillRatePercent,
    long openDisputes,
    long criticalOpenDisputes,
    long activeClientsToday,
    List<CurrencyTotal> assetsMovedByCurrency
) {
    public record CurrencyTotal(String currencyCode, BigDecimal totalValue) {}
}
