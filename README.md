# Dating App

Spring Boot / MariaDB backend and Vue / Vite frontend.

## Local development

- Java 17, MariaDB, Node.js and npm are required.
- Create a local database and account, then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend terminal.
- Backend: from `backend`, run `./gradlew bootRun --args="--server.port=8082"` (Windows: `./gradlew.bat`).
- Frontend: from `frontend`, run `npm ci`, then `npm run dev`.
- Open http://localhost:5173. The development server proxies `/api` and `/uploads` to port 8082.
- Admin console: http://localhost:5173/admin. Account/password login replaces the old shared key. See `docs/security-review-2026-10-29.md` for roles and bootstrap setup.
- Configure a random `JWT_SECRET` (at least 32 bytes) or `app.jwt.secret` in ignored `backend/.local/admin.properties`. There is no built-in default. `ADMIN_REVIEW_KEY` no longer grants access.
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


## Google login (Firebase Authentication)

1. In Firebase project settings register a **Web app**. Android/iOS apps can later be registered in the same project.
2. Enable **Authentication > Sign-in method > Google**, and choose a support email.
3. Add your site hostname to Authentication > Settings > Authorized domains. Add `localhost` for local development; use an HTTPS domain for mobile-device testing instead of assuming a LAN IP works with Google OAuth.
4. Copy the four `VITE_FIREBASE_*` values from `frontend/.env.example` into `frontend/.env.local` using your Web app's firebaseConfig. Restart Vite after changing them. These are public client configuration, never admin credentials.
5. Set backend `FIREBASE_PROJECT_ID` to the same project. Configure Application Default Credentials: for local development, save a Firebase service-account JSON outside Git and set `GOOGLE_APPLICATION_CREDENTIALS` to its absolute path. Never put this file in frontend/public, uploads, or a VITE environment variable. Production should prefer a managed service identity. The identity needs Firebase Authentication read permission to check disabled users and revoked tokens.
6. Restart the backend. Server-side Firebase Admin verifies signatures, issuer, audience, expiration, Google provider, verified email, revocation and disabled users. Firebase auth is exchanged for the existing application session so local suspensions and account review still apply.

New Google users complete the existing consent/profile form before a PENDING_REVIEW account is created. Existing email accounts are linked only after verifying their existing password; email equality alone never links them. Google authentication is kept in memory during signup, so refreshing the signup page requires authenticating again. Existing email login remains available.

Configuration alone is not an end-to-end verification: test a real Google popup on the authorized hostname, new-user signup, existing-account linking, popup cancellation and suspended-account rejection before launch.

### 로컬 Firebase 설정을 다른 PC로 옮기기
현재 로컬 설정은 `backend/.local/firebase.properties`에 있으며 인증 파일을 `backend/.local/firebase-admin.json` 상대경로로 읽습니다. 백엔드는 `backend` 디렉터리에서 실행하세요. 두 파일과 `frontend/.env.local`은 Git에서 제외됩니다. 폴더 복사 시 함께 옮기고, Git clone으로 옮기는 경우 이 파일들은 별도로 안전하게 복사해야 합니다. 비공개 키를 GitHub에 커밋하지 마세요. 새 PC에는 Java/Node/MariaDB와 해당 PC의 DB 설정도 필요합니다.

## Administrator accounts and security (2026-10-29)
- Local initial login details: `backend/.local/admin-first-login.txt` (Git ignored). Change the password in 관리자 계정·권한 after first login.
- On a new deployment, configure `app.admin.bootstrap-username` and `app.admin.bootstrap-password` in `backend/.local/admin.properties`. Bootstrap runs only when no administrator exists. Use at least 12 characters and no more than 72 UTF-8 bytes.
- OWNER manages administrators and all operations; REVIEWER handles signup/profile decisions; SUPPORT handles inquiries/reports; READ_ONLY only reads. Password/role/activation changes revoke existing admin sessions.
- HTTPS deployment: set `app.auth.refresh-cookie-secure=true` and `app.cors.origins` to the exact frontend origin(s). Keep all `.local` files private.
- Uploaded photos are private: authenticated `/api/media/...` for members, `/api/admin/media/...` for admins. Do not expose the uploads folder through nginx/static hosting.
- Email verification and password reset are disabled until real email delivery is implemented: the old endpoints disclosed development codes/tokens. Existing email login and Google login still work. Never enable `app.auth.dev-verification=true` on a public server.
- Public legal page: `/legal`; operator 양종현 / sku2013@naver.com. Retention and deletion gaps are recorded in the security report, not silently described as implemented.
