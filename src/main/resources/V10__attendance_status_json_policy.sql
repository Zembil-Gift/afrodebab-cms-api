ALTER TABLE employee_attendance
    ALTER COLUMN clock_in_at DROP NOT NULL;

ALTER TABLE employee_attendance
    ADD COLUMN IF NOT EXISTS attendance_status JSONB;

DO $$
DECLARE
    current_type TEXT;
BEGIN
    SELECT data_type
    INTO current_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'employee_attendance'
      AND column_name = 'attendance_status';

    IF current_type = 'USER-DEFINED' THEN
        ALTER TABLE employee_attendance
            ALTER COLUMN attendance_status TYPE JSONB
                USING CASE
                    WHEN attendance_status IS NULL THEN '{}'::jsonb
                    ELSE jsonb_build_object('final', attendance_status::text)
                END;
    END IF;
END $$;

UPDATE employee_attendance
SET attendance_status = COALESCE(attendance_status, '{}'::jsonb)
WHERE attendance_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_employee_attendance_status_jsonb
    ON employee_attendance USING GIN (attendance_status);
