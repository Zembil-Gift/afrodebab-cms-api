-- Per-manager Google account link (Calendar + Sheets). The refresh token is encrypted with TokenCipher.
ALTER TABLE managers
    ADD COLUMN IF NOT EXISTS google_refresh_token TEXT,
    ADD COLUMN IF NOT EXISTS google_email         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS google_scopes        TEXT;

-- Interviews scheduled for job applications.
CREATE TABLE IF NOT EXISTS interviews (
    id                      BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    application_id          BIGINT NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    scheduled_by_manager_id BIGINT REFERENCES managers(id) ON DELETE SET NULL,
    start_at                TIMESTAMPTZ NOT NULL,
    end_at                  TIMESTAMPTZ NOT NULL,
    mode                    VARCHAR(16) NOT NULL,
    location                VARCHAR(500),
    meeting_url             VARCHAR(1000),
    notes                   TEXT,
    status                  VARCHAR(16) NOT NULL,
    google_event_id         VARCHAR(255),
    sequence                INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_interviews_time CHECK (end_at > start_at)
);

CREATE INDEX IF NOT EXISTS idx_interviews_application ON interviews (application_id);
CREATE INDEX IF NOT EXISTS idx_interviews_org_start ON interviews (organization_id, start_at);

-- Interviewers: org managers, employees, or anyone invited by email.
CREATE TABLE IF NOT EXISTS interview_participants (
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    kind         VARCHAR(16) NOT NULL,
    manager_id   BIGINT REFERENCES managers(id) ON DELETE SET NULL,
    employee_id  BIGINT REFERENCES employees(id) ON DELETE SET NULL,
    name         VARCHAR(255),
    email        VARCHAR(255) NOT NULL,
    PRIMARY KEY (interview_id, email)
);
