CREATE TABLE watchlist_items (
    watchlist_item_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_instrument FOREIGN KEY (instrument_id) REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    CONSTRAINT unique_watchlist_user_instrument UNIQUE (user_id, instrument_id)
);

CREATE INDEX idx_watchlist_user ON watchlist_items(user_id);
