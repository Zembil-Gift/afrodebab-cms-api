-- Manager / vice manager broadcasts to the whole org or chosen branches.
CREATE TABLE IF NOT EXISTS broadcasts (
    id                BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sender_manager_id BIGINT REFERENCES managers(id) ON DELETE SET NULL,
    sender_name       VARCHAR(255) NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    body              TEXT NOT NULL,
    send_email        BOOLEAN NOT NULL DEFAULT TRUE,
    recipient_count   INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_broadcasts_org_created ON broadcasts (organization_id, created_at DESC);

-- No rows for a broadcast = everyone in the organization.
CREATE TABLE IF NOT EXISTS broadcast_sub_organizations (
    broadcast_id        BIGINT NOT NULL REFERENCES broadcasts(id) ON DELETE CASCADE,
    sub_organization_id BIGINT NOT NULL REFERENCES sub_organizations(id) ON DELETE CASCADE,
    PRIMARY KEY (broadcast_id, sub_organization_id)
);

-- In-app notifications. Exactly one recipient: an employee or a manager/vice manager.
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    employee_id     BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    manager_id      BIGINT REFERENCES managers(id) ON DELETE CASCADE,
    type            VARCHAR(64) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    link            VARCHAR(500),
    broadcast_id    BIGINT REFERENCES broadcasts(id) ON DELETE CASCADE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notifications_one_recipient CHECK ((employee_id IS NULL) <> (manager_id IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_notifications_employee_created ON notifications (employee_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_manager_created ON notifications (manager_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_broadcast ON notifications (broadcast_id);
