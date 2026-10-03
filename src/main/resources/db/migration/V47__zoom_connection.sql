-- Per-manager Zoom account link (encrypted refresh token) and the Zoom meeting behind an interview.
ALTER TABLE managers ADD COLUMN IF NOT EXISTS zoom_refresh_token TEXT;
ALTER TABLE managers ADD COLUMN IF NOT EXISTS zoom_email VARCHAR(255);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS zoom_meeting_id VARCHAR(64);
