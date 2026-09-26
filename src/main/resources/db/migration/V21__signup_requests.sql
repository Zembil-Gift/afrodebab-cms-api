-- Self-serve "Start free" signup requests. Global (non-tenant) table: a prospect submits
-- their company + contact details from the public site; a platform admin later reviews the
-- request and provisions the organization + first manager from it.
CREATE TABLE IF NOT EXISTS signup_requests (
    id           BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(150) NOT NULL,
    email        VARCHAR(200) NOT NULL,
    message      TEXT         NULL,
    status       VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_signup_requests_status_created
    ON signup_requests (status, created_at DESC);
