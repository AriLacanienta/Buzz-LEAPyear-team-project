ALTER TABLE instruments
    ADD COLUMN trading_session VARCHAR(20) NOT NULL DEFAULT 'US_MARKET'
        CHECK (trading_session IN ('US_MARKET', 'UK_MARKET', 'INDIA_MARKET', 'ALWAYS_OPEN')),
    ADD COLUMN whole_shares_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fractional_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN quantity_precision INT NOT NULL DEFAULT 6 CHECK (quantity_precision BETWEEN 0 AND 8),
    ADD COLUMN min_order_amount NUMERIC(20, 2) NOT NULL DEFAULT 1.00 CHECK (min_order_amount >= 0);

UPDATE instruments SET trading_session = 'UK_MARKET' WHERE currency_code = 'GBP';
UPDATE instruments SET trading_session = 'INDIA_MARKET' WHERE currency_code = 'INR';

UPDATE instruments
SET trading_session = 'ALWAYS_OPEN',
    whole_shares_allowed = FALSE,
    fractional_allowed = TRUE,
    quantity_precision = 8
WHERE asset_type = 'CRYPTO';

UPDATE instruments
SET fractional_allowed = FALSE,
    quantity_precision = 0
WHERE asset_type = 'BOND';

ALTER TABLE trade_orders ALTER COLUMN quantity TYPE NUMERIC(28, 8);
ALTER TABLE holdings ALTER COLUMN quantity TYPE NUMERIC(28, 8);

ALTER TABLE trade_orders
    ADD COLUMN quantity_mode VARCHAR(10) NOT NULL DEFAULT 'SHARES'
        CHECK (quantity_mode IN ('SHARES', 'AMOUNT')),
    ADD COLUMN requested_amount NUMERIC(20, 2)
        CHECK (requested_amount IS NULL OR requested_amount >= 0);