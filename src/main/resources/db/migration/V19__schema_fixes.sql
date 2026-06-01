ALTER TABLE events ADD COLUMN IF NOT EXISTS cover_image_url TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_admin_peer_reviews_reviewee_period'
          AND conrelid = 'admin_peer_reviews'::regclass
    ) THEN
        ALTER TABLE admin_peer_reviews
            ADD CONSTRAINT uk_admin_peer_reviews_reviewee_period
            UNIQUE (reviewee_id, period_id);
    END IF;
END $$;
