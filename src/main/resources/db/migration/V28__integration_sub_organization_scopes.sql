-- Which sub-organizations each tracked Trello board / GitHub organization credits during
-- sync. No rows for a board/org = every sub-organization (the previous behaviour).
-- Vice managers are always scoped to their own branch in code, so they never need rows here.
CREATE TABLE IF NOT EXISTS manager_trello_board_sub_orgs (
    manager_id          BIGINT      NOT NULL REFERENCES managers (id) ON DELETE CASCADE,
    board_id            VARCHAR(64) NOT NULL,
    sub_organization_id BIGINT      NOT NULL REFERENCES sub_organizations (id) ON DELETE CASCADE,
    PRIMARY KEY (manager_id, board_id, sub_organization_id)
);

CREATE TABLE IF NOT EXISTS manager_github_org_sub_orgs (
    manager_id          BIGINT       NOT NULL REFERENCES managers (id) ON DELETE CASCADE,
    org_login           VARCHAR(255) NOT NULL,
    sub_organization_id BIGINT       NOT NULL REFERENCES sub_organizations (id) ON DELETE CASCADE,
    PRIMARY KEY (manager_id, org_login, sub_organization_id)
);
