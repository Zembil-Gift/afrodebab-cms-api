-- One row per interviewer per interview: their no-login feedback link (only its SHA-256 hash is stored)
-- and, once submitted, their feedback. The token is cleared on submit, so each link works until used.
CREATE TABLE IF NOT EXISTS interview_feedback (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    interview_id      BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    interviewer_name  VARCHAR(255),
    interviewer_email VARCHAR(255) NOT NULL,
    token_hash        VARCHAR(64) UNIQUE,
    expires_at        TIMESTAMPTZ NOT NULL,
    recommendation    VARCHAR(32),
    comments          TEXT,
    submitted_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_interview_feedback_interviewer UNIQUE (interview_id, interviewer_email)
);
