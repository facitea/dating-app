USE dating_db;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS admin_memo TEXT NULL;