-- users 테이블을 현재 Spring Boot 엔티티 기준에 맞춰 정리한다.
-- 핵심 목적:
-- 1. status 체크 제약에 심사 상태를 포함한다.
-- 2. 실제 로그인에 쓰는 비밀번호 해시 컬럼을 password_hash 하나로 통일한다.
-- 3. JPA가 임시로 만든 legacy password 컬럼은 데이터 이관 후 제거한다.

USE dating_db;

-- password 컬럼에만 값이 남아 있다면 password_hash로 이관한다.
UPDATE users
SET password_hash = password
WHERE password_hash IS NULL
  AND password IS NOT NULL;

-- 심사 흐름에 필요한 상태값을 허용하도록 체크 제약을 재정의한다.
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;

ALTER TABLE users
ADD CONSTRAINT chk_users_status
CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED', 'PENDING_REVIEW', 'REJECTED'));

-- 현재 백엔드는 password_hash를 사용하므로 legacy password 컬럼은 제거한다.
ALTER TABLE users DROP COLUMN IF EXISTS password;