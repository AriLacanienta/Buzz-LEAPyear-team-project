/*
-- User Details
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;

-- Trading data
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS quotes CASCADE;
DROP TABLE IF EXISTS pricing CASCADE;
DROP TABLE IF EXISTS trade_orders CASCADE;
DROP TABLE IF EXISTS trade_order_status CASCADE;
DROP TABLE IF EXISTS holdings CASCADE;

-- Model Portfolio subscriptions
DROP TABLE IF EXISTS model_portfolio CASCADE;
DROP TABLE IF EXISTS portfolio_holdings CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
*/
--=================================
-- MARK: User Details
--=================================

-- Users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    user_first_name VARCHAR(50) NOT NULL,
    user_last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User roles junction table
CREATE TABLE user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    CONSTRAINT unique_user_role UNIQUE (user_id, role_id)
);

-- Clients table
CREATE TABLE clients (
    clients_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clients_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Accounts table
CREATE TABLE accounts (
    account_id BIGSERIAL PRIMARY KEY,
    account_name VARCHAR(255) NOT NULL,
    client_id BIGINT NOT NULL,
    risk_profile VARCHAR(50) NOT NULL CHECK (risk_profile IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    cash_available NUMERIC(20, 2) NOT NULL DEFAULT 0,
    cash_reserved NUMERIC(20, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_accounts_client_id FOREIGN KEY (client_id) REFERENCES clients(clients_id) ON DELETE CASCADE
);

--=================================
-- MARK: Trading data
--=================================

-- Instruments table
CREATE TABLE instruments (
    instrument_id BIGSERIAL PRIMARY KEY,
    instrument_name VARCHAR(255) NOT NULL,
    instrument_symbol VARCHAR(50) NOT NULL UNIQUE,
    asset_type VARCHAR(50) NOT NULL CHECK (asset_type IN ('EQUITY', 'BOND', 'FUND', 'CASH', 'CRYPTO', 'ETF')),
    currency_code VARCHAR(3) NOT NULL
);

-- Quotes Table (real-time market data)
CREATE TABLE quotes (
    quote_id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    price NUMERIC(20, 2) NOT NULL,
    high_price_of_day NUMERIC(20, 2) NOT NULL,
    low_price_of_day NUMERIC(20, 2) NOT NULL,
    open_price_of_day NUMERIC(20, 2) NOT NULL,
    previous_close_price_of_day NUMERIC(20, 2) NOT NULL,
    change_amount NUMERIC(20, 2) NOT NULL,
    change_percent NUMERIC(20, 4) NOT NULL,
    volume BIGINT NOT NULL,
    market_cap BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_quotes_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

-- Pricing Table
CREATE TABLE pricing (
    pricing_id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    price NUMERIC(20, 2) NOT NULL CHECK (price >= 0),
    price_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_pricing_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

-- Trade orders table
CREATE TABLE trade_orders (
    trade_order_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity NUMERIC(20, 4) NOT NULL CHECK (quantity > 0),
    price NUMERIC(20, 2) NOT NULL CHECK (price > 0),
    trade_value NUMERIC(20, 2) NOT NULL CHECK (trade_value > 0),
    order_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_trade_orders_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_orders_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

-- Trade order status table
CREATE TABLE trade_order_status (
    status_id BIGSERIAL PRIMARY KEY,
    trade_order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('SUBMITTED', 'VALIDATED', 'FILLED', 'REJECTED')),
    time_updated TIMESTAMP NOT NULL,
    reason_text VARCHAR(500),
    CONSTRAINT fk_trade_order_status_trade_order_id FOREIGN KEY (trade_order_id) REFERENCES trade_orders(trade_order_id) ON DELETE CASCADE
);

-- Holdings table
CREATE TABLE holdings (
    holdings_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    quantity NUMERIC(20, 4) NOT NULL CHECK (quantity > 0),
    total_cost NUMERIC(20, 4) NOT NULL CHECK (total_cost >= 0),
    CONSTRAINT fk_holdings_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_holdings_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    CONSTRAINT unique_account_instrument UNIQUE (account_id, instrument_id)
);

--=================================
-- MARK: Subscriptions
--=================================

-- Model portfolio table
CREATE TABLE model_portfolio (
    portfolio_id BIGSERIAL PRIMARY KEY,
    portfolio_name VARCHAR(255) NOT NULL UNIQUE
);

-- Subscriptions table
CREATE TABLE subscriptions (
    subscription_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    portfolio_id BIGINT,
    subscription_start_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_subscriptions_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT unique_account_portfolio UNIQUE (account_id, portfolio_id)
);

-- Portfolio holdings table
CREATE TABLE portfolio_holdings (
    holdings_id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    holding_weight NUMERIC(20, 4) NOT NULL CHECK (holding_weight >= 0),
    CONSTRAINT fk_portfolio_holdings_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_holdings_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    CONSTRAINT unique_portfolio_instrument UNIQUE (portfolio_id, instrument_id)
);

--=================================
-- MARK: Misc
--=================================

-- Create indexes for performance
CREATE INDEX idx_quote_timestamp ON quotes(instrument_id, timestamp DESC);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
