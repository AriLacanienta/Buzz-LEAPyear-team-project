ALTER TABLE analytics_trade_events
    ADD COLUMN IF NOT EXISTS client_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3);