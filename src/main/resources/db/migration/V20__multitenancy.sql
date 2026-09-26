-- Multi-tenancy: introduce Organizations as the top-level tenant, split the former global
-- "admin" into a global PlatformAdmin (org CRUD) and a per-org Manager, and scope every
-- business table by organization_id. All existing data is migrated into a single default
-- "Afrodebab" organization so the app behaves identically for current users.

-- 1. Global (non-tenant) tables ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    slug       VARCHAR(120) NOT NULL UNIQUE,
    status     VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    plan       VARCHAR(30)  NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_admins (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ  NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 2. Default organization that owns all pre-existing data ----------------------------------
INSERT INTO organizations (name, slug, status, plan)
VALUES ('Afrodebab', 'afrodebab', 'ACTIVE', 'FREE')
ON CONFLICT (slug) DO NOTHING;

-- 3. The former global "admins" become this org's managers --------------------------------
ALTER TABLE admins RENAME TO managers;

-- 4. Scope every tenant-owned table by organization_id (add -> backfill -> enforce) --------
DO $$
DECLARE
    t       TEXT;
    org_id  BIGINT;
    tenant_tables TEXT[] := ARRAY[
        'managers', 'employees', 'employee_attendance', 'employee_payments',
        'employee_metric_scores', 'blogs', 'events', 'jobs', 'job_applications',
        'email_notifications', 'github_activities', 'trello_activities',
        'support_metrics', 'task_metrics', 'peer_reviews', 'peer_review_periods',
        'admin_peer_reviews', 'leadership_principles'
    ];
BEGIN
    SELECT id INTO org_id FROM organizations WHERE slug = 'afrodebab';

    FOREACH t IN ARRAY tenant_tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS organization_id BIGINT', t);
        EXECUTE format('UPDATE %I SET organization_id = %L WHERE organization_id IS NULL', t, org_id);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN organization_id SET NOT NULL', t);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (organization_id) REFERENCES organizations(id)',
                       t, 'fk_' || t || '_org');
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I(organization_id)', 'idx_' || t || '_org', t);
    END LOOP;
END $$;

-- 5. Business keys that must be unique per-org, not globally -------------------------------
-- Manager/employee email stay GLOBALLY unique so login needs only email + password.
ALTER TABLE blogs  DROP CONSTRAINT IF EXISTS blogs_slug_key;
ALTER TABLE blogs  ADD  CONSTRAINT uk_blogs_org_slug  UNIQUE (organization_id, slug);

ALTER TABLE events DROP CONSTRAINT IF EXISTS events_slug_key;
ALTER TABLE events ADD  CONSTRAINT uk_events_org_slug UNIQUE (organization_id, slug);

ALTER TABLE jobs   DROP CONSTRAINT IF EXISTS jobs_slug_key;
ALTER TABLE jobs   ADD  CONSTRAINT uk_jobs_org_slug   UNIQUE (organization_id, slug);

ALTER TABLE leadership_principles DROP CONSTRAINT IF EXISTS leadership_principles_name_key;
ALTER TABLE leadership_principles ADD  CONSTRAINT uk_leadership_principles_org_name UNIQUE (organization_id, name);

ALTER TABLE peer_review_periods DROP CONSTRAINT IF EXISTS uk_peer_review_periods_period;
ALTER TABLE peer_review_periods ADD  CONSTRAINT uk_peer_review_periods_org_period UNIQUE (organization_id, period_start, period_end);

ALTER TABLE peer_review_periods DROP CONSTRAINT IF EXISTS uk_peer_review_periods_name;
ALTER TABLE peer_review_periods ADD  CONSTRAINT uk_peer_review_periods_org_name UNIQUE (organization_id, name);
