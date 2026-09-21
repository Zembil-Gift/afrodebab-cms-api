-- Per-organization email customization: a dedicated email logo and
-- subject/heading/message overrides per notification type (NULL = default).
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS email_logo_url TEXT;

CREATE TABLE IF NOT EXISTS email_templates (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    type            VARCHAR(64) NOT NULL,
    subject         VARCHAR(255),
    heading         VARCHAR(255),
    message         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_email_templates_org_type UNIQUE (organization_id, type)
);
