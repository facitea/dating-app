# 작업 현황 — 2026-11-03

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
- 이메일 인증/비밀번호 재설정 발송 미구현. 개발 인증코드 공개를 활성화해서 대체하지 않는다.
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
