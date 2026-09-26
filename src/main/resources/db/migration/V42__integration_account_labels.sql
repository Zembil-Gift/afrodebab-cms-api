-- Which Trello / GitHub account a manager connected (email, or @username when no email is shared).
ALTER TABLE managers ADD COLUMN IF NOT EXISTS trello_account VARCHAR(255);
ALTER TABLE managers ADD COLUMN IF NOT EXISTS github_account VARCHAR(255);
