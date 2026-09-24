-- Richer job postings + a per-job application form (links, file uploads, written answers).
ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS experience_level     VARCHAR(40),
    ADD COLUMN IF NOT EXISTS salary_range         VARCHAR(120),
    ADD COLUMN IF NOT EXISTS application_deadline DATE,
    ADD COLUMN IF NOT EXISTS application_fields   JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Applicant answers, one entry per form field (label/type snapshotted at submit time).
ALTER TABLE job_applications
    ADD COLUMN IF NOT EXISTS answers JSONB NOT NULL DEFAULT '[]'::jsonb;

-- Phones are now stored canonically as +2519XXXXXXXX / +2517XXXXXXXX; convert existing
-- Ethiopian numbers so duplicate checks match regardless of how they were typed.
UPDATE job_applications
SET phone_number = '+251' || substring(regexp_replace(phone_number, '[\s().-]', '', 'g') from '([79][0-9]{8})$')
WHERE regexp_replace(phone_number, '[\s().-]', '', 'g') ~ '^(\+?251|0)?[79][0-9]{8}$';

-- One application per email / phone per job, enforced by the database too (the service check
-- alone can race on double submits). Skipped if old data already holds duplicates.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM job_applications GROUP BY job_id, lower(email) HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_job_applications_job_email
            ON job_applications (job_id, lower(email));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM job_applications WHERE phone_number IS NOT NULL
                   GROUP BY job_id, phone_number HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_job_applications_job_phone
            ON job_applications (job_id, phone_number) WHERE phone_number IS NOT NULL;
    END IF;
END $$;
