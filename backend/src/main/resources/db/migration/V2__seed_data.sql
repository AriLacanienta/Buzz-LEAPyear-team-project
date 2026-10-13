-- Demo users use the same bcrypt hash as the CSV importer.
INSERT INTO users (username, user_first_name, user_last_name, email, password_hash)
VALUES
    ('C001', 'Alice', 'Chen', 'C001@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C002', 'Ben', 'Whitfield', 'C002@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C003', 'Chidi', 'Nwosu', 'C003@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C004', 'Diane', 'Kowalski', 'C004@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C005', 'Elliot', 'Park', 'C005@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C006', 'Farida', 'Haidari', 'C006@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C007', 'Grace', 'Lindqvist', 'C007@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C008', 'Hassan', 'Malik', 'C008@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C009', 'Isla', 'Fraser', 'C009@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi'),
    ('C010', 'Jamal', 'Ferris', 'C010@trading.demo', '$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi')
ON CONFLICT (username) DO NOTHING;

INSERT INTO roles (role_name, description)
VALUES
    ('CLIENT', 'Client role - users who manage their own portfolios and trade'),
    ('ANALYST', 'Analyst role - users who analyze market data and create recommendations'),
    ('OPERATIONS', 'Operations role - users who manage operational processes and workflows'),
    ('COMPLIANCE', 'Compliance role - users who oversee regulatory compliance and audit trails')
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.user_id, r.role_id, 'flyway-v2'
FROM users u
JOIN roles r ON r.role_name = 'CLIENT'
WHERE u.username LIKE 'C%'
    AND NOT EXISTS (
            SELECT 1
            FROM user_roles ur
            WHERE ur.user_id = u.user_id
                AND ur.role_id = r.role_id
    );

INSERT INTO clients (user_id)
SELECT user_id
FROM users
WHERE username LIKE 'C%'
  AND NOT EXISTS (SELECT 1 FROM clients c WHERE c.user_id = users.user_id);

INSERT INTO accounts (account_name, client_id, risk_profile, cash_available, cash_reserved)
SELECT u.username || '_Account', c.clients_id, 'MODERATE', 10000.00, 0.00
FROM users u
JOIN clients c ON c.user_id = u.user_id
WHERE u.username LIKE 'C%'
  AND NOT EXISTS (SELECT 1 FROM accounts a WHERE a.client_id = c.clients_id);

