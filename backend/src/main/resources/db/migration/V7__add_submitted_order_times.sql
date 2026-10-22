-- Add order placement times one minute before seeded filled orders.
-- The manager trade log reads ORDER TIME from the SUBMITTED status.
INSERT INTO trade_order_status (trade_order_id, status, time_updated)
SELECT filled.trade_order_id,
       'SUBMITTED',
    filled.filled_at - INTERVAL '1 minute'
FROM (
    SELECT trade_order_id, MIN(time_updated) AS filled_at
    FROM trade_order_status
    WHERE status = 'FILLED'
    GROUP BY trade_order_id
) AS filled
WHERE NOT EXISTS (
    SELECT 1
    FROM trade_order_status submitted
    WHERE submitted.trade_order_id = filled.trade_order_id
      AND submitted.status = 'SUBMITTED'
);
