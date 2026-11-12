# 최신 현황: 2026-11-12

[이번 배포 기능·검증·제한 사항](changes-2026-11-12.md)을 먼저 읽으세요. 아래 과거 날짜의 미구현/미배포 설명보다 최신 기록을 우선합니다.

# 작업 현황 — 2026-11-04

## 구조와 환경

- backend: Java 17, Spring Boot 4.0.5, MariaDB, Gradle wrapper.
- frontend: Vue / Vite, Capacitor Android. Node 버전은 설치된 의존성 요구조건도 확인한다.
- 소스 저장소: https://github.com/facitea/dating-app (origin). previous-origin은 친구의 예전 저장소이며 push 대상이 아니다.
- 클라우드: 서울 Lightsail geureomuri-server, Ubuntu 24.04, 2GB, 고정 IP 3.34.208.199.
- URL: https://geureomuri.3.34.208.199.sslip.io / 관리자 /admin/overview (/admin/login도 redirect).
- DB, 업로드 사진, 비밀 설정은 GitHub에 없다. 클라우드는 새 DB로 시작했고 로컬 계정을 이관하지 않았다. 이후 가입 데이터가 존재하므로 빈 DB로 가정하지 않는다.

## 최근 반영한 내용

- Capacitor Android 프로젝트와 빌드 스크립트. 웹 API는 동일 origin /api, Android는 빌드 시 ANDROID_API_URL 지정.
- Firebase SDK 중복 레지스트리로 빈 화면이 발생했던 문제: vite.config.js resolve.dedupe로 해결.
- 사진 자르기: 미리보기 높이를 제한하고 확대/완료 버튼을 처음부터 노출. 360x640 화면에서 버튼이 보이는 것 확인.
- /admin/login이 잘못된 section으로 해석돼 로그인 후 본문이 비던 문제: /admin/overview redirect 추가. 클라우드 관리자 실제 로그인 및 현황 조회 확인.
- AWS 무료 크레딧 테스트 충전 활성화. 100/300/500 상품, 실제 결제 아님. 서버 설정 적용/재기동 확인, 사용자 충전 실행은 별도 확인 필요.
- 웹 수정은 AWS에도 배포했다. 최신 사진/관리자 UI 변경 이후 APK를 다시 만들지는 않았다. 이전 APK를 최신으로 착각하지 않는다.

## 주요 제품 동작

- 이성 추천, 오전/오후 08:00/20:00 한국시간 회차. 상세 추천·제외·매칭 표시 규칙은 기존 matching 문서와 코드를 확인.
- 추가 인연 10크레딧, 하루 구매 수 제한 없음. 없는 후보를 유료로 판매하지 않는다.
- 대화 요청, 크레딧 반환, 알림, 신고·정지, 관리자 역할, 차단, 프로필 재심사 기능은 기존 구현을 유지한다.
- 성향 태그와 양방향 점수는 설문 기반 점수다. UI의 '좋아할 확률'을 통계적으로 검증된 실제 확률로 주장하지 않는다.

## 남은 일 / 출시 제한

- 실제 PG/Google Play Billing 연동 없음. 공개 출시 전 테스트 충전 비활성화 필요.
- 신규 Firebase 이메일 계정의 인증/비밀번호 재설정은 Firebase 메일 사용. 기존 자체 이메일 계정의 분실 비밀번호 자동 복구는 미구현; 개발 인증코드 공개로 대체하지 않는다.
- Firebase Android google-services.json 미구성, Android Google 로그인/FCM 푸시 미완료.
- 실기기 Android 전체 플로우, Google 신규가입/기존계정 연결/정지계정 시나리오 전체 검증은 미완료.
- DB는 Hibernate ddl-auto:update. 버전 관리 마이그레이션 및 복구 연습 필요.
- 매일 03:00 KST 서버 내부 백업, 초기 PC 백업 1회. 외부 자동 백업/보관 기간/복원 검증은 미완료.
- 개인정보 보관·탈퇴 삭제와 법적 안내는 기존 security-review 문서의 미해결 항목 확인. 운영 준비 완료로 단정하지 않는다.
- 의존성 audit 경고와 공개 서비스 보안 검토는 별도 작업 필요.

## 작업 재개 순서

