-- Employee password emails used to keep the generated password in plain text forever.
-- Delivered ones no longer need it; queued ones are now encrypted by the app on creation.
UPDATE email_notifications
SET payload = jsonb_set(payload::jsonb, '{generatedPassword}', '"[redacted]"')::text
WHERE type = 'EMPLOYEE_PASSWORD'
  AND status = 'SENT'
  AND payload::jsonb ->> 'generatedPassword' IS DISTINCT FROM '[redacted]';
