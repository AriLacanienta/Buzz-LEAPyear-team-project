package com.buzzleapyear.trading_api.repository;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.buzzleapyear.trading_api.dto.TradeActivityReportDto;
import com.buzzleapyear.trading_api.dto.AnalystTradeResponseDto;
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

    public Page<AnalystTradeResponseDto> findAnalystTrades(
        LocalDate from,
        LocalDate to,
        String search,
        Pageable pageable
    ) {
        String filter = """
            AND order_date >= ? AND order_date < ?
                AND (? = '' OR CAST(order_id AS TEXT) ILIKE '%' || ? || '%'
                    OR COALESCE(client_name, '') ILIKE '%' || ? || '%'
                    OR COALESCE(account_name, '') ILIKE '%' || ? || '%'
                    OR instrument_symbol ILIKE '%' || ? || '%')
            """;
        String latestEvents = """
            WITH latest_events AS (
                SELECT *, ROW_NUMBER() OVER (
                    PARTITION BY order_id
                    ORDER BY status_time DESC NULLS LAST, event_id DESC
                ) AS row_number
                FROM analytics_trade_events
            )
            SELECT order_id, account_id, account_name, client_name,
                   instrument_symbol, asset_type, currency_code, side,
                   quantity, price, trade_value, order_date, status,
                   status_time, reason
            FROM latest_events
            WHERE row_number = 1
            """;
        String dataSql = latestEvents + filter
            + " ORDER BY order_date DESC, order_id DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM (" + latestEvents + filter + ") trades";

        String normalizedSearch = search == null ? "" : search.trim();
        List<Object> parameters = List.of(
            Timestamp.valueOf(from.atStartOfDay()),
            Timestamp.valueOf(to.plusDays(1).atStartOfDay()),
            normalizedSearch, normalizedSearch, normalizedSearch, normalizedSearch, normalizedSearch
        );
        List<Object> dataParameters = new ArrayList<>(parameters);
        dataParameters.add(pageable.getPageSize());
        dataParameters.add(pageable.getOffset());

        List<AnalystTradeResponseDto> trades = jdbcTemplate.query(
            dataSql,
            (rs, rowNum) -> new AnalystTradeResponseDto(
                rs.getLong("order_id"),
                rs.getLong("account_id"),
                rs.getString("account_name"),
                rs.getString("client_name"),
                rs.getString("instrument_symbol"),
                parseAssetType(rs.getString("asset_type")),
                rs.getString("currency_code"),
                parseSide(rs.getString("side")),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("trade_value"),
                rs.getTimestamp("order_date").toLocalDateTime(),
                parseStatus(rs.getString("status")),
                rs.getTimestamp("status_time") == null ? null : rs.getTimestamp("status_time").toLocalDateTime(),
                rs.getString("reason")
            ),
            dataParameters.toArray()
        );
        long total = jdbcTemplate.queryForObject(countSql, Long.class, parameters.toArray());
        return new PageImpl<>(trades, pageable, total);
    }

    private com.buzzleapyear.trading_api.entity.Instrument.AssetType parseAssetType(String value) {
        return value == null ? null : com.buzzleapyear.trading_api.entity.Instrument.AssetType.valueOf(value);
    }

    private com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide parseSide(String value) {
        return value == null ? null : com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide.valueOf(value);
    }

    private com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus parseStatus(String value) {
        return value == null ? null : com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus.valueOf(value);
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
