-- Gross salary is now entered per employee; income tax and pension are derived and
-- captured on each payment. amount_minor becomes the net (disbursed) amount.
ALTER TABLE employee_payments
    ADD COLUMN IF NOT EXISTS gross_amount_minor BIGINT,
    ADD COLUMN IF NOT EXISTS income_tax_minor BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employee_pension_minor BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS employer_pension_minor BIGINT NOT NULL DEFAULT 0;

-- Existing payments were stored as net (salary was previously treated as net), so the
-- gross equals the historical amount and no deductions applied.
UPDATE employee_payments
SET gross_amount_minor = amount_minor
WHERE gross_amount_minor IS NULL;

ALTER TABLE employee_payments
    ALTER COLUMN gross_amount_minor SET NOT NULL;
