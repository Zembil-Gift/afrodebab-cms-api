-- V8 was meant to rename employee_salary_schedule_days -> employee_office_schedule_days but
-- shipped as a no-op, so existing databases were renamed by hand and a database built purely
-- from migrations never got the table the Employee entity maps. Idempotent across all three
-- states: old name only (rename), already renamed (no-op), neither (create).
DO $$
BEGIN
    IF to_regclass('employee_office_schedule_days') IS NULL THEN
        IF to_regclass('employee_salary_schedule_days') IS NOT NULL THEN
            ALTER TABLE employee_salary_schedule_days RENAME TO employee_office_schedule_days;
            ALTER TABLE employee_office_schedule_days
                RENAME CONSTRAINT uk_employee_salary_schedule_days_employee_day
                TO uk_employee_office_schedule_days_employee_day;
            ALTER INDEX IF EXISTS idx_employee_salary_schedule_days_employee
                RENAME TO idx_employee_office_schedule_days_employee;
        ELSE
            CREATE TABLE employee_office_schedule_days (
                employee_id  BIGINT      NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
                schedule_day VARCHAR(16) NOT NULL,
                CONSTRAINT uk_employee_office_schedule_days_employee_day UNIQUE (employee_id, schedule_day)
            );
            CREATE INDEX idx_employee_office_schedule_days_employee
                ON employee_office_schedule_days (employee_id);
        END IF;
    END IF;
END $$;
