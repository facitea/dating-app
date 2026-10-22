# Dating App

Spring Boot / MariaDB backend and Vue / Vite frontend.

## Local development

- Java 17, MariaDB, Node.js and npm are required.
- Create a local database and account, then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend terminal.
- Backend: from `backend`, run `./gradlew bootRun --args="--server.port=8082"` (Windows: `./gradlew.bat`).
- Frontend: from `frontend`, run `npm ci`, then `npm run dev`.
- Open http://localhost:5173. The development server proxies `/api` and `/uploads` to port 8082.
- Admin console: http://localhost:5173/admin. Configure `ADMIN_REVIEW_KEY` before deployment.
- `JWT_SECRET` and `ADMIN_REVIEW_KEY` can override the development placeholders. Use a strong JWT secret of at least 32 bytes.
- Optional push credentials and test-credit settings belong in `backend/.local/push.properties`; that file must remain local. Real payments are not connected.

## Verification

- Backend: `./gradlew test` (Windows: `./gradlew.bat test`).
- Frontend: `npm run build`.

## Repository contents

Commit source code, tests, package manifests, lockfiles, the Gradle wrapper, and SQL schema changes in `backend/sql`.
Do not commit `node_modules`, builds, uploads, logs, database dumps, local secrets, or dated backup folders.
Ignoring a previously tracked file requires removing it from the Git index; this does not erase old commits.
Uploaded photos and the database are not included in clones. Back them up separately if needed.

## Current behavior

- Daily recommendations refresh at 08:00 and 20:00 Asia/Seoul, with a new daily cycle at 08:00.
- Additional recommendations cost 10 credits per card with no daily purchase cap; only eligible, not-yet-selected candidates can be purchased.
- Customer support uses direct inquiries and administrator replies; there is no chatbot.
- Report handling supports warnings, temporary suspensions and permanent suspensions.
- Terms are test drafts; account withdrawal disables the account rather than purging all database records.
