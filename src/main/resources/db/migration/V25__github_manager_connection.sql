-- Per-manager GitHub connection: each manager stores their own (encrypted) OAuth token
-- and the set of GitHub organizations they chose to track. Replaces the old single global
-- GITHUB_KEY/GITHUB_ORG_NAME sync path.

ALTER TABLE managers ADD COLUMN github_token TEXT;

CREATE TABLE IF NOT EXISTS manager_github_orgs (
    manager_id BIGINT       NOT NULL REFERENCES managers (id) ON DELETE CASCADE,
    org_login  VARCHAR(255) NOT NULL,
    org_name   VARCHAR(255),
    PRIMARY KEY (manager_id, org_login)
);
