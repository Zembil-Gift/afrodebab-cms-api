ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS salary_effective_date DATE,
    ADD COLUMN IF NOT EXISTS salary_amount_minor BIGINT;

CREATE TABLE IF NOT EXISTS employee_salary_schedule_days (
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    schedule_day VARCHAR(16) NOT NULL,
    CONSTRAINT uk_employee_salary_schedule_days_employee_day
        UNIQUE (employee_id, schedule_day)
);

CREATE INDEX IF NOT EXISTS idx_employee_salary_schedule_days_employee
    ON employee_salary_schedule_days (employee_id);
