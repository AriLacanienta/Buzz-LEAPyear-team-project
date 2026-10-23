package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.ManagerDashboardSummary;
import com.buzzleapyear.trading_api.repository.DashboardSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class ManagerDashboardService {
    private final DashboardSummaryRepository repository;
    private final Clock clock;

    public ManagerDashboardService(DashboardSummaryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ManagerDashboardSummary summary(String username) {
        var asOf = clock.instant();
        var date = LocalDate.ofInstant(asOf, clock.getZone());
        var today = date.atStartOfDay();
        var counts = repository.orderCounts(date.minusDays(1).atStartOfDay(), today, date.plusDays(1).atStartOfDay());
        var disputes = repository.disputeCounts(username);
        long difference = counts.today() - counts.yesterday();
        BigDecimal change = counts.yesterday() == 0
            ? (counts.today() == 0 ? new BigDecimal("0.0") : null)
            : percentage(difference, counts.yesterday());
        return new ManagerDashboardSummary(date, clock.getZone().getId(), asOf,
            counts.today(), counts.yesterday(), difference, change, counts.filled(),
            percentage(counts.filled(), counts.today()), disputes.open(), disputes.critical(),
            counts.activeUsers(), repository.assetsMoved(today, date.plusDays(1).atStartOfDay()));
    }

    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? new BigDecimal("0.0")
            : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
