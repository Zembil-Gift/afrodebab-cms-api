-- Per-organization email schedule. The dispatch time is local to email_timezone and is
-- converted to UTC on every scheduler tick (so DST zones stay correct).
-- Defaults: 03:00 Africa/Addis_Ababa (UTC+3, no DST) == 00:00 UTC, the previous fixed cron.
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS email_dispatch_time TIME NOT NULL DEFAULT '03:00';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS email_timezone VARCHAR(64) NOT NULL DEFAULT 'Africa/Addis_Ababa';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS payroll_reminder_interval_days INT NOT NULL DEFAULT 3;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS last_email_dispatch_at TIMESTAMPTZ;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS last_payroll_reminder_at TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_organizations_payroll_reminder_interval') THEN
        ALTER TABLE organizations ADD CONSTRAINT chk_organizations_payroll_reminder_interval
            CHECK (payroll_reminder_interval_days BETWEEN 1 AND 30);
    END IF;
END $$;