INSERT INTO instruments (instrument_name, instrument_symbol, asset_type, currency_code)
VALUES
    ('Apple Inc.', 'AAPL', 'EQUITY', 'USD'),
    ('Microsoft Corporation', 'MSFT', 'EQUITY', 'USD'),
    ('Alphabet Inc.', 'GOOGL', 'EQUITY', 'USD'),
    ('Tesla Inc.', 'TSLA', 'EQUITY', 'USD'),
    ('Amazon.com Inc.', 'AMZN', 'EQUITY', 'USD'),
    ('NVIDIA Corporation', 'NVDA', 'EQUITY', 'USD'),
    ('Meta Platforms Inc.', 'META', 'EQUITY', 'USD'),
    ('Berkshire Hathaway Inc.', 'BRK.B', 'EQUITY', 'USD'),
    ('JPMorgan Chase & Co.', 'JPM', 'EQUITY', 'USD'),
    ('Johnson & Johnson', 'JNJ', 'EQUITY', 'USD'),
    ('Visa Inc.', 'V', 'EQUITY', 'USD'),
    ('Walmart Inc.', 'WMT', 'EQUITY', 'USD'),
    ('Procter & Gamble Company', 'PG', 'EQUITY', 'USD'),
    ('UnitedHealth Group Inc.', 'UNH', 'EQUITY', 'USD'),
    ('Verizon Communications Inc.', 'VZ', 'EQUITY', 'USD'),
    ('The Coca-Cola Company', 'KO', 'EQUITY', 'USD'),
    ('Pfizer Inc.', 'PFE', 'EQUITY', 'USD'),
    ('Adobe Inc.', 'ADBE', 'EQUITY', 'USD'),
    ('Costco Wholesale Corporation', 'COST', 'EQUITY', 'USD'),
    ('HSBC Holdings', 'HSBC', 'EQUITY', 'GBP'),
    ('Barclays', 'BARC', 'EQUITY', 'GBP'),
    ('Unilever', 'ULVR', 'EQUITY', 'GBP'),
    ('AstraZeneca', 'AZN', 'EQUITY', 'GBP'),
    ('Shell', 'SHEL', 'EQUITY', 'GBP'),
    ('Diageo', 'DGE', 'EQUITY', 'GBP'),
    ('Tata Consultancy Services', 'TCS', 'EQUITY', 'INR'),
    ('Infosys', 'INFY', 'EQUITY', 'INR'),
    ('Reliance Industries', 'RELIANCE', 'EQUITY', 'INR'),
    ('HDFC Bank', 'HDFCBANK', 'EQUITY', 'INR'),
    ('Wipro', 'WIPRO', 'EQUITY', 'INR'),
    ('Bajaj Auto', 'BAJAJAUT', 'EQUITY', 'INR'),
    ('US Treasury 10Y', 'USDT', 'BOND', 'USD'),
    ('Apple Corporate Bond', 'APPL-B', 'BOND', 'USD'),
    ('UK Gilts 10Y', 'GILT10Y', 'BOND', 'GBP'),
    ('Reliance Corporate Bond', 'RELY-B', 'BOND', 'INR'),
    ('Bitcoin', 'BTC', 'CRYPTO', 'USD'),
    ('Ethereum', 'ETH', 'CRYPTO', 'USD'),
    ('Ripple', 'XRP', 'CRYPTO', 'USD'),
    ('Vanguard S&P 500 ETF', 'VOO', 'ETF', 'USD'),
    ('Invesco QQQ Trust', 'QQQ', 'ETF', 'USD'),
    ('iShares Core FTSE 100', 'ISF', 'ETF', 'GBP'),
    ('Nifty 50 ETF', 'NIFTYBEES', 'ETF', 'INR'),
    ('VUSA', 'VUSA', 'ETF', 'GBP')
ON CONFLICT (instrument_symbol) DO NOTHING;

INSERT INTO quotes (instrument_id, price, high_price_of_day, low_price_of_day,
                    open_price_of_day, previous_close_price_of_day, change_amount,
                    change_percent, volume, market_cap, timestamp)
SELECT i.instrument_id, quote_data.price, quote_data.high_price_of_day,
       quote_data.low_price_of_day, quote_data.open_price_of_day,
       quote_data.previous_close_price_of_day, quote_data.change_amount,
    quote_data.change_percent, quote_data.volume, quote_data.market_cap,
    quote_data.timestamp::timestamp
