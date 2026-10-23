package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.DisputeSeverity;
import com.buzzleapyear.trading_api.service.DisputeSeverityPolicy;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DashboardSummaryRepositoryTests {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    private static JdbcTemplate jdbc;
    private static DashboardSummaryRepository repository;
    private static final LocalDateTime TODAY = LocalDateTime.of(2026, 10, 22, 0, 0);

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new DashboardSummaryRepository(new NamedParameterJdbcTemplate(dataSource));
    }

    @BeforeEach
    void resetFixtures() {
        jdbc.execute("TRUNCATE users, instruments CASCADE");
    }

    @Test
    void resolutionColumnSupportsLegacyRowsAndSeparateResolutionText() {
        long owner = account("client");
        long orderId = order(owner, "USD", "BUY", "1", "100");
        dispute(orderId, "RESOLVED");
        assertNull(jdbc.queryForObject(
            "SELECT resolution_message FROM disputes WHERE trade_order_id = ?", String.class, orderId));
        jdbc.update("UPDATE disputes SET dispute_message = ?, resolution_message = ? WHERE trade_order_id = ?",
            "Original dispute", "x".repeat(2000), orderId);
        assertEquals("Original dispute", jdbc.queryForObject(
            "SELECT dispute_message FROM disputes WHERE trade_order_id = ?", String.class, orderId));
        assertEquals("x".repeat(2000), jdbc.queryForObject(
            "SELECT resolution_message FROM disputes WHERE trade_order_id = ?", String.class, orderId));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE disputes SET resolution_message = ? WHERE trade_order_id = ?",
                "x".repeat(2001), orderId));
    }

    @Test
    void countsFirstSubmissionsAndLatestFilledStateWithoutDuplicatingUsersOrOrders() {
        long owner = account("client");
        long secondAccount = extraAccount(owner);
        long manager = account("manager");
        long first = order(owner, "USD", "BUY", "1", "100");
        status(first, "SUBMITTED", TODAY);
        status(first, "SUBMITTED", TODAY.plusHours(1));
        status(first, "FILLED", TODAY.plusHours(2));
        long second = order(secondAccount, "USD", "SELL", "1", "100");
        status(second, "SUBMITTED", TODAY.plusHours(1));
        status(second, "FILLED", TODAY.plusHours(2));
        status(second, "REJECTED", TODAY.plusHours(2)); // Latest ID wins the timestamp tie.
        long third = order(manager, "USD", "BUY", "1", "100");
        status(third, "SUBMITTED", TODAY.plusHours(3));
        long older = order(owner, "USD", "BUY", "1", "100");
        status(older, "SUBMITTED", TODAY.minusNanos(1000));
        status(older, "SUBMITTED", TODAY.plusHours(1));
        status(older, "FILLED", TODAY.plusHours(2));
        long tomorrow = order(owner, "USD", "BUY", "1", "100");
        status(tomorrow, "SUBMITTED", TODAY.plusDays(1));
        long missingSubmission = order(owner, "USD", "BUY", "1", "100");
        status(missingSubmission, "FILLED", TODAY.plusHours(1));

        var counts = repository.orderCounts(TODAY.minusDays(1), TODAY, TODAY.plusDays(1));
        assertEquals(new DashboardSummaryRepository.OrderCounts(3, 1, 1, 2), counts);
    }

    @Test
    void assetsIncludeBothSidesAndOlderSubmissionsButOnlyFirstFillsTodayByCurrency() {
        long owner = account("client");
        long first = order(owner, "USD", "BUY", "2", "100");
        status(first, "SUBMITTED", TODAY.minusDays(1));
        status(first, "FILLED", TODAY);
        status(first, "FILLED", TODAY.plusHours(1));
        long second = order(owner, "USD", "SELL", "1", "50");
        status(second, "FILLED", TODAY.plusHours(2));
        long third = order(owner, "EUR", "BUY", "1", "75");
        status(third, "FILLED", TODAY.plusHours(2));
        long alreadyFilled = order(owner, "USD", "BUY", "1", "999");
        status(alreadyFilled, "FILLED", TODAY.minusNanos(1000));
        status(alreadyFilled, "FILLED", TODAY.plusHours(1));
        long tomorrow = order(owner, "USD", "BUY", "1", "1000");
        status(tomorrow, "FILLED", TODAY.plusDays(1));

        var totals = repository.assetsMoved(TODAY, TODAY.plusDays(1));
        assertEquals(2, totals.size());
        assertEquals("EUR", totals.get(0).currencyCode());
        assertEquals(new BigDecimal("75.00"), totals.get(0).totalValue());
        assertEquals("USD", totals.get(1).currencyCode());
        assertEquals(new BigDecimal("250.00"), totals.get(1).totalValue());
    }

    @Test
    void disputeCountsExcludeOwnAndResolvedAndMatchRoundedSeverityPolicy() {
        long manager = account("manager");
        long client = account("client");
        dispute(order(manager, "USD", "BUY", "1", "20000"), "OPEN");
        dispute(order(client, "USD", "BUY", "1", "20000"), "RESOLVED");
        String[][] values = {{"1", "9999.99"}, {"1", "10000"}, {"1", "10000.01"},
            {"0.9999", "10001"}, {"0.9998", "10001"}};
        long expectedCritical = 0;
        for (String[] value : values) {
            dispute(order(client, "USD", "BUY", value[0], value[1]), "OPEN");
            if (DisputeSeverityPolicy.severity(new BigDecimal(value[0]), new BigDecimal(value[1]))
                == DisputeSeverity.CRITICAL) {
                expectedCritical++;
            }
        }
        var counts = repository.disputeCounts("manager");
        assertEquals(values.length, counts.open());
        assertEquals(3, expectedCritical);
        assertEquals(expectedCritical, counts.critical());
    }

    @Test
    void emptyDatabaseProducesZerosAndNoCurrencies() {
        assertEquals(new DashboardSummaryRepository.OrderCounts(0, 0, 0, 0),
            repository.orderCounts(TODAY.minusDays(1), TODAY, TODAY.plusDays(1)));
        assertEquals(new DashboardSummaryRepository.DisputeCounts(0, 0), repository.disputeCounts("manager"));
        assertTrue(repository.assetsMoved(TODAY, TODAY.plusDays(1)).isEmpty());
    }

    private long account(String username) {
        Long user = jdbc.queryForObject("""
            INSERT INTO users(username, user_first_name, user_last_name, email, password_hash)
            VALUES (?, 'Test', 'User', ?, 'test') RETURNING user_id
            """, Long.class, username, username + "@example.test");
        Long client = jdbc.queryForObject("INSERT INTO clients(user_id) VALUES (?) RETURNING clients_id", Long.class, user);
        return jdbc.queryForObject("""
            INSERT INTO accounts(account_name, client_id, risk_profile)
            VALUES ('Test', ?, 'MODERATE') RETURNING account_id
            """, Long.class, client);
    }

    private long extraAccount(long account) {
        return jdbc.queryForObject("""
            INSERT INTO accounts(account_name, client_id, risk_profile)
            SELECT 'Second', client_id, 'MODERATE' FROM accounts WHERE account_id = ? RETURNING account_id
            """, Long.class, account);
    }

    private long order(long account, String currency, String side, String quantity, String price) {
        Long instrument = jdbc.queryForObject("""
            INSERT INTO instruments(instrument_name, instrument_symbol, asset_type, currency_code)
            VALUES ('Test', ?, 'EQUITY', ?) RETURNING instrument_id
            """, Long.class, java.util.UUID.randomUUID().toString(), currency);
        return jdbc.queryForObject("""
            INSERT INTO trade_orders(account_id, instrument_id, side, quantity, price, trade_value, order_date)
            VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING trade_order_id
            """, Long.class, account, instrument, side, new BigDecimal(quantity), new BigDecimal(price),
            new BigDecimal(quantity).multiply(new BigDecimal(price)), TODAY);
    }

    private void status(long order, String status, LocalDateTime time) {
        jdbc.update("INSERT INTO trade_order_status(trade_order_id, status, time_updated) VALUES (?, ?, ?)",
            order, status, time);
    }

    private void dispute(long order, String status) {
        jdbc.update("""
            INSERT INTO disputes(trade_order_id, status, created_at, resolved_at)
            VALUES (?, ?, ?, ?)
            """, order, status, TODAY.minusDays(10), status.equals("RESOLVED") ? TODAY : null);
    }
}
