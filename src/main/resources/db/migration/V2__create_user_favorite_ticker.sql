CREATE TABLE IF NOT EXISTS user_favorite (
    user_id TEXT PRIMARY KEY,
    ticker TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS user_favorite_ticker_idx
    ON user_favorite (ticker);
