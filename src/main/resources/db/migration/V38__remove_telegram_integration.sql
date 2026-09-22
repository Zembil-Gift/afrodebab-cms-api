-- Telegram support integration removed: drop its tickets table, the employee handle
-- and the support score that was derived solely from Telegram tickets.
DROP TABLE IF EXISTS support_metrics;
ALTER TABLE employees DROP COLUMN IF EXISTS telegram_username;
ALTER TABLE employee_metric_scores DROP COLUMN IF EXISTS support_score;
