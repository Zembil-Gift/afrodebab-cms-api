CREATE TABLE IF NOT EXISTS admin_peer_reviews (
    id BIGSERIAL PRIMARY KEY,
    reviewer_id BIGINT REFERENCES admins(id) ON DELETE SET NULL,
    reviewee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    period_id BIGINT NOT NULL REFERENCES peer_review_periods(id) ON DELETE CASCADE,
    rating peer_review_rating_enum NOT NULL,
    feedback TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

