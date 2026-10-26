package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.dto.ManagerDashboardSummary.CurrencyTotal;
import com.buzzleapyear.trading_api.service.DisputeSeverityPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardSummaryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardSummaryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record OrderCounts(long today, long yesterday, long filled, long activeUsers) {}
    public record DisputeCounts(long open, long critical) {}

    public OrderCounts orderCounts(LocalDateTime yesterday, LocalDateTime today, LocalDateTime tomorrow) {
        return jdbc.queryForObject("""
            WITH submissions AS (
                SELECT trade_order_id, MIN(time_updated) AS submitted_at
                FROM trade_order_status WHERE status = 'SUBMITTED' GROUP BY trade_order_id
            ), latest_status AS (
                SELECT DISTINCT ON (trade_order_id) trade_order_id, status
                FROM trade_order_status
                ORDER BY trade_order_id, time_updated DESC, status_id DESC
            )
            SELECT COUNT(*) FILTER (WHERE s.submitted_at >= :today) AS today,
                   COUNT(*) FILTER (WHERE s.submitted_at < :today) AS yesterday,
                   COUNT(*) FILTER (WHERE s.submitted_at >= :today AND l.status = 'FILLED') AS filled,
                   COUNT(DISTINCT c.user_id) FILTER (WHERE s.submitted_at >= :today) AS active_users
            FROM submissions s
            JOIN trade_orders o ON o.trade_order_id = s.trade_order_id
            JOIN accounts a ON a.account_id = o.account_id
            JOIN clients c ON c.clients_id = a.client_id
            JOIN latest_status l ON l.trade_order_id = o.trade_order_id
            WHERE s.submitted_at >= :yesterday AND s.submitted_at < :tomorrow
            """, new MapSqlParameterSource()
                .addValue("yesterday", yesterday).addValue("today", today).addValue("tomorrow", tomorrow),
            (row, index) -> new OrderCounts(row.getLong("today"), row.getLong("yesterday"),
                row.getLong("filled"), row.getLong("active_users")));
    }

    public DisputeCounts disputeCounts(String username) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) AS open,
                   COUNT(*) FILTER (WHERE ROUND(o.quantity * o.price, 2) >= :threshold) AS critical
            FROM disputes d
            JOIN trade_orders o ON o.trade_order_id = d.trade_order_id
            JOIN accounts a ON a.account_id = o.account_id
            JOIN clients c ON c.clients_id = a.client_id
            JOIN users u ON u.user_id = c.user_id
            WHERE d.status = 'OPEN' AND u.username <> :username
            """, Map.of("username", username, "threshold", DisputeSeverityPolicy.CRITICAL_TRADE_VALUE),
            (row, index) -> new DisputeCounts(row.getLong("open"), row.getLong("critical")));
    }

    public List<CurrencyTotal> assetsMoved(LocalDateTime today, LocalDateTime tomorrow) {
        return jdbc.query("""
            SELECT i.currency_code, SUM(o.trade_value) AS total_value
            FROM trade_orders o
            JOIN instruments i ON i.instrument_id = o.instrument_id
            JOIN (
                SELECT trade_order_id, MIN(time_updated) AS filled_at
                FROM trade_order_status WHERE status = 'FILLED' GROUP BY trade_order_id
            ) fills ON fills.trade_order_id = o.trade_order_id
            WHERE fills.filled_at >= :today AND fills.filled_at < :tomorrow
            GROUP BY i.currency_code ORDER BY i.currency_code
            """, new MapSqlParameterSource().addValue("today", today).addValue("tomorrow", tomorrow),
            (row, index) -> new CurrencyTotal(row.getString("currency_code"), row.getBigDecimal("total_value")));
    }
}
