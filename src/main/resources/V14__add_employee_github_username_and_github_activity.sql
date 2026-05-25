-- Add github_username column to employees table
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS github_username VARCHAR(150) NULL;

-- Create github_activities table
CREATE TABLE IF NOT EXISTS github_activities (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    github_username VARCHAR(150) NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- e.g., 'COMMIT', 'PR_OPENED', 'PR_MERGED', 'PR_CLOSED', 'PR_REVIEW', 'ISSUE_OPENED', 'ISSUE_CLOSED'
    repository VARCHAR(250) NOT NULL,
    activity_id VARCHAR(100) NOT NULL, -- GitHub Event ID or composite key (e.g. SHA)
    title TEXT,
    description TEXT,
    url TEXT,
    activity_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_github_activities_activity_id UNIQUE (activity_id)
);

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_github_activities_employee_timestamp
    ON github_activities (employee_id, activity_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_github_activities_type
    ON github_activities (activity_type);
