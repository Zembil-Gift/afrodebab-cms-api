-- Add telegram_username column to employees table
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS telegram_username VARCHAR(150) NULL;
