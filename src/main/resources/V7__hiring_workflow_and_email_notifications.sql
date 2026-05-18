ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS resume_url TEXT,
    ADD COLUMN IF NOT EXISTS hired_employee_id BIGINT REFERENCES employees(id),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE job_applications
SET status = 'APPLIED'
WHERE status IS NULL;

UPDATE job_applications
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE job_applications
    ALTER COLUMN status SET NULL,
    ALTER COLUMN updated_at SET NULL;

CREATE INDEX IF NOT EXISTS idx_job_applications_job_status
    ON job_applications (job_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_job_applications_job_hired
    ON job_applications (job_id)
    WHERE status = 'HIRED';

CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    recipient_email VARCHAR(200) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_notifications_status_attempt_created
    ON email_notifications (status, attempt_count, created_at);
