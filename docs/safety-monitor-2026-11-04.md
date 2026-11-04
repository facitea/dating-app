# Safety monitoring and manual selfie review — 2026-11-04

## Rules
- All accounts: at most seven NEW paid chat requests per Korea calendar day (00:00 reset). Existing conversations and mutual matches do not consume the quota. Declined/refunded requests count. Pair/member write locks serialize concurrent purchases; no credits are deducted for a blocked request.
- Existing acceptance gate permits only one initial message before acceptance. No new cap on ordinary accepted conversation messages was added.
- Server-side financial/link text rules require acknowledgement on the send API. Browser confirmation supports cancel. Recipients see a warning beside flagged messages. This is heuristic detection, not comprehensive scam/URL detection.
- Events survive rejected transaction rollback. Same user/type signals within 60 seconds are grouped by retaining the first event; this is not a total-attempt counter.
- Email (case-normalized) and Firebase UID sanctions deny registration until temporary expiry, or indefinitely for permanent sanctions. Existing manual identity blocks remain separate. A new unrelated email/Google identity cannot be linked to a person until verified identity/DI is integrated. No phone identity claim is made.
- User explicitly selected manual profile risk review. Keyword score alone no longer suspends or denies login. Explicit account sanctions/status remain enforced; existing suspended members are NOT released. The 0–100 score is a heuristic priority, not a fraud probability.

## Admin
- /admin/monitor: risk activity, scores, prior flagged profiles, review note, warning/temporary/permanent sanction.
- OWNER/SUPPORT can review risk events. OWNER/REVIEWER can review selfies. READ_ONLY/SUPPORT cannot access selfie images.
- Selfie image GETs are no-store and audited. Existing private media endpoints cannot read selfie-review paths.

## Selfies
- Settings → selfie check: optional explicit consent, camera capture, randomly requested hand pose, 10-minute challenge. Max three starts per rolling 24 hours. Browser resizes <=800 px; backend validates <=2 MB and <=2 million pixels, re-encodes image without metadata.
- This is MANUAL photo comparison, not face recognition, liveness, identity proof, or an anti-deepfake guarantee. Client capture can be bypassed by a modified client. No embeddings or face-match score are generated. Do not label users biometrically verified.
- Captures and reference copies are private files under configured uploads/selfie-review. Images must NOT be served by Nginx or public file hosting.
- Photo access stops at review+30 days, or submission+30 days if never reviewed. Hourly cleanup deletes originals in batches of 100. Outages/backlogs can delay physical cleanup, but API access checks expiry.
- Consent withdrawal/account withdrawal immediately expires photo access. Metadata (consent version/time, result, timestamps) remains. Public badges are not issued; changed reference photos invalidate displayed approval.

## Deployment gate and backup policy
- Before enabling real selfie collection on AWS, exclude uploads/selfie-review from all filesystem backups/snapshots that could outlive retention. Disk snapshots include deleted files/old copies; do not promise backup deletion without actual retention controls. The repository backup script now explicitly excludes uploads/selfie-review; install this version before collecting real selfies. Cloud disk snapshots must be managed separately.
- Never upload client files or test photos from local to AWS. Tables are currently created by Hibernate update; obtain DB backup first.
- Camera permission and real phone/Android hardware behavior still require user testing. No production face accuracy claim is supported.

## Verification and deployed state
- Full backend suite: 162 tests passed, frontend production build passed.
- Browser mock API/synthetic camera: capture, preview, explicit consent enabling submit, pending state, admin temporary sanction input verified. No real personal selfie was captured.
- AWS safety-20261104 deployed; app active, web 200, unauthenticated monitor 401/selfie 403, public selfie URL 404. Scheduled backup script exclusion installed. Initial admin credentials failed 401; no reset/repeated attempts. Authenticated live admin views and real mobile camera remain unverified.
- GitHub and Android APK not updated in this task.
