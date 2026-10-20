package com.buzzleapyear.trading_api.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import com.buzzleapyear.trading_api.dto.TradeActivityReportDto;
import org.springframework.beans.factory.annotation.Qualifier;

@Repository
public class TradeAnalyticsRepository {
    private final JdbcTemplate jdbcTemplate;

    public TradeAnalyticsRepository(@Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TradeActivityReportDto> findFilledTrades(
        LocalDate from,
        LocalDate to,
        String instrumentSymbol
    ) {
        return findTrades(from, to, instrumentSymbol, true);
    }

    public List<TradeActivityReportDto> findAllTrades(
        LocalDate from,
        LocalDate to,
        String instrumentSymbol
    ) {
        return findTrades(from, to, instrumentSymbol, false);
    }

    private List<TradeActivityReportDto> findTrades(
        LocalDate from,
        LocalDate to,
        String instrumentSymbol,
        boolean filledOnly
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT order_id,
                    instrument_symbol,
                    side,
                    quantity,
                    price,
                    trade_value,
                    order_date,
                    status,
                    status_time
                FROM analytics_trade_events
                WHERE status_time >= ?
                    AND status_time < ?
                """);
        
        List<Object> parameters = new ArrayList<>();
        parameters.add(Timestamp.valueOf(from.atStartOfDay()));
        parameters.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));

        if (filledOnly) {
            sql.append(" AND status = 'FILLED'");
        }

        if (instrumentSymbol != null && !instrumentSymbol.isBlank()) {
            sql.append(" AND instrument_symbol = ? ");
            parameters.add(instrumentSymbol);
        }

        sql.append("""
            ORDER BY status_time, order_id
                """);

        return jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> new TradeActivityReportDto(
                rs.getLong("order_id"),
                rs.getString("instrument_symbol"),
                rs.getString("side"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("trade_value"),
                rs.getTimestamp("order_date").toLocalDateTime(),
                rs.getString("status"),
                rs.getTimestamp("status_time").toLocalDateTime()
            ),
            parameters.toArray()
        );

    }
    
}
