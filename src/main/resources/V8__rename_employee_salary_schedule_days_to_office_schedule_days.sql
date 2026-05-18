DO $$
BEGIN
    IF to_regclass('public.employee_salary_schedule_days') IS NOT NULL
        AND to_regclass('public.employee_office_schedule_days') IS NULL THEN
        ALTER TABLE employee_salary_schedule_days RENAME TO employee_office_schedule_days;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_employee_salary_schedule_days_employee_day'
    )
    AND to_regclass('public.employee_office_schedule_days') IS NOT NULL THEN
        ALTER TABLE employee_office_schedule_days
            RENAME CONSTRAINT uk_employee_salary_schedule_days_employee_day
                TO uk_employee_office_schedule_days_employee_day;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.idx_employee_salary_schedule_days_employee') IS NOT NULL
        AND to_regclass('public.idx_employee_office_schedule_days_employee') IS NULL THEN
        ALTER INDEX idx_employee_salary_schedule_days_employee
            RENAME TO idx_employee_office_schedule_days_employee;
    END IF;
END $$;
