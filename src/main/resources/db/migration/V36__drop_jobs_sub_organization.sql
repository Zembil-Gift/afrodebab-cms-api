-- Jobs are organization-wide; the sub-organization is chosen when a candidate is hired.
-- Dropping the column also drops its foreign key and index.
ALTER TABLE jobs DROP COLUMN IF EXISTS sub_organization_id;
