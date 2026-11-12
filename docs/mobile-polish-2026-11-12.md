# 모바일 UI 보정 — 2026-11-12

- AWS release: `/srv/geureomuri/releases/mobile-polish-20261112`
- 배포 직전 백업: `/srv/geureomuri/backups/20261112T090824Z`
- 이전 release: `community-20261112` (데이터·비밀 설정 유지)
- APK: 1.0.5-test / versionCode 6. 웹은 dist, Android는 dist-android로 각각 빌드.

## 변경
- 사용자 제목: 20px / 42px 헤더, 상단 safe area + 20px 기준. 오늘의 인연·매칭·채팅·커뮤니티·설정에 같은 기준 적용.
- Capacitor 8 SystemBars가 주입하는 safe-area CSS 변수 사용. Keyboard.resizeOnFullScreen 중복 보정 제거 (설치된 Capacitor 구현도 해당 조합을 경고함).
- 채팅: visualViewport의 실제 가용 높이를 사용. 메시지 목록만 스크롤하고 입력창 아래 8px + 필요한 시스템 하단 여백만 유지. 기존 IME 전송·포커스 유지 동작 보존.
- 커뮤니티: 탭/목록/본문/작성 화면 개편. 제목 옆 76px 썸네일. 글당 사진 1장 선택·교체·제거 가능.
- 사진은 브라우저에서 긴 변 1200px JPEG로 압축하고 서버에서 재인코딩하여 메타데이터 제거. 서버는 2MB/400만 화소 제한을 유지한다.
- POST /api/community: 기존 JSON 및 사진 multipart 모두 지원. GET /api/community/{id}/photo는 활성 회원, 게시물 공개 여부, 양방향 차단/신고 제한을 매번 확인하며 no-store 사용.
- /api/admin/community/{id}/photo: OWNER/SUPPORT만 접근. 신고 관리에서 사진 확인·확대 가능.
- community_posts.image_key nullable 컬럼 추가. 사진은 기존 영속 업로드 경로의 community 하위 폴더에 저장. 공개 /uploads 경로는 열지 않음. 작성 롤백 시 새 사진 정리, 글 삭제 커밋 후 사진 삭제.

## 확인
- web build, Android sync/assembleDebug, APK 서명 검증 통과.
- CommunityServiceTest 6개 통과: 사진 저장/조회, 숨김·차단·정지·삭제 시 접근 거부, 잘못된 이미지/경로 거부 포함.
- 프런트 세션 회귀 테스트 통과.
- 실제 브라우저 390×844에서 제목/썸네일/필터/작성 화면 확인. safe-area 28px 모사 시 제목 가림 없음.
- 390×480 축소 viewport에서 채팅 입력창 아래 8px, 하단탭 미표시, 한글 전송 및 포커스 유지 확인.
- AWS 로그인/토큰 갱신, 커뮤니티/설정/매칭/지갑 API 확인. 비로그인 접근 거부 확인.
- 실제 Android 기기의 상태 표시줄·IME 애니메이션은 실기기 재확인 필요. 웹 viewport 모사가 실기기 검증을 대체하지 않음.

이전 기능 및 운영 제한은 changes-2026-11-12.md 참고.
