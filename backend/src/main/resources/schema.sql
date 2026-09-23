
--schema file into the database, that destroys the tables and rebuilds them from scratch every single time using: JPA
--
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS model_portfolio CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS pricing CASCADE;
DROP TABLE IF EXISTS holdings CASCADE;
DROP TABLE IF EXISTS portfolio_holdings CASCADE;
DROP TABLE IF EXISTS trade_orders CASCADE;
DROP TABLE IF EXISTS trade_order_status CASCADE;
DROP TABLE IF EXISTS trades CASCADE;
DROP TABLE IF EXISTS portfolios CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS compliance CASCADE;
DROP TABLE IF EXISTS report CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;

CREATE TABLE users(
    user_id SERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    user_first_name char(50) NOT NULL,
    user_last_name char(50) NOT NULL,
    email TEXT NOT NULL UNIQUE,
    CONSTRAINT check_password_complexity CHECK (LENGTH(password_hash) >= 8),
    --double check hashing algorithm and storage method for password_hash
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_email UNIQUE (email),
    CONSTRAINT unique_username UNIQUE (username),
    --TO DO: Add additional columns for the users table
    CONSTRAINT unique_customer_name UNIQUE (user_first_name, user_last_name)
    --TODO: check if either user is a client, or an employee, or both 
);
CREATE TABLE clients(
    clients_id SERIAL PRIMARY KEY,
    user_id int NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    --No unique constraint on user_id because a user can have multiple client entries
    --CONSTRAINT unique_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE compliance(
    compliance_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    compliance_status VARCHAR(50) NOT NULL CHECK (compliance_status IN ('Pending', 'Approved', 'Rejected')),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE accounts(
    account_id SERIAL PRIMARY KEY,
    account_name TEXT NOT NULL,
    client_id INT NOT NULL,
    risk_profile TEXT NOT NULL CHECK(risk_profile IN ('Conservative', 'Moderate', 'Aggressive')),
    cash_available NUMERIC(20, 2) NOT NULL DEFAULT 0,
    cash_reserved NUMERIC(20, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_id FOREIGN KEY (client_id) REFERENCES clients(clients_id)
);
CREATE TABLE operations(
    operation_id SERIAL PRIMARY KEY,
    operation_name VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT unique_operation_name UNIQUE (operation_name),
    user_id INT NOT NULL,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE analysts(
    analyst_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE model_portfolio(
    portfolio_id SERIAL PRIMARY KEY,
    portfolio_name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE subscriptions(
    subscription_id SERIAL PRIMARY KEY,
    account_id INT,
    portfolio_id INT,
    subscription_start_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id),
    CONSTRAINT unique_account_portfolio UNIQUE (account_id, portfolio_id)
);
CREATE TABLE instruments(
    instrument_id SERIAL PRIMARY KEY,
    instrument_name TEXT NOT NULL,
    instrument_symbol VARCHAR(50) NOT NULL UNIQUE,
    asset_type VARCHAR(256) NOT NULL CHECK (asset_type IN ('Equity', 'Bond', 'Fund', 'Cash')),
    currency_code VARCHAR(3) NOT NULL
);
CREATE TABLE reports (
    report_id SERIAL PRIMARY KEY,
    report_name VARCHAR(255) NOT NULL UNIQUE,
    report_date TIMESTAMP NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
    compliance_id INT NOT NULL,
    CONSTRAINT fk_compliance_id FOREIGN KEY (compliance_id) REFERENCES compliance(compliance_id)
);
CREATE TABLE audit_log(
    audit_id SERIAL PRIMARY KEY,
    report_id INT NOT NULL,
    client_id INT NOT NULL,
    account_id INT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    CONSTRAINT check_start_date_not_null CHECK (start_date IS NOT NULL),
    CONSTRAINT check_end_date_not_null CHECK (end_date IS NOT NULL),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_client_id FOREIGN KEY (client_id) REFERENCES clients(client_id),
    CONSTRAINT fk_report_id FOREIGN KEY (report_id) REFERENCES reports(report_id)
);
CREATE TABLE trade_orders(
    trade_order_id SERIAL PRIMARY KEY,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity NUMERIC(20, 4) NOT NULL CHECK (quantity > 0),
    price NUMERIC(20,2) NOT NULL CHECK (price > 0),
    trade_value NUMERIC(20,2) NOT NULL CHECK (price > 0),
    order_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id)
);
CREATE TABLE trade_order_status(
    status_id SERIAL PRIMARY KEY,   
    trade_order_id INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    status_date TIMESTAMP NOT NULL,
    status_name VARCHAR(50) NOT NULL CHECK (status_name IN ('Pending', 'Success', 'Failed')),
    time_updated TIMESTAMP NOT NULL,
    reason_text VARCHAR(500),
    CONSTRAINT fk_trade_order_id FOREIGN KEY (trade_order_id) REFERENCES trade_orders(trade_order_id)
);
CREATE TABLE holdings(
    holdings_id SERIAL PRIMARY KEY,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
    quantity NUMERIC(20, 4) NOT NULL,
    -- The price at which the instrument was acquired
    total_cost NUMERIC(20, 4) NOT NULL CHECK (total_cost >= 0),
    CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT unique_account_instrument UNIQUE (account_id, instrument_id),
    CONSTRAINT check_quantity_positive CHECK (quantity > 0)
);
CREATE TABLE portfolio_holdings(
    holdings_id SERIAL PRIMARY KEY,
    portfolio_id INT NOT NULL,
    instrument_id INT NOT NULL,
    holding_weight NUMERIC(20, 4) NOT NULL CHECK (holding_weight >= 0),
    CONSTRAINT fk_portfolio_id FOREIGN KEY (portfolio_id) REFERENCES model_portfolio(portfolio_id),
    CONSTRAINT fk_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id),
    CONSTRAINT unique_portfolio_instrument UNIQUE (portfolio_id, instrument_id)
);
IMPORT .csv 'C:/Users/Administrator/trades.csv' INTO TABLE trade_orders;
IMPORT .csv 'C:/Users/Administrator/holdings.csv' INTO TABLE holdings;
IMPORT .csv 'C:/Users/Administrator/portfolio_holdings.csv' INTO TABLE portfolio_holdings;
IMPORT .csv 'C:/Users/Administrator/audit_log.csv' INTO TABLE audit_log;
IMPORT .csv 'C:/Users/Administrator/reports.csv' INTO TABLE reports;
IMPORT .csv 'C:/Users/Administrator/clients.csv' INTO TABLE clients;
IMPORT .csv 'C:/Users/Administrator/accounts.csv' INTO TABLE accounts;
IMPORT .csv 'C:/Users/Administrator/instruments.csv' INTO TABLE instruments;
IMPORT .csv 'C:/Users/Administrator/model_portfolio.csv' INTO TABLE model_portfolio;
--IMPORT .csv 'C:/Users/Administrator/pricing.csv' INTO TABLE pricing;

CREATE TABLE pricing(
    pricing_id SERIAL PRIMARY KEY,
    instrument_id INT NOT NULL,
    price NUMERIC(20, 2) NOT NULL CHECK (price >= 0),
    price_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_instrument_id FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id)
);