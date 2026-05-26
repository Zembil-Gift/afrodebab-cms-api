ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS role TEXT,
    ADD COLUMN IF NOT EXISTS department TEXT,
    ADD COLUMN IF NOT EXISTS employment_type TEXT,
    ADD COLUMN IF NOT EXISTS employee_status TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attendance_status_enum') THEN
        CREATE TYPE attendance_status_enum AS ENUM (
            'ON_TIME',
            'LATE',
            'ABSENT',
            'APPROVED_LEAVE',
            'REMOTE_APPROVED'
        );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'peer_review_rating_enum') THEN
        CREATE TYPE peer_review_rating_enum AS ENUM (
            'EXCEEDS_THE_BAR',
            'MEETS_THE_BAR',
            'NEEDS_IMPROVEMENT'
        );
    END IF;
END $$;

ALTER TABLE employee_attendance
    ADD COLUMN IF NOT EXISTS attendance_status attendance_status_enum,
    ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_employee_attendance_status
    ON employee_attendance (attendance_status);

CREATE TABLE IF NOT EXISTS leadership_principles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS peer_reviews (
    id BIGSERIAL PRIMARY KEY,
    reviewer_id BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    reviewee_id BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    period_start DATE,
    period_end DATE,
    principle_id BIGINT REFERENCES leadership_principles(id),
    rating peer_review_rating_enum,
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_peer_reviews_unique_per_principle_period
        UNIQUE (reviewer_id, reviewee_id, principle_id, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewee_period
    ON peer_reviews (reviewee_id, period_start, period_end);

CREATE INDEX IF NOT EXISTS idx_peer_reviews_reviewer_period
    ON peer_reviews (reviewer_id, period_start, period_end);

CREATE TABLE IF NOT EXISTS task_metrics (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    source VARCHAR(40),
    source_task_id VARCHAR(200),
    task_title TEXT,
    status VARCHAR(40),
    assigned_date DATE,
    completed_date DATE,
    due_date DATE,
    is_overdue BOOLEAN,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_task_metrics_employee_created
    ON task_metrics (employee_id, created_at DESC);

CREATE TABLE IF NOT EXISTS support_metrics (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    telegram_ticket_id VARCHAR(200),
    customer_name VARCHAR(200),
    issue_type VARCHAR(80),
    status VARCHAR(40),
    response_time_minutes INT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_support_metrics_employee_created
    ON support_metrics (employee_id, created_at DESC);

CREATE TABLE IF NOT EXISTS employee_metric_scores (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    period_start DATE,
    period_end DATE,
    leadership_score NUMERIC(6,2),
    attendance_score NUMERIC(6,2),
    task_score NUMERIC(6,2),
    support_score NUMERIC(6,2),
    overall_score NUMERIC(6,2),
    strength_summary TEXT,
    improvement_summary TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_employee_metric_scores_employee_period
    ON employee_metric_scores (employee_id, period_start, period_end, created_at DESC);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Ownership & Accountability', 'Takes responsibility, follows through, and proactively solves problems.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Ownership & Accountability'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Integrity & Transparency', 'Acts with honesty, openness, and ethical behavior in all situations.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Integrity & Transparency'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Customer-Centered Thinking', 'Prioritizes customer value and long-term trust.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Customer-Centered Thinking'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Bias for Action', 'Executes decisively and avoids unnecessary delay.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Bias for Action'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Continuous Growth', 'Seeks feedback, learns quickly, and improves consistently.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Continuous Growth'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Team Collaboration', 'Collaborates effectively and supports cross-team outcomes.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Team Collaboration'
);

INSERT INTO leadership_principles (name, description, is_active)
SELECT 'Communication Discipline', 'Communicates clearly, consistently, and with ownership.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM leadership_principles WHERE name = 'Communication Discipline'
);
