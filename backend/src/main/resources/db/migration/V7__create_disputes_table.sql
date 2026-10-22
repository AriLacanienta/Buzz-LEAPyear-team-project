CREATE TABLE disputes (
    dispute_id BIGSERIAL PRIMARY KEY,
    trade_order_id BIGINT NOT NULL,
    dispute_message VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_disputes_trade_order FOREIGN KEY (trade_order_id)
        REFERENCES trade_orders(trade_order_id) ON DELETE CASCADE,
    CONSTRAINT chk_disputes_status CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT chk_disputes_resolved_at CHECK (
        (status = 'OPEN' AND resolved_at IS NULL)
        OR (status = 'RESOLVED' AND resolved_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX ux_disputes_one_open_per_order
    ON disputes(trade_order_id) WHERE status = 'OPEN';
CREATE INDEX idx_disputes_created_at ON disputes(created_at DESC);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_trade_order ON disputes(trade_order_id);
