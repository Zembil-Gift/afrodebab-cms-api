CREATE TABLE IF NOT EXISTS employee_payments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    cycle_start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    amount_minor BIGINT NOT NULL,
    paid_amount_minor BIGINT,
    status VARCHAR(16) NOT NULL,
    transaction_reference VARCHAR(255),
    paid_at TIMESTAMPTZ,
    last_reminder_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_employee_payments_employee_cycle_start UNIQUE (employee_id, cycle_start_date),
    CONSTRAINT uk_employee_payments_transaction_reference UNIQUE (transaction_reference)
);

CREATE INDEX IF NOT EXISTS idx_employee_payments_employee_due
    ON employee_payments (employee_id, due_date DESC);

CREATE INDEX IF NOT EXISTS idx_employee_payments_status_due
    ON employee_payments (status, due_date ASC);