1. AGENTS.md → NEW-PC.md → 배포 문서를 읽고 git status와 현재 설정 확인.
2. 로컬에서 수정·검증, GitHub 커밋. 배포 요청 범위를 확인해서 별도로 AWS 반영.
3. DB/사진 삭제 금지. 공개 업로드 경로를 열지 않는다. 신규 release 디렉터리와 rollback 가능한 symlink를 사용.
4. 완료 시 로컬/GitHub/AWS/APK 각각의 반영 상태와 미검증 사항 보고.


## 2026-11-04 안전 모니터링 / 셀피 수동 심사

- AWS release `/srv/geureomuri/releases/safety-20261104`, 이전 release `review-notice-20261103`; 배포 전 백업 `/srv/geureomuri/backups/20261104T003754Z`.
- 신규 유료 대화 요청 하루 7명, 한국시간 자정 초기화. 거절·환불도 횟수 포함. 초과 요청은 차감 전에 차단. 기존 대화/상호 매칭 대화는 제외.
- 외부 링크·금전 표현의 서버 경고 확인, 수신자 경고, 관리자 이벤트 기록. 정지 이메일/Firebase UID 재가입 제한.
- 사용자 명시 선택에 따라 프로필 키워드 점수만으로 자동 정지/로그인 차단하지 않고 관리자 검토. 기존 정지 계정은 자동 해제하지 않음.
- `/admin/monitor`: 신호/점수/검토·경고·기간/영구 정지, 기존 위험 프로필 목록, 셀피 수동 심사. score는 사기 확률이 아님.
- 설정 → 셀피 확인(`/profile/selfie`): 카메라 촬영, 별도 선택 동의, 대표 사진과 수동 비교. 자동 얼굴 대조·라이브니스·안면 인증은 구현하지 않았으며 그런 배지를 부여하지 않음.
- 셀피는 일반 회원/공개 미디어 주소에서 읽을 수 없음. OWNER/REVIEWER만 열람·심사, 열람 감사 이력. 심사 후 30일 또는 미심사 제출 후 30일 열람 만료, 시간당 원본 정리. 동의 철회/탈퇴도 열람 만료. 정기 파일 백업에서 selfie-review 폴더 제외하도록 서버 스크립트 교체.
- Backend 전체 162 tests PASS, frontend production build PASS. 브라우저 UI는 격리된 mock API와 synthetic camera stream으로 촬영/미리보기/동의/요청, 관리자 조치 입력 확인. 임시 harness는 삭제함.
- AWS 웹 200, 미인증 monitor 401, selfie 403, 공개 셀피 파일 경로 404 확인. 저장된 초기 관리자 자격정보를 이용한 라이브 로그인은 401로 실패해 반복 시도/비밀번호 변경하지 않음. 사용자 현재 관리자 로그인 및 실기기 카메라 테스트 필요.
- 이 변경은 로컬 및 AWS에 반영. GitHub commit/push 및 APK 갱신은 수행하지 않음. 기존 ProfileEdit.vue 미커밋 수정도 보존.
- 상세 제약 및 보관 정책: `docs/safety-monitor-2026-11-04.md`.


## 2026-11-04 Firebase 이메일 로그인 / 테스트 회원 6명

- Firebase `dating-260d2` 이메일/비밀번호 제공업체 활성화. Google 제공업체 유지.
- 프런트 이메일 로그인, 가입 인증 링크 확인/재발송, 비밀번호 재설정 메일. 신규 서버 계정은 FIREBASE_EMAIL, 서버 비밀번호 저장 없음. 이메일 미확인/다른 제공업체 토큰은 거부.
- `/api/auth/firebase/email`: Firebase Admin으로 토큰·이메일 확인·provider=password·최근 인증·철회 여부 검사. 신규 Firebase 사용자도 앱 약관/프로필/심사를 완료해야 회원 가입. 이메일 주소만 일치한다고 기존 계정 자동 연결하지 않음.
- 기존 LOCAL 비밀번호 로그인 fallback 유지. Firebase 연결은 기존 LOCAL 비밀번호 증명 필요. 연결해도 기존 비밀번호는 유지됨.
- 기존 사용자 승인에 따라 키워드 위험 점수만으로 refresh를 거부하던 잔여 분기 제거. 실제 정지/탈퇴/차단 신원 제한은 유지.
- AWS release `/srv/geureomuri/releases/firebase-email-20261104`, 이전 `safety-20261104`, 배포 전 백업 `/srv/geureomuri/backups/20261104T010728Z`. 서버 백업 스크립트 CRLF를 LF로 복구하고 실제 백업 실행 확인.
- 사용자 요청으로 AWS에 남녀 각 3명만 생성: `test.m01@dating.test`~`test.m03@dating.test`, `test.f01@dating.test`~`test.f03@dating.test`. UID `geureomuri-test-m01` 등. 모두 닉네임 테스트 표시, ACTIVE, 100 테스트 크레딧, 비공개 테스트 아바타 2장, 성향/이상형 카테고리별 최대 3개. 추천 기본 조건 ANY.
- 이 6개는 관리자 생성 가상 인증 계정으로 emailVerified=true이며 실제 메일 수신을 검증한 것이 아님. 전화번호/성별/셀피 본인 인증은 부여하지 않음. 실제 신규 회원의 이메일 확인·심사를 생략하는 코드 없음. 실제 회원 데이터 변경 없음. 로컬 DB에 가계정을 복제하지 않음.
- 비밀번호 파일은 `.local/cloud/firebase-test-accounts.txt`와 JSON이며 gitignore 확인됨. 공개 문서에 비밀번호를 적거나 커밋하지 말 것.
- backend 전체 169 tests PASS, frontend production build PASS. 6명 모두 실제 Firebase 비밀번호 로그인 → AWS 인증 세션 → 프로필/비공개 사진 2장 PASS, 공개 uploads 404. 브라우저 남성 테스트 로그인·오늘의 인연·프로필 표시 확인.
- 실제 수신함으로 인증/재설정 메일 수신 및 링크 클릭은 미검증. Firebase 비밀번호 재설정은 기존 앱 발급 JWT/refresh를 즉시 회수하지 않으므로 필요한 경우 설정의 모든 기기 로그아웃 사용. 이 연동은 후속 보강 필요.
- 로컬 소스와 AWS 웹/백엔드 반영. GitHub commit/push 및 Android APK 갱신은 미실행.

## 2026-11-04 크레딧 부족 안내
- 대화 요청/추가 인연의 잔액 부족 시 공통 네·아니오 모달. 네는 /credits, 아니오는 현재 화면 유지. 구매 API 409 크레딧 부족도 처리.
- frontend build PASS. 별도 mock 잔액 0 UI에서 아니오 유지·네 /credits 이동 확인. 실제 회원 잔액 변경 없음. 임시 harness 삭제.
- AWS release credit-prompt-20261104, 이전 firebase-email-20261104. 로컬/AWS 반영, GitHub/APK 미반영.


## 2026-11-04 가입 필수 셀피 / 가입심사 통합
- 가입 사진 등록 단계에 필수 셀피 촬영 모달. 별도 필수 수집·보관 동의 후 카메라 시작. 영상·미리보기 좌우 반전, 저장 원본은 반전하지 않음. 촬영·재촬영·제출 버튼 고정.
- ProfileImageService 심사 신청은 현재 대표 사진과 연결된 유효한 PENDING/APPROVED 셀피 및 동의 기록 필요. AdminReviewService 가입 승인은 APPROVED 셀피 필수. 철회·만료·대표 사진 변경이면 승인 차단. 기존 ACTIVE 회원 재가입 요구 없음.
- /admin/signup 회원 상세에 AdminSelfiePanel: 대표사진/셀피 비교, 사유와 수동 확인·보완 요청. OWNER/REVIEWER만 접근. /api/admin/selfies/user/{userId}도 동일 권한. 사진 열람 감사 이력 유지.
- 자동 얼굴 대조/좌우 고개 동작 인식/라이브니스는 구현하지 않음. 수동 확인이며 본인·성별 인증을 보증하지 않음. 기존 랜덤 손동작 요청 유지.
- required consent version selfie-signup-required-2026-11-04. 심사 후 30일, 미심사 제출 후 30일 보관 및 정리 정책 유지. 개인정보 안내 업데이트.
- backend 171 tests PASS, frontend build PASS. 합성 카메라+mock API에서 필수 동의/촬영/미리보기/제출/관리자 비교·확인 완료 검증. 실제 휴대폰 카메라와 라이브 관리자 로그인 테스트는 미완료. 임시 harness 삭제.
- AWS release signup-selfie-20261104, 이전 credit-prompt-20261104, 백업 /srv/geureomuri/backups/20261104T013637Z. 로컬/AWS 반영, GitHub/APK 미반영.

## 2026-11-04 프로필 사진 이력 / 압축
- 사용자 선택: 교체·삭제 후 90일, 탈퇴 시 이력 사진 삭제. 기존 셀피 30일 정책은 유지.
- 신규 프로필 업로드는 최대 긴 변 1600px JPEG 품질 .85, 재인코딩으로 메타데이터 제거. 기존 파일 일괄 변환은 하지 않음.
- ProfileImageService 교체·삭제 전에 ProfilePhotoHistoryService가 이전 사진을 최대 960px JPEG 품질 .82로 보관. 등록 중인 사진은 이후 교체·삭제될 때 자동 보관. 과거 이미 삭제된 파일의 소유·변경 이력 복원은 미구현.
- 관리자 회원 상세 AdminPhotoHistory에서 날짜·교체/삭제·보관 종료·크기 표시. OWNER/REVIEWER만 전용 API 열람, 감사 기록, no-store. 공개 uploads나 일반 미디어 API로 접근 불가.
- 만료 시 즉시 API 차단, 시간당 최대 100건 물리 삭제. 탈퇴 시 즉시 API 차단과 commit 후 이력 압축 파일 삭제 시도; 실패는 정기 작업에서 재시도. 실제 사용 중인 현재/승인 사진은 보존. 미사용 원본은 사용자 잠금 후 참조 재검사하고 정리. 일반 프로필 원본 및 과거 백업 전체의 탈퇴 삭제를 완료한 것으로 간주하지 말 것.
- 보관용 photo-history 폴더는 정기 파일 백업 제외. 과거 백업 원본은 삭제하지 않았으며 별도 보관 정책 필요. 개인정보 안내 및 사진 편집 안내 업데이트.
- backend 전체 175 tests PASS 후 동시성 보호 수정의 ProfilePhotoHistoryTest 4 tests 재통과. frontend build PASS. mock UI에서 두 사진의 교체/삭제·기한·용량 확인, 임시 harness 제거. 운영 회원 사진을 테스트용으로 변경하지 않음.
- 최종 AWS release photo-history-v2-20261104, 백업 /srv/geureomuri/backups/20261104T020019Z. 로컬/AWS 반영, GitHub/APK 미반영.

## 2026-11-04 가입 단계 정리 / 운영자 셀피 요청 / 사진 확대
- 가입 이메일 비밀번호 확인란 추가, 인증 계정 생성 전에 두 입력의 일치 확인. Google 가입은 별도 비밀번호 불필요.
- 가입 셀피 필수는 유지. ReviewPending은 프로필 사진 → 셀피 → 심사 대기 한 단계씩 표시. 승인 시 /today 자동 이동, /signup-complete도 /today redirect. 기존 승인 회원은 설정의 셀피 메뉴 제거.
- OWNER/REVIEWER가 회원 상세에서 사유를 적어 추가 셀피 요청·취소 가능. users.selfie_required 기본 false. 요청 회원은 접속 시 /selfie-required, 확인 완료까지 추천·채팅 API 409 SELFIE_REQUIRED. 셀피/본인 프로필 사진/계정관리/고객센터 등 해결에 필요한 경로는 허용. 승인·취소 시 제한 해제. 가입심사 중인 회원의 필수 셀피 조건은 요청 플래그와 별개.
- 셀피 정상 승인 코멘트 선택 (빈 값이면 시스템 기록 셀피 확인 완료), 보완 요청 사유 필수. 최신 셀피만 심사 가능. 요청 취소 후 기존 촬영 제출 불가.
- 촬영 요청 자세 6종: V, 손바닥, 엄지, 검지 1개, 손가락 3개, 주먹. 자동 얼굴 대조나 동작 인식 아님. 촬영창의 운영자/문의 이메일·좌우반전 설명 삭제; 개인정보 안내의 연락처와 보관 정책 유지.
- 관리자 프로필/승인 사진·셀피·사진 이력을 클릭하거나 키보드로 확대, 1~3배 조절. 기존 인증된 이미지 URL을 재사용하며 공개 미디어 경로를 만들지 않음.
- backend 전체 179 tests PASS, frontend build PASS. mock UI에서 빈 코멘트 확인 완료·보완 버튼 비활성, 확대 열기·닫기 확인. 가입 화면 비밀번호 불일치 표시 확인. 실기기 카메라와 실제 회원 제한은 시험하지 않음. 임시 UI harness 삭제.
- AWS release onboarding-20261104, 백업 /srv/geureomuri/backups/20261104T021527Z. 로컬/AWS 반영, GitHub/APK 미반영. 중복 사진 탐지는 아직 제안 단계.


## 2026-11-04 중복 사진 의심 갤러리
- 관리자 `/admin/photos`, OWNER/REVIEWER 전용 API `/api/admin/photo-duplicates`. 사진은 기존 인증 media API 사용, 공개 URL 추가 없음.
- 서로 다른 계정의 현재 사진만 검사. SHA-256 동일 파일 + 64-bit dHash(거리 3 이하, 저정보량 사진 제외)로 후보 생성. 얼굴 인식/신원 판정 아님. 자동 정지 없음.
- 시작 45초 후, 이후 1분마다 최대 10장. 디코더 subsampling으로 최대 약 128px 비교용 이미지를 생성하며 원본 10MB/20MP 상한. 사진당 한 번 처리, fingerprint만 저장, 새 사진 복사본 없음.
- 인덱스된 SHA와 4개 16-bit band별 최대 200개 후보만 비교. 대량 중복 그룹은 모든 쌍을 열거하지 않을 수 있음. 크롭/회전/큰 보정은 놓칠 수 있으며 유사 사진의 오탐은 관리자 판단 필요.
- 교체/삭제/탈퇴 즉시 갤러리에서 제외, 다음 순차 정리에서 fingerprint와 연결된 검토 쌍 삭제(회당 최대 100개). 읽기 실패 사진은 반복 재시도하지 않으며 새 사진 업로드로 재처리.
- 확인 완료/되돌리기는 감사 로그 기록. 회원 상세에서 기존 셀피 요청 기능 연결. 기존 사진 이력 90일과 셀피 30일 보관 정책 변경 없음.
- backend tests 및 bootJar, frontend npm run build 통과. mock UI에서 확대/완료/되돌리기 검증. 실회원에 셀피 요청/제재하지 않음.
- AWS release `/srv/geureomuri/releases/photo-duplicates-20261104`, 이전 onboarding-20261104. GitHub/APK 반영 안 함.


## 2026-11-04 Android 로그인 / 웹 병행 빌드
- Firebase Android 앱 `com.facitea.geureomuri` 등록, 이 PC debug SHA-1/SHA-256 등록 완료. 앱 ID `1:589048607927:android:1a8adf8995cfebf26f0932`. 무료 Spark 유지.
- 공식 Firebase Management API로 `frontend/android/app/google-services.json` 저장. Git 제외 유지. 웹 Firebase 앱/도메인/로그인 공급자 유지.
- `npm run build` -> `frontend/dist` (웹), `npm run android:sync` -> `frontend/dist-android` (앱), Capacitor webDir=dist-android. 앱 빌드 전후 웹 index 해시 동일 확인. 웹 배포는 반드시 dist만 사용.
- Android 빌드는 Firebase 설정 누락 및 웹 OAuth client 누락 시 실패하도록 변경. native Google 로그인, 웹 popup 로그인 분기 유지. 테스트 versionCode=2, versionName=1.0.1-test.
- 공통 API 로그인 갱신을 single-flight로 변경. 동시 401 요청 시 한 번만 쿠키 갱신. 일시적 네트워크 오류는 저장된 세션을 삭제하지 않고, refresh 401/403만 삭제.
- Android CAMERA 권한 추가: getUserMedia 셀피 촬영 지원. 실기기 권한 허용/카메라/구글 계정 선택은 기기 미연결로 아직 검증 불가.
- 사진 원본 10MB 선택 제한/문구 제거. crop preview 최대 2048px, 서버 업로드는 600x600 JPEG quality .9. 12,979,946-byte PNG가 브라우저에서 230,490-byte JPEG로 변환됨. 서버 직접 호출용 multipart/이미지 보호 상한은 그대로 유지(프런트 압축 우회 방지).
- 웹 build / Android assembleDebug / APK signature 검증 완료. `node --experimental-vm-modules scripts/test-session.mjs`로 동시 갱신·오프라인·인증 만료·무한 재시도 방지 검증.
- APK는 테스트용 debug 서명. FCM native push는 아직 미구현, 기존 앱 내 알림함 사용. 실제 결제 없음.
- AWS release `/srv/geureomuri/releases/android-login-20261104` (기존 photo-duplicates-20261104). GitHub push 안 함.

## 2026-11-04 대화 요청 500 수정
- DirectRequest/SendRequest safetyAcknowledged 누락·null의 Jackson 3 primitive 역직렬화 실패 수정. Boolean 입력을 false로 정규화, true 명시 시에만 안전 경고 확인 인정.
- MessageRequestJsonTest, SafetyControlsTest, SocialServiceTest 및 bootJar 통과. AWS chat-ack-20261104 배포, 기존 웹 그대로 유지. APK 재설치 불필요. GitHub 미반영.

## 2026-11-04 서비스 보상 / 신고 대화 / 앱 푸시
- 관리자 회원 상세에 보상 크레딧: OWNER/SUPPORT, 1~10,000 C, 메시지 필수, 확인 후 지급. 회원 잠금·요청 UUID로 중복 지급 방지. credit_entries에 message/admin_id 저장, 알림함·사용내역에 서비스 보상 표시. 실회원에게 테스트 지급하지 않음.
- 채팅 신고에서 상대 메시지 최대 10개 선택. 서버에서 해당 두 회원의 대화방/발신자 검증. 관리자 신고 상세에서 선택 메시지와 앞뒤 각 5개 조회; 선택 없는 기존 신고는 접수 전 최근 100개. OWNER/SUPPORT/REVIEWER만, 열람 감사 기록. 메시지 내용은 기존 불변 chat_messages를 참조하며 별도 사진/증거 파일 복제 없음.
- 상호 좋아요의 무료 매칭은 기존 유지, 회귀 테스트 통과. 미확인 MATCH 알림에 축하 팝업·확인하러 가기·닫기 추가. 확인 시 알림 읽음 처리.
- 채팅 textarea disabled 제거, 전송 버튼 pointerdown 초점 유지 및 완료 후 복원. 전송 도중 추가 입력 보존. 가입 심사 대기 화면 기존 색상/모바일 구성으로 정리.
- Android @capacitor/push-notifications + FCM token 등록/해제, 네이티브 권한/채널/알림 탭 이동 구현. 웹 VAPID 방식 유지. 잠금화면에는 메시지 본문/신고 상세 노출하지 않음. 기기당 계정 연결·로그아웃 해제·sessionVersion 검사. 기존 알림 설정/방해금지 적용.
- FCM dry-run은 유효하지 않은 테스트 토큰으로 INVALID_ARGUMENT 응답 확인(실발송 없음). 실제 Android 토큰 등록/백그라운드 수신/키보드 유지 동작은 실기기 미연결로 아직 검증하지 못함.
- 전체 backend tests 통과 후 신규 ServiceSupportTest 4개·FirebaseEmailLoginTest 재검증, frontend build 및 Android assembleDebug 통과. mock UI 보상 확인/완료·대화 증거·메시지 선택 신고·커서 유지·매칭 팝업 확인. 프리뷰 파일은 제거.
- AWS release service-support-20261104, 이전 chat-ack-20261104. 웹/서버 반영, APK 1.0.2-test(versionCode 3). GitHub push 안 함.
- 참고: https://capacitorjs.com/docs/guides/push-notifications-firebase 및 https://firebase.google.com/docs/cloud-messaging/send/admin-sdk

## 2026-11-04 셀피 필수 화면 간소화
- /selfie-required: 제목·한 줄 안내와 하단 인증하기/새로고침만 표시. 사진 관리/고객센터/로그아웃 링크 제거. 재촬영 사유는 RETRY일 때만 표시.
- 제출 후 확인 중 버튼 비활성, 자동 상태 확인·완료 후 이동 유지. 백그라운드 갱신 시 버튼 깜박임 제거, 중복 요청/언마운트 후 이동 방지.
- 실제 컴포넌트 mock UI에서 인증 모달·새로고침 확인. 웹/Android 빌드 통과. AWS selfie-ui-20261104 및 APK 1.0.3-test(versionCode 4), GitHub 미반영.

## 2026-11-04 관리자 셀피 대기 알림
- /admin/selfies 전용 메뉴: 기존 회원 재인증/신규 가입 구분, 제출순 30명 페이지, 회원 상세 심사 연결. OWNER/REVIEWER만 목록/빨간 배지 표시, 기존 API 권한 유지.
- /api/admin/selfies/pending: 최신 제출 PENDING, 만료 전 사진 존재, 활성 재인증 요청 또는 가입 심사 대상만 집계. 취소/탈퇴/만료/이전 제출 제외.
- 관리자 메뉴 빨간 대기 수와 운영 현황 안내. 화면 활성 시 15초 갱신, 심사 완료/보완/요청 취소 이벤트 즉시 갱신.
- SelfieReviewTest 및 SelfieQueueTest, frontend build 확인. AWS selfie-queue-20261104 반영. 관리자 웹 변경이므로 APK 미갱신, GitHub 미반영.

## 2026-11-04 관리자 회원 차단·신고 목록
- 회원 상세의 차단·신고한 회원 → 목록 확인. OWNER/SUPPORT만 조회, VIEW_MEMBER_BLOCKS 감사 기록. 30명 페이지, 조회할 때만 API 요청.
- 해당 회원이 직접 차단한 상대와 해당 회원이 신고한 상대를 합쳐 중복 없이 표시. 직접 차단/신고로 접촉 제한 구분, 상대 상세 열기. 반대 방향 차단·신고는 포함하지 않음.
- 일반 회원의 설정 차단 목록은 기존 직접 차단만 유지. 신고 이력에 따른 접촉 제한 정책 그대로, 관리자 임의 차단 해제 기능 추가하지 않음. 기존 데이터에 차단 시각이 없어 날짜 표시하지 않음.
- AdminMemberBlocksTest: 방향/중복/해제 후 신고 제한/권한 확인, frontend build 및 mock UI 조회 검증. AWS admin-blocks-20261104 반영, 관리자 웹 변경이라 APK 미갱신. GitHub 미반영.

## 2026-11-04 관리자 차단 해제
- OWNER/SUPPORT 회원 상세 차단·신고 목록에서 사유(1~200자) 입력 후 확인 해제. 사용자 쌍 ID 순 잠금, 감사 기록. 직접 차단 해제와 해당 방향의 신고 접촉 제한 해제를 함께 수행.
- UserReport.contactReleasedAt/By/Reason 추가. 신고 내용·OPEN/RESOLVED 상태·제재는 그대로 보존. 새 신고는 기본 null로 다시 접촉 제한. 반대 방향의 차단/신고는 유지, 정지·탈퇴·매칭 해제 상태도 유지.
- SocialService/DailyMatchService는 해제되지 않은 신고만 접촉 제한으로 취급. 해제된 상대는 관리자 제한 목록에서 제외, 신고 기록은 고객센터 신고 목록에 유지. 누적 차단 횟수 유지.
- AdminMemberBlocksTest/SocialServiceTest 통과, frontend build, mock UI 사유 필수/해제 성공 확인. 실회원 차단 해제 테스트 미실행. AWS admin-unblock-20261104 배포, APK/GitHub 미갱신.

## 2026-11-04 매칭 해제 후 재매칭
- 종료된 방은 메시지/유료 요청 차단 유지, 좋아요만 두 회원 ACTIVE·차단/미해제 신고 없음 조건으로 허용. 양쪽의 새 좋아요가 모이면 ended=false, matched=true, 무료 대화 재개. 반대 좋아요는 unmatch에서 이미 비활성화하므로 일방 좋아요만으로 재개하지 않음.
- 재매칭 시 matchGeneration 증가, 회차별 MATCH 알림 키. 반복 좋아요는 새 축하 알림 생성하지 않음. 재전송 좋아요 알림 키는 재활성 시각 포함.
- 매칭 해제 시 해당 MATCH 알림 읽음/푸시 종료. 기존 종료된 방의 오래된 MATCH 알림도 inbox 조회 시 제외하고 읽음 처리. 기존 종료 방을 일괄 복구하지 않음.
- 서로 재동의한 재매칭 시 매칭/채팅 숨김 해제, 채팅 삭제 지점은 보존. 신고·차단·정지 제한은 유지.
- SocialServiceTest/SafetyControlsTest로 양방향 목록·재매칭·메시지 수신·중복 알림·차단/신고 우회 방지 검증. AWS rematch-20261104, 웹/서버 반영. APK 재빌드/GitHub push 안 함.
