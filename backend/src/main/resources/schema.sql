
-- Flyway migration: Schema aligned with JPA entity definitions
-- This schema replaces ddl-auto=create-drop approach with explicit Flyway migrations

DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS trade_order_status CASCADE;
DROP TABLE IF EXISTS holdings CASCADE;
DROP TABLE IF EXISTS portfolio_holdings CASCADE;
DROP TABLE IF EXISTS trade_orders CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS quotes CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS compliance CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS model_portfolio CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Users table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
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
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User roles junction table
CREATE TABLE user_roles (
    user_role_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    CONSTRAINT unique_user_role UNIQUE (user_id, role_id)
);

-- Clients table
CREATE TABLE clients (
    clients_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clients_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Compliance table
CREATE TABLE compliance (
    compliance_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    compliance_status VARCHAR(50) NOT NULL CHECK (compliance_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_compliance_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Accounts table
CREATE TABLE accounts (
    account_id SERIAL PRIMARY KEY,
    account_name VARCHAR(255) NOT NULL,
    client_id INT NOT NULL,
    risk_profile VARCHAR(50) NOT NULL CHECK (risk_profile IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    cash_available NUMERIC(20, 2) NOT NULL DEFAULT 0,
    cash_reserved NUMERIC(20, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_accounts_client_id FOREIGN KEY (client_id) REFERENCES clients(clients_id) ON DELETE CASCADE
);

-- Instruments table
CREATE TABLE instruments (
    instrument_id SERIAL PRIMARY KEY,
    instrument_name VARCHAR(255) NOT NULL,
    instrument_symbol VARCHAR(50) NOT NULL UNIQUE,
    asset_type VARCHAR(50) NOT NULL CHECK (asset_type IN ('EQUITY', 'BOND', 'FUND', 'CASH', 'CRYPTO', 'ETF')),
    currency_code VARCHAR(3) NOT NULL
);

-- Model portfolio table
CREATE TABLE model_portfolio (
    portfolio_id SERIAL PRIMARY KEY,
    portfolio_name VARCHAR(255) NOT NULL UNIQUE
);

-- Subscriptions table
CREATE TABLE subscriptions (
    subscription_id SERIAL PRIMARY KEY,
    account_id INT,
    portfolio_id INT,
    subscription_start_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_subscriptions_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT unique_account_portfolio UNIQUE (account_id, portfolio_id)
);

-- Trade orders table
CREATE TABLE trade_orders (
    trade_order_id SERIAL PRIMARY KEY,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
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
    status_id SERIAL PRIMARY KEY,
    trade_order_id INT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('SUBMITTED', 'VALIDATED', 'FILLED', 'REJECTED')),
    time_updated TIMESTAMP NOT NULL,
    reason_text VARCHAR(500),
    CONSTRAINT fk_trade_order_status_trade_order_id FOREIGN KEY (trade_order_id) REFERENCES trade_orders(trade_order_id) ON DELETE CASCADE
);

-- Holdings table
CREATE TABLE holdings (
    holdings_id SERIAL PRIMARY KEY,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
    quantity NUMERIC(20, 4) NOT NULL CHECK (quantity > 0),
    total_cost NUMERIC(20, 4) NOT NULL CHECK (total_cost >= 0),
    CONSTRAINT fk_holdings_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT fk_holdings_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    CONSTRAINT unique_account_instrument UNIQUE (account_id, instrument_id)
);

-- Portfolio holdings table
CREATE TABLE portfolio_holdings (
    holdings_id SERIAL PRIMARY KEY,
    portfolio_id INT NOT NULL,
    instrument_id INT NOT NULL,
    holding_weight NUMERIC(20, 4) NOT NULL CHECK (holding_weight >= 0),
    CONSTRAINT fk_portfolio_holdings_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id) ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_holdings_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    CONSTRAINT unique_portfolio_instrument UNIQUE (portfolio_id, instrument_id)
);

-- Reports table
CREATE TABLE reports (
    report_id SERIAL PRIMARY KEY,
    report_name VARCHAR(255) NOT NULL UNIQUE,
    report_date TIMESTAMP NOT NULL,
    user_id INT NOT NULL,
    compliance_id INT NOT NULL UNIQUE,
    CONSTRAINT fk_reports_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_compliance_id FOREIGN KEY (compliance_id) REFERENCES compliance(compliance_id) ON DELETE CASCADE
);

-- Audit log table
CREATE TABLE audit_log (
    audit_id SERIAL PRIMARY KEY,
    report_id INT NOT NULL,
    client_id INT NOT NULL,
    account_id INT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    CONSTRAINT fk_audit_log_report_id FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_client_id FOREIGN KEY (client_id) REFERENCES clients(clients_id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);

-- Quotes table
CREATE TABLE quotes (
    quote_id SERIAL PRIMARY KEY,
    instrument_id INT NOT NULL,
    price NUMERIC(20, 2) NOT NULL,
    high_price_of_day NUMERIC(20, 2) NOT NULL,
    low_price_of_day NUMERIC(20, 2) NOT NULL,
    open_price_of_day NUMERIC(20, 2) NOT NULL,
    previous_close_price_of_day NUMERIC(20, 2) NOT NULL,
    change_amount NUMERIC(20, 2) NOT NULL,
    change_percent NUMERIC(20, 2) NOT NULL,
    volume BIGINT NOT NULL,
    market_cap BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_quotes_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE
);

-- Index on quotes for common queries
CREATE INDEX idx_quote_timestamp ON quotes(instrument_id, timestamp DESC);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
CREATE INDEX idx_trade_orders_account_date ON trade_orders(account_id, order_date);