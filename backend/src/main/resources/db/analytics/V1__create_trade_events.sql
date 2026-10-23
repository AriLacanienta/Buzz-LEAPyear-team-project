CREATE TABLE IF NOT EXISTS analytics_trade_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(50) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity NUMERIC(20, 4) NOT NULL,
    price NUMERIC(20, 2) NOT NULL,
    trade_value NUMERIC(20, 2) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(50),
    status_time TIMESTAMP,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_analytics_trade_report
    ON analytics_trade_events (status, status_time, instrument_symbol);
