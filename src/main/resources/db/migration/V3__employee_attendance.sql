CREATE TABLE IF NOT EXISTS employee_attendance (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    clock_in_at TIMESTAMPTZ NOT NULL,
    clock_out_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_employee_attendance_employee_date UNIQUE (employee_id, attendance_date)
);

CREATE INDEX idx_employee_attendance_employee_date
    ON employee_attendance (employee_id, attendance_date DESC);


ALTER TABLE employee_attendance ALTER COLUMN clock_out_at DROP NOT NULL;