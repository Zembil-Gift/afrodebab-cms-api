-- Blogs are organization-wide; dropping the column also drops fk_blogs_sub_organization and idx_blogs_sub_org.
ALTER TABLE blogs DROP COLUMN IF EXISTS sub_organization_id;
