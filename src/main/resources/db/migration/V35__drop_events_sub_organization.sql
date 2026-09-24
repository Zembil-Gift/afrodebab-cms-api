-- Events are organization-wide; dropping the column also drops fk_events_sub_organization and idx_events_sub_org.
ALTER TABLE events DROP COLUMN IF EXISTS sub_organization_id;
