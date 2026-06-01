CREATE TABLE IF NOT EXISTS peer_review_periods (
    id BIGSERIAL PRIMARY KEY,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_peer_review_periods_period UNIQUE (period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_peer_review_periods_period
    ON peer_review_periods (period_start, period_end);
