ALTER TABLE employee_attendance
    ADD COLUMN IF NOT EXISTS lunch_break_in_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lunch_break_out_at TIMESTAMPTZ;