FROM (VALUES
    (1, 175.50, 176.25, 174.80, 175.00, 174.25, 1.25, 0.72, 52500000, 2750000000000, '2026-01-15T10:30:00'),
    (2, 425.50, 428.00, 424.25, 426.00, 424.50, 1.00, 0.24, 45000000, 2850000000000, '2026-01-15T10:30:00'),
    (3, 182.50, 185.00, 181.75, 183.00, 181.25, 1.25, 0.69, 38000000, 1200000000000, '2026-01-15T10:30:00'),
    (4, 245.75, 248.50, 242.00, 244.00, 241.50, 4.25, 1.76, 52000000, 780000000000, '2026-01-15T10:30:00'),
    (5, 178.25, 180.50, 177.00, 178.75, 176.50, 1.75, 0.99, 48000000, 1750000000000, '2026-01-15T10:30:00'),
    (6, 892.50, 905.00, 885.00, 890.00, 880.00, 12.50, 1.42, 35000000, 2200000000000, '2026-01-15T10:30:00'),
    (7, 498.75, 505.00, 495.00, 500.00, 492.50, 6.25, 1.27, 42000000, 1280000000000, '2026-01-15T10:30:00'),
    (8, 545.25, 550.00, 540.00, 543.00, 541.00, 4.25, 0.79, 28000000, 1650000000000, '2026-01-15T10:30:00'),
    (9, 198.50, 202.00, 197.00, 200.00, 195.50, 3.00, 1.53, 55000000, 570000000000, '2026-01-15T10:30:00'),
    (10, 162.75, 165.00, 161.50, 163.00, 160.75, 2.00, 1.24, 38000000, 380000000000, '2026-01-15T10:30:00'),
    (11, 265.50, 268.00, 263.50, 265.00, 262.50, 3.00, 1.14, 42000000, 560000000000, '2026-01-15T10:30:00'),
    (12, 92.75, 94.00, 91.50, 93.00, 91.25, 1.50, 1.64, 65000000, 245000000000, '2026-01-15T10:30:00'),
    (13, 168.25, 170.50, 167.00, 169.00, 166.75, 1.50, 0.90, 35000000, 455000000000, '2026-01-15T10:30:00'),
    (14, 425.75, 430.00, 422.50, 427.00, 421.50, 4.25, 1.01, 48000000, 435000000000, '2026-01-15T10:30:00'),
    (15, 45.50, 46.50, 44.75, 45.75, 44.50, 1.00, 2.25, 72000000, 220000000000, '2026-01-15T10:30:00'),
    (16, 32.75, 33.50, 32.00, 32.50, 32.10, 0.65, 2.02, 88000000, 130000000000, '2026-01-15T10:30:00'),
    (17, 685.50, 695.00, 680.00, 688.00, 675.50, 10.00, 1.48, 41000000, 210000000000, '2026-01-15T10:30:00'),
    (18, 525.25, 535.00, 520.00, 528.00, 518.50, 6.75, 1.30, 33000000, 245000000000, '2026-01-15T10:30:00'),
    (19, 38.50, 39.75, 37.25, 38.75, 37.50, 1.00, 2.67, 95000000, 140000000000, '2026-01-15T10:30:00'),
    (20, 28.75, 29.50, 28.00, 28.50, 27.75, 1.00, 3.60, 105000000, 115000000000, '2026-01-15T10:30:00'),
    (21, 35.25, 36.00, 34.80, 35.50, 34.75, 0.50, 1.44, 38000000, 280000000000, '2026-01-15T10:30:00'),
    (22, 125.50, 128.00, 124.50, 126.00, 124.75, 0.75, 0.60, 42000000, 1900000000000, '2026-01-15T10:30:00'),
    (23, 52.75, 54.00, 52.00, 53.00, 51.75, 1.00, 1.93, 65000000, 420000000000, '2026-01-15T10:30:00'),
    (24, 168.25, 171.00, 167.50, 169.00, 167.00, 1.25, 0.75, 48000000, 690000000000, '2026-01-15T10:30:00'),
    (25, 215.50, 218.00, 213.50, 214.75, 212.50, 3.00, 1.41, 55000000, 1100000000000, '2026-01-15T10:30:00'),
    (26, 145.75, 148.50, 144.00, 146.50, 143.50, 2.25, 1.57, 52000000, 850000000000, '2026-01-15T10:30:00'),
    (27, 185.50, 188.00, 183.75, 186.00, 184.25, 1.25, 0.68, 47000000, 920000000000, '2026-01-15T10:30:00'),
    (28, 618.50, 625.00, 615.00, 620.00, 612.50, 6.00, 0.98, 38000000, 2450000000000, '2026-01-15T10:30:00'),
    (29, 245.75, 250.00, 243.50, 247.00, 242.75, 3.00, 1.24, 58000000, 680000000000, '2026-01-15T10:30:00'),
    (20, 42.50, 43.75, 41.50, 42.75, 41.25, 1.25, 3.03, 15000000, 850000000000, '2026-01-15T10:30:00'),
    (21, 28.75, 29.50, 28.00, 28.50, 28.25, 0.50, 1.77, 12000000, 280000000000, '2026-01-15T10:30:00'),
    (22, 35.25, 36.00, 34.75, 35.50, 34.75, 0.50, 1.44, 18000000, 420000000000, '2026-01-15T10:30:00'),
    (23, 45.80, 47.00, 45.00, 46.00, 45.25, 0.55, 1.21, 22000000, 950000000000, '2026-01-15T10:30:00'),
    (24, 18.90, 19.50, 18.50, 18.75, 18.50, 0.40, 2.16, 25000000, 180000000000, '2026-01-15T10:30:00'),
    (25, 38.50, 39.75, 37.80, 38.75, 38.00, 0.50, 1.32, 20000000, 580000000000, '2026-01-15T10:30:00'),
    (26, 3285, 3400, 3250, 3300, 3260, 25, 0.78, 48000000, 1200000000000, '2026-01-15T10:30:00'),
    (27, 1625, 1700, 1600, 1650, 1615, 10, 0.66, 42000000, 780000000000, '2026-01-15T10:30:00'),
    (28, 2450, 2550, 2400, 2480, 2440, 10, 0.42, 38000000, 1650000000000, '2026-01-15T10:30:00'),
    (29, 1525, 1600, 1500, 1550, 1510, 15, 1.03, 35000000, 950000000000, '2026-01-15T10:30:00'),
    (30, 825, 850, 810, 835, 815, 10, 1.32, 40000000, 420000000000, '2026-01-15T10:30:00'),
    (31, 1950, 2000, 1920, 1970, 1940, 10, 0.53, 50000000, 580000000000, '2026-01-15T10:30:00'),
    (32, 102.50, 103.25, 101.75, 102.00, 101.50, 1.00, 0.98, 2500000, 1200000000000, '2026-01-15T10:30:00'),
    (33, 105.75, 106.50, 104.75, 105.25, 105.00, 0.75, 0.71, 1800000, 850000000000, '2026-01-15T10:30:00'),
    (34, 98.25, 99.50, 97.50, 98.75, 97.50, 0.75, 0.77, 2200000, 950000000000, '2026-01-15T10:30:00'),
    (35, 101.50, 102.25, 100.75, 101.25, 101.00, 0.50, 0.49, 1950000, 780000000000, '2026-01-15T10:30:00'),
    (36, 43250.50, 44000.00, 42500.00, 43500.00, 42500.00, 750.50, 1.76, 125000, 850000000000, '2026-01-15T10:30:00'),
    (37, 2285.75, 2350.00, 2250.00, 2300.00, 2250.00, 35.75, 1.59, 185000, 275000000000, '2026-01-15T10:30:00'),
    (38, 2.45, 2.55, 2.35, 2.40, 2.38, 0.07, 2.94, 5500000, 125000000000, '2026-01-15T10:30:00'),
    (39, 485.50, 492.00, 482.00, 487.50, 480.75, 4.75, 0.99, 22000000, 1950000000000, '2026-01-15T10:30:00'),
    (40, 375.25, 382.00, 372.50, 378.00, 371.50, 3.75, 1.01, 18500000, 1520000000000, '2026-01-15T10:30:00'),
    (41, 5485.75, 5650.00, 5400.00, 5550.00, 5425.00, 60.75, 1.12, 8500000, 2100000000000, '2026-01-15T10:30:00'),
    (42, 285, 295, 280, 288, 282, 3, 1.09, 12000000, 680000000000, '2026-01-15T10:30:00')
) AS quote_data(instrument_id, price, high_price_of_day, low_price_of_day,
                open_price_of_day, previous_close_price_of_day, change_amount,
                change_percent, volume, market_cap, timestamp)
