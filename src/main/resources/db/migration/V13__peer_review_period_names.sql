ALTER TABLE peer_review_periods
    ADD COLUMN IF NOT EXISTS name TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_peer_review_periods_name'
    ) THEN
        ALTER TABLE peer_review_periods
            ADD CONSTRAINT uk_peer_review_periods_name UNIQUE (name);
    END IF;
END $$;
