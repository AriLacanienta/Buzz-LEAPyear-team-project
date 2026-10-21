package com.buzzleapyear.trading_api.service;

import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.buzzleapyear.trading_api.messaging.TradeOrderEvent;

@Component
public class TradingAnalyticsConsumer {
    
    private final JdbcTemplate analyticsJdbcTemplate;
    private final ObjectMapper objectMapper;

    public TradingAnalyticsConsumer(@Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate, ObjectMapper objectMapper) {
        this.analyticsJdbcTemplate = analyticsJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.trade-order-topic}")
    public void consume(String json) {
        final TradeOrderEvent event;
        try {
            event = objectMapper.readValue(json, TradeOrderEvent.class);
        } catch (JsonProcessingException e) {
           throw new IllegalArgumentException("Invalid trade-order kafka event", e);
        }

        analyticsJdbcTemplate.update("""
            INSERT INTO analytics_trade_events (
            event_id, event_type, order_id, account_id, client_id, instrument_id,
            client_name, account_name, instrument_symbol, asset_type, currency_code,
            side, quantity, price, trade_value,
            order_date, status, status_time, reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
            event.eventId(),
            event.eventType(),
            event.orderId(),
            event.accountId(),
            event.clientId(),
            event.instrumentId(),
            event.clientName(),
            event.accountName(),
            event.instrumentSymbol(),
            event.assetType(),
            event.currencyCode(),
            event.side(),
            event.quantity(),
            event.price(),
            event.tradeValue(),
            event.orderDate(),
            event.status(),
            event.statusTime(),
            event.reason()
        );
    }
}
