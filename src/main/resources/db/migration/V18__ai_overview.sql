ALTER TABLE job_applications
  ADD COLUMN IF NOT EXISTS ai_overview_text TEXT,
  ADD COLUMN IF NOT EXISTS ai_overview_status VARCHAR(20) DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS ai_overview_error TEXT,
  ADD COLUMN IF NOT EXISTS ai_overview_attempt_count INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS ai_overview_completed_at TIMESTAMPTZ;
