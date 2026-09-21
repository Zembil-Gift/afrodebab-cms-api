-- Leadership principles become platform-wide (managed by the platform admin) instead of
-- per-organization, the seed "Afrodebab" organization from V20 is removed, and the first
-- platform admin is created.

-- 1. Collapse same-named principles from different orgs into one global row -----------------
--    (the lowest id wins; peer reviews are repointed so no rating is lost).
WITH ranked AS (
    SELECT id, MIN(id) OVER (PARTITION BY LOWER(TRIM(name))) AS keep_id
    FROM leadership_principles
)
UPDATE peer_reviews pr
SET principle_id = r.keep_id
FROM ranked r
WHERE pr.principle_id = r.id AND r.id <> r.keep_id;

DELETE FROM leadership_principles lp
USING leadership_principles keeper
WHERE LOWER(TRIM(lp.name)) = LOWER(TRIM(keeper.name)) AND lp.id > keeper.id;

-- 2. Detach principles from organizations --------------------------------------------------
ALTER TABLE leadership_principles DROP CONSTRAINT IF EXISTS fk_leadership_principles_org;
ALTER TABLE leadership_principles DROP CONSTRAINT IF EXISTS uk_leadership_principles_org_name;
DROP INDEX IF EXISTS idx_leadership_principles_org;
ALTER TABLE leadership_principles DROP COLUMN IF EXISTS organization_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_leadership_principles_name ON leadership_principles (LOWER(name));

-- 3. Remove the seed "Afrodebab" organization -----------------------------------------------
--    Its default sub-organization goes with it (ON DELETE CASCADE). A database where the org
--    already owns real data (managers, employees, blogs...) fails the FK check and is left
--    untouched, so this can never wipe a live tenant.
DO $$
BEGIN
    DELETE FROM organizations WHERE slug = 'afrodebab';
EXCEPTION WHEN foreign_key_violation THEN
    RAISE NOTICE 'Organization "afrodebab" still owns data; kept.';
END $$;

-- 4. First platform admin --------------------------------------------------------------------
INSERT INTO platform_admins (name, email, password_hash)
VALUES ('Platform Admin', 'gogerami.afro@gmail.com',
        '$2a$12$Ftz8hDouJFPDuT271ygKzufXtYtLv9eLWXUaEbU/MH7GHQzZIODxy')
ON CONFLICT (email) DO NOTHING;
