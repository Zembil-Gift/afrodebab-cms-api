-- Leadership principles go back to being per-organization (undoing V31's move to one global
-- set). The platform admin now curates a template that orgs copy from ("add defaults").

-- 1. The current global principles become the platform template ------------------------------
CREATE TABLE IF NOT EXISTS default_leadership_principles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_default_leadership_principles_name
    ON default_leadership_principles (LOWER(name));

INSERT INTO default_leadership_principles (name, description, is_active, created_at, updated_at)
SELECT name, description, COALESCE(is_active, TRUE), created_at, updated_at
FROM leadership_principles
ORDER BY id;

-- 2. Every existing org gets its own copy of what it rates against today: the active ones plus
--    any inactive one its own reviews still reference ------------------------------------------
DROP INDEX IF EXISTS uk_leadership_principles_name;
ALTER TABLE leadership_principles ADD COLUMN IF NOT EXISTS organization_id BIGINT;

INSERT INTO leadership_principles (name, description, is_active, created_at, updated_at, organization_id)
SELECT lp.name, lp.description, lp.is_active, lp.created_at, lp.updated_at, o.id
FROM leadership_principles lp
CROSS JOIN organizations o
WHERE lp.organization_id IS NULL
  AND (lp.is_active
       OR EXISTS (SELECT 1 FROM peer_reviews pr
                  WHERE pr.principle_id = lp.id AND pr.organization_id = o.id))
ORDER BY o.id, lp.id;

-- 3. Repoint each org's ratings at its own copy, then drop the global rows ------------------
UPDATE peer_reviews pr
SET principle_id = own.id
FROM leadership_principles global, leadership_principles own
WHERE pr.principle_id = global.id
  AND global.organization_id IS NULL
  AND own.organization_id = pr.organization_id
  AND LOWER(own.name) = LOWER(global.name);

DELETE FROM leadership_principles WHERE organization_id IS NULL;

-- 4. Restore the per-org constraints ----------------------------------------------------------
ALTER TABLE leadership_principles ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE leadership_principles DROP CONSTRAINT IF EXISTS fk_leadership_principles_org;
ALTER TABLE leadership_principles
    ADD CONSTRAINT fk_leadership_principles_org FOREIGN KEY (organization_id) REFERENCES organizations(id);
CREATE INDEX IF NOT EXISTS idx_leadership_principles_org ON leadership_principles (organization_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_leadership_principles_org_name
    ON leadership_principles (organization_id, LOWER(name));
