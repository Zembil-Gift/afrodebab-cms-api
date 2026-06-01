DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'employees'
          AND column_name = 'department'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE employees
            ALTER COLUMN department TYPE TEXT
                USING CASE
                    WHEN department IS NULL THEN NULL
                    ELSE convert_from(department, 'UTF8')
                END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'employees'
          AND column_name = 'role'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE employees
            ALTER COLUMN role TYPE TEXT
                USING CASE
                    WHEN role IS NULL THEN NULL
                    ELSE convert_from(role, 'UTF8')
                END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'employees'
          AND column_name = 'employment_type'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE employees
            ALTER COLUMN employment_type TYPE TEXT
                USING CASE
                    WHEN employment_type IS NULL THEN NULL
                    ELSE convert_from(employment_type, 'UTF8')
                END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'employees'
          AND column_name = 'employee_status'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE employees
            ALTER COLUMN employee_status TYPE TEXT
                USING CASE
                    WHEN employee_status IS NULL THEN NULL
                    ELSE convert_from(employee_status, 'UTF8')
                END;
    END IF;
END $$;
