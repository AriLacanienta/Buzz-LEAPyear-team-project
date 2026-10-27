package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.repository.DashboardSummaryRepository;
import com.buzzleapyear.trading_api.repository.DashboardSummaryRepository.OrderCounts;
import com.buzzleapyear.trading_api.repository.DashboardSummaryRepository.DisputeCounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagerDashboardServiceTests {
    private final DashboardSummaryRepository repository = mock(DashboardSummaryRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"),
        ZoneId.of("America/New_York"));
    private final ManagerDashboardService service = new ManagerDashboardService(repository, clock);

    @BeforeEach
    void setUp() {
        when(repository.disputeCounts("manager")).thenReturn(new DisputeCounts(5, 2));
        when(repository.assetsMoved(any(), any())).thenReturn(List.of());
    }

    @Test
    void calculatesCohortPercentagesAndUsesServerCalendarFromOneClockReading() {
        when(repository.orderCounts(any(), any(), any())).thenReturn(new OrderCounts(3, 2, 2, 2));

        var result = service.summary("manager");

        assertEquals(LocalDate.of(2026, 10, 22), result.businessDate());
        assertEquals("America/New_York", result.timeZone());
        assertEquals(clock.instant(), result.asOf());
        assertEquals(1, result.ordersDifference());
        assertEquals(new BigDecimal("50.0"), result.ordersChangePercent());
        assertEquals(new BigDecimal("66.7"), result.fillRatePercent());
        assertEquals(5, result.openDisputes());
        assertEquals(2, result.criticalOpenDisputes());
        assertEquals(2, result.activeClientsToday());
        var today = result.businessDate().atStartOfDay();
        verify(repository).orderCounts(today.minusDays(1), today, today.plusDays(1));
        verify(repository).assetsMoved(today, today.plusDays(1));
    }

    @Test
    void zeroYesterdayWithOrdersTodayHasUndefinedPercentage() {
        when(repository.orderCounts(any(), any(), any())).thenReturn(new OrderCounts(4, 0, 0, 1));
        assertNull(service.summary("manager").ordersChangePercent());
    }

    @Test
    void emptyDatabaseHasZeroPercentagesAndNoCurrencies() {
        when(repository.orderCounts(any(), any(), any())).thenReturn(new OrderCounts(0, 0, 0, 0));
        var result = service.summary("manager");
        assertEquals(new BigDecimal("0.0"), result.ordersChangePercent());
        assertEquals(new BigDecimal("0.0"), result.fillRatePercent());
        assertTrue(result.assetsMovedByCurrency().isEmpty());
    }

    @Test
    void declineHasNegativeCountAndPercentage() {
        when(repository.orderCounts(any(), any(), any())).thenReturn(new OrderCounts(1, 4, 1, 1));
        var result = service.summary("manager");
        assertEquals(-3, result.ordersDifference());
        assertEquals(new BigDecimal("-75.0"), result.ordersChangePercent());
    }

    @Test
    void backendFailuresAreNotConvertedToSuccessfulZeroMetrics() {
        when(repository.orderCounts(any(), any(), any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.summary("manager"));
    }
}
