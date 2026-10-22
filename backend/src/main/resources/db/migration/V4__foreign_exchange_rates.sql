CREATE TABLE exchange_rates (
    exchange_rate_id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(20, 10) NOT NULL CHECK (rate > 0),
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT unique_exchange_rate_pair UNIQUE (base_currency, quote_currency)
);

INSERT INTO exchange_rates (base_currency, quote_currency, rate)
VALUES
    ('USD', 'USD', 1.0000000000),
    ('GBP', 'USD', 1.2700000000),
    ('INR', 'USD', 0.0120000000);

ALTER TABLE trade_orders
    ADD COLUMN native_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN exchange_rate NUMERIC(20, 10) NOT NULL DEFAULT 1.0000000000;
