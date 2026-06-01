-- Add trello_username column to employees table
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS trello_username VARCHAR(150) NULL;

-- Create trello_activities table
CREATE TABLE IF NOT EXISTS trello_activities (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    trello_username VARCHAR(150) NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- e.g., 'CARD_CREATED', 'CARD_MOVED', 'CARD_ARCHIVED', 'COMMENT_ADDED', 'CHECKITEM_COMPLETED', 'ATTACHMENT_ADDED'
    board_name VARCHAR(250) NOT NULL,
    card_id VARCHAR(100) NOT NULL,
    card_name TEXT,
    list_name TEXT,
    activity_id VARCHAR(100) NOT NULL, -- Trello action ID
    description TEXT,
    url TEXT,
    activity_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_trello_activities_activity_id UNIQUE (activity_id)
);

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_trello_activities_employee_timestamp
    ON trello_activities (employee_id, activity_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_trello_activities_type
    ON trello_activities (activity_type);
