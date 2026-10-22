# DB 정렬 기준

현재 프로젝트에서는 Spring Boot 엔티티와 서비스 로직을 기준으로 데이터베이스 스키마를 맞춘다.

## 기준 원칙

- 회원가입 후 상태값은 `PENDING_REVIEW`를 사용한다.
- 사진 심사 반려 상태는 `REJECTED`를 사용한다.
- 이메일 로그인 비밀번호는 `users.password_hash` 컬럼에 해시 값으로 저장한다.
- `users.password` 같은 중복 컬럼은 유지하지 않는다.

## 현재 users 테이블에서 중요한 컬럼

- `email`: 로그인 이메일
- `nickname`: 닉네임
- `provider`: `LOCAL`, `GOOGLE`, `BOTH`
- `status`: `ACTIVE`, `SUSPENDED`, `DELETED`, `PENDING_REVIEW`, `REJECTED`
- `review_comment`: 심사 결과 안내 문구
- `password_hash`: BCrypt 해시 비밀번호
- `reset_password_token`: 비밀번호 재설정 토큰
- `reset_password_token_expires_at`: 비밀번호 재설정 토큰 만료 시각
- `created_at`, `updated_at`: 생성/수정 시각

## 스키마 수정 방법

팀원이 DB를 새로 만들거나 수동으로 수정해야 할 때는 먼저 엔티티를 확인하고, 필요하면 아래 SQL부터 적용한다.

- [2026-04-06-align-users-schema-with-backend.sql](/C:/workspace/dating-app/backend/sql/2026-04-06-align-users-schema-with-backend.sql)

## 주의사항

- JPA `ddl-auto: update`와 수동 SQL 변경을 섞으면 컬럼이 중복될 수 있다.
- 운영 전에는 Flyway 같은 마이그레이션 도구로 SQL을 고정하는 것이 좋다.
- 컬럼명을 바꿀 때는 엔티티 필드명보다 `@Column(name = ...)` 매핑을 먼저 확인한다.