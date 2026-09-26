-- One live spreadsheet per manager / vice manager, kept up to date by the hourly sync.
CREATE TABLE IF NOT EXISTS google_sheet_syncs (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    manager_id      BIGINT NOT NULL UNIQUE REFERENCES managers(id) ON DELETE CASCADE,
    spreadsheet_id  VARCHAR(255) NOT NULL,
    spreadsheet_url VARCHAR(500) NOT NULL,
    datasets        VARCHAR(255) NOT NULL,
    last_synced_at  TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