JOIN (
    SELECT instrument_id,
           ROW_NUMBER() OVER (ORDER BY instrument_id) AS source_instrument_id
    FROM instruments
) i ON i.source_instrument_id = quote_data.instrument_id
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    trade RECORD;
    account_id_value BIGINT;
    instrument_id_value BIGINT;
    trade_order_id_value BIGINT;
    holding_id_value BIGINT;
    holding_created BOOLEAN;
BEGIN
    FOR trade IN
        SELECT *
        FROM (VALUES
            ('C001', 'AAPL', 'BUY', 120.0, 185.32, 22238.40, '2026-01-05 00:00:00'::timestamp),
            ('C002', 'MSFT', 'BUY', 60.0, 402.11, 24126.60, '2026-01-05 00:00:00'::timestamp),
            ('C003', 'USDT', 'BUY', 5000.0, 98.75, 4937.50, '2026-01-06 00:00:00'::timestamp),
            ('C001', 'AAPL', 'SELL', 40.0, 186.10, 7444.00, '2026-01-06 00:00:00'::timestamp),
            ('C004', 'GOOGL', 'BUY', 25.0, 141.87, 3546.75, '2026-01-07 00:00:00'::timestamp),
            ('C005', 'VUSA', 'BUY', 300.0, 84.22, 25266.00, '2026-01-07 00:00:00'::timestamp),
            ('C002', 'MSFT', 'SELL', 20.0, 404.55, 8091.00, '2026-01-08 00:00:00'::timestamp),
            ('C006', 'BTC', 'BUY', 0.5, 42000.00, 21000.00, '2026-01-08 00:00:00'::timestamp),
            ('C003', 'USDT', 'BUY', 2000.0, 98.90, 1978.00, '2026-01-09 00:00:00'::timestamp),
            ('C007', 'TSLA', 'BUY', 15.0, 238.44, 3576.60, '2026-01-09 00:00:00'::timestamp),
            ('C001', 'VUSA', 'BUY', 100.0, 84.10, 8410.00, '2026-01-12 00:00:00'::timestamp),
            ('C008', 'MSFT', 'BUY', 10.0, 405.20, 4052.00, '2026-01-12 00:00:00'::timestamp),
            ('C004', 'GOOGL', 'SELL', 10.0, 143.02, 1430.20, '2026-01-13 00:00:00'::timestamp),
            ('C009', 'BTC', 'SELL', 0.2, 41500.00, 8300.00, '2026-01-13 00:00:00'::timestamp),
            ('C005', 'VUSA', 'SELL', 50.0, 85.01, 4250.50, '2026-01-14 00:00:00'::timestamp),
            ('C010', 'AAPL', 'BUY', 200.0, 187.55, 37510.00, '2026-01-14 00:00:00'::timestamp),
            ('C002', 'USDT', 'BUY', 3000.0, 99.10, 2973.00, '2026-01-15 00:00:00'::timestamp),
            ('C006', 'ETH', 'BUY', 4.0, 2450.00, 9800.00, '2026-01-15 00:00:00'::timestamp),
            ('C003', 'TSLA', 'BUY', 8.0, 241.20, 1929.60, '2026-01-16 00:00:00'::timestamp),
            ('C007', 'TSLA', 'SELL', 5.0, 239.90, 1199.50, '2026-01-16 00:00:00'::timestamp)
        ) AS values(client_username, instrument_symbol, side, quantity, price, trade_value, order_date)
    LOOP
        holding_created := FALSE;

        SELECT a.account_id INTO account_id_value
        FROM accounts a
        JOIN clients c ON c.clients_id = a.client_id
        JOIN users u ON u.user_id = c.user_id
        WHERE u.username = trade.client_username;

        SELECT i.instrument_id INTO instrument_id_value
        FROM instruments i
        WHERE i.instrument_symbol = trade.instrument_symbol;

        INSERT INTO trade_orders (account_id, instrument_id, side, quantity, price, trade_value, order_date)
        VALUES (account_id_value, instrument_id_value, trade.side, trade.quantity, trade.price, trade.trade_value, trade.order_date)
        RETURNING trade_order_id INTO trade_order_id_value;

        INSERT INTO trade_order_status (trade_order_id, status, time_updated)
        VALUES (trade_order_id_value, 'FILLED', trade.order_date);

        SELECT h.holdings_id INTO holding_id_value
        FROM holdings h
        WHERE h.account_id = account_id_value
          AND h.instrument_id = instrument_id_value;

        IF NOT FOUND THEN
            INSERT INTO holdings (account_id, instrument_id, quantity, total_cost)
            VALUES (account_id_value, instrument_id_value,
                    CASE WHEN trade.side = 'SELL' THEN 1.0 ELSE trade.quantity END,
                    trade.trade_value)
            RETURNING holdings_id INTO holding_id_value;
            holding_created := TRUE;
        END IF;

        IF NOT holding_created AND trade.side = 'BUY' THEN
            UPDATE holdings
            SET quantity = quantity + trade.quantity
            WHERE holdings_id = holding_id_value;
        ELSIF NOT holding_created THEN
            UPDATE holdings
            SET quantity = quantity - trade.quantity
            WHERE holdings_id = holding_id_value;

            DELETE FROM holdings
            WHERE holdings_id = holding_id_value
              AND quantity <= 0;
        END IF;
    END LOOP;
END $$;
