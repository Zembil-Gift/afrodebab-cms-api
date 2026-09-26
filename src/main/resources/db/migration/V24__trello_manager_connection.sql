-- Per-manager Trello connection: each manager stores their own (encrypted) Trello token
-- and the set of boards they chose to track. Replaces the old single global TRELLO_TOKEN/
-- TRELLO_BOARD sync path.

ALTER TABLE managers ADD COLUMN trello_token TEXT;

CREATE TABLE IF NOT EXISTS manager_trello_boards (
    manager_id BIGINT      NOT NULL REFERENCES managers (id) ON DELETE CASCADE,
    board_id   VARCHAR(64) NOT NULL,
    board_name VARCHAR(255),
    PRIMARY KEY (manager_id, board_id)
);
