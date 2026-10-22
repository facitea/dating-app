USE dating_db;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS review_deadline_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS last_warning_sent_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS profile_completed_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL;

UPDATE users
SET review_deadline_at = DATE_ADD(created_at, INTERVAL 3 DAY)
WHERE status = 'PENDING_REVIEW'
  AND review_deadline_at IS NULL;

UPDATE users
SET review_deadline_at = DATE_ADD(updated_at, INTERVAL 7 DAY)
WHERE status = 'REJECTED'
  AND review_deadline_at IS NULL;