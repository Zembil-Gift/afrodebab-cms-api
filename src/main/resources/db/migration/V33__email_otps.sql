-- One pending email verification code per (purpose, email): self-serve signup and manager
-- password changes. Only a BCrypt hash of the code is stored; the row is deleted once used.
CREATE TABLE IF NOT EXISTS email_otps (
    id         BIGSERIAL PRIMARY KEY,
    purpose    VARCHAR(40)  NOT NULL,
    email      VARCHAR(200) NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_email_otps_purpose_email UNIQUE (purpose, email)
);
