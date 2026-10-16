-- Add the missing quote so VUSA is available to search and trade.
INSERT INTO quotes (
    instrument_id,
    price,
    high_price_of_day,
    low_price_of_day,
    open_price_of_day,
    previous_close_price_of_day,
    change_amount,
    change_percent,
    volume,
    market_cap,
    timestamp
)
SELECT
    i.instrument_id,
    84.22,
    85.01,
    84.10,
    84.22,
    84.00,
    0.22,
    0.26,
    1000000,
    10000000000,
    TIMESTAMP '2026-10-16 10:30:00'
FROM instruments i
WHERE i.instrument_symbol = 'VUSA'
  AND NOT EXISTS (
      SELECT 1
      FROM quotes q
      WHERE q.instrument_id = i.instrument_id
  );

-- Rebuild seeded holdings from filled orders using average-cost accounting.
DO $$
DECLARE
    position RECORD;
    trade RECORD;
    existing_holding_id BIGINT;
    running_quantity NUMERIC := 0;
    running_cost NUMERIC := 0;
    average_cost NUMERIC;
BEGIN
    FOR position IN
        SELECT DISTINCT t.account_id, t.instrument_id
        FROM trade_orders t
        JOIN accounts a ON a.account_id = t.account_id
        JOIN clients c ON c.clients_id = a.client_id
        JOIN users u ON u.user_id = c.user_id
        WHERE u.username LIKE 'C%'
          AND EXISTS (
              SELECT 1
              FROM trade_order_status tos
              WHERE tos.trade_order_id = t.trade_order_id
                AND tos.status = 'FILLED'
          )
    LOOP
        running_quantity := 0;
        running_cost := 0;

        FOR trade IN
            SELECT t.side, t.quantity, t.price, t.trade_value
            FROM trade_orders t
            WHERE t.account_id = position.account_id
              AND t.instrument_id = position.instrument_id
              AND EXISTS (
                  SELECT 1
                  FROM trade_order_status tos
                  WHERE tos.trade_order_id = t.trade_order_id
                    AND tos.status = 'FILLED'
              )
            ORDER BY t.order_date, t.trade_order_id
        LOOP
            IF trade.side = 'BUY' THEN
                running_quantity := running_quantity + trade.quantity;
                running_cost := running_cost + (trade.quantity * trade.price);
            ELSIF trade.side = 'SELL' AND running_quantity > 0 THEN
                average_cost := running_cost / running_quantity;
                running_quantity := running_quantity - trade.quantity;
                running_cost := running_cost - average_cost * trade.quantity;
            END IF;
        END LOOP;

        SELECT h.holdings_id
        INTO existing_holding_id
        FROM holdings h
        WHERE h.account_id = position.account_id
          AND h.instrument_id = position.instrument_id;

        IF running_quantity <= 0 THEN
            DELETE FROM holdings
            WHERE holdings_id = existing_holding_id;
        ELSIF existing_holding_id IS NULL THEN
            INSERT INTO holdings (account_id, instrument_id, quantity, total_cost)
            VALUES (position.account_id, position.instrument_id, running_quantity, running_cost);
        ELSE
            UPDATE holdings
            SET quantity = running_quantity,
                total_cost = running_cost
            WHERE holdings_id = existing_holding_id;
        END IF;
    END LOOP;
END $$;