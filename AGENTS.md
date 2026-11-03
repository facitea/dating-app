# 그럼우리 작업 인수인계

먼저 README.md, docs/HANDOFF.md, docs/NEW-PC.md, docs/lightsail-deployment.md를 읽고 실제 상태와 대조한다.
문서의 기준일은 2026-11-03이다. 문서를 읽었다고 배포나 실기기 검증이 완료된 것으로 간주하지 않는다.

- 모든 로컬 경로는 저장소 루트 기준이다. 특정 Windows 사용자명/드라이브를 가정하지 않는다.
- backend: Java 17 / Spring Boot / MariaDB. frontend: Vue / Vite. Android: Capacitor / JDK 21.
- 로컬, AWS, GitHub, APK는 자동 동기화되지 않는다. 작업 종료 시 각각 무엇을 반영했는지 보고한다.
- 사용자 변경을 덮어쓰지 말고 git status와 diff를 먼저 확인한다.
- .local, .env, 서비스 계정 JSON, SSH/서명 키, 비밀번호, DB 덤프, 업로드 사진을 출력하거나 커밋하지 않는다.
- Git ignore는 이미 추적 중인 파일을 보호하지 않는다. stage된 변경도 검사한다.
- AWS의 /var/lib/mysql, /srv/geureomuri/data, /etc/geureomuri를 배포 중 삭제하거나 로컬 데이터로 덮어쓰지 않는다.
- 운영 DB 초기화, 테스트 계정 대량 생성, 방화벽 확대, 유료 리소스 생성은 일반 코드 수정에 포함시키지 않는다.
- 사진은 인증된 API로 제공한다. Nginx에서 uploads를 공개하거나 API 인증을 우회하지 않는다.
- AWS 테스트 충전은 사용자 요청으로 활성화돼 있다. 실제 결제 연동은 없다.
- 웹 배포는 반드시 npm run build 결과를 사용한다. android:sync 결과를 웹에 배포하지 않는다.
- 프런트 수정은 npm run build, 백엔드 수정은 관련 Gradle 테스트로 확인한다. UI 수정은 실제 브라우저에서 확인한다.
- 가입/심사/추천/채팅/크레딧 관련 기존 동작은 HANDOFF.md와 소스를 확인한다.
- 새 비밀 파일이 없으면 값을 추측하지 말고 안전한 복원 경로를 안내한다. 개인키를 채팅에 붙여넣도록 요청하지 않는다.
