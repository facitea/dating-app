# Android 테스트 앱

- 패키지: com.facitea.geureomuri / 이름: 그럼우리
- Capacitor 8.6, Android SDK 36, 최소 Android 7(API 24), JDK 21.
- APK: frontend/android/app/build/outputs/apk/debug/app-debug.apk
- 기본 테스트 API: https://geureomuri.3.34.208.199.sslip.io/api. 외부 인터넷에서도 사용 가능. 로컬 API로 빌드할 때만 같은 LAN이 필요하다.
- 기존 demo 계정은 로컬 DB에만 있다. AWS로 자동 복사되지 않는다.

## 빌드 (PowerShell, frontend 폴더)

```powershell
$env:JAVA_HOME = Read-Host '이 PC의 JDK 21 설치 폴더'
$env:ANDROID_API_URL='https://geureomuri.3.34.208.199.sslip.io/api'
$env:ANDROID_DEBUG_HTTP='0'
npm ci
npm run android:sync
.\android\gradlew.bat -p android :app:assembleDebug
```

다른 PC에서는 android/local.properties에 Android SDK 경로를 설정한다. 공개 배포 시 ANDROID_API_URL을 HTTPS 서버로 바꾼다. 로컬 HTTP는 debug 전용 네트워크 설정의 지정 호스트만 허용한다. HTTP 주소로 동기화한 리소스는 release 빌드를 차단한다. 서명 키, local.properties, google-services.json, 빌드 산출물은 Git에 올리지 않는다.

## Firebase Android 설정 (2026-11-04 등록 완료, 아래 재설정 절차 참고)

1. 기존 dating-260d2 프로젝트에 Android 앱을 추가한다. 패키지명은 com.facitea.geureomuri.
2. 이 PC 테스트 서명 SHA-1: F1:26:7E:EC:3A:29:23:42:7D:5A:D7:93:2C:C3:F6:08:96:D7:F5:20
3. SHA-256: DC:15:F3:05:74:BF:7C:92:29:2C:2C:DB:48:C8:3A:EF:D9:87:B3:C9:E2:D1:E1:4A:28:51:6A:0D:ED:98:CC:E5
4. google-services.json을 frontend/android/app/google-services.json에 저장하고 android:sync 및 APK 빌드를 다시 한다. 웹 firebaseConfig 또는 관리자 서비스 계정 키로 대신하지 않는다.
5. 출시용 서명과 Play App Signing을 쓰면 해당 인증서 지문도 별도로 등록한다.

파일이 없으면 구글 로그인을 사용할 수 없다는 안내를 표시하고 네이티브 Firebase 인증 플러그인의 시작 로드를 제외한다. 파일이 있으면 네이티브 로그인 후 Firebase ID token을 기존 서버에 전달한다. 플러그인의 peer dependency에 맞춰 웹 Firebase SDK를 12.19로 맞췄다.

## 구현 및 검증 범위

- 웹 화면을 앱 내부에 포함하며 서버 페이지를 통째로 띄우는 live-reload 방식이 아니다.
- 앱 뒤로가기는 열린 dialog를 먼저 닫고, 하위 화면은 뒤로 이동하며 기본 탭에서는 앱을 최소화한다.
- native HTTP/cookie 브리지를 활성화하여 기존 HttpOnly refresh cookie 인증 계약을 유지한다. 서버의 토큰 반환 정책이나 CORS를 완화하지 않았다.
- Android adjustResize와 keyboard 플러그인 설정, 브랜드 색상 아이콘/시작 배경 적용.
- 프로덕션 웹 빌드와 Android debug APK 빌드 통과. APK 서명/패키지/최소 SDK 확인, LAN 주소를 통한 이메일 로그인 및 cookie refresh HTTP 검사 통과.
- 현재 연결된 Android 기기와 에뮬레이터는 없다. 실제 앱 시작, 사진 선택/자르기/업로드, 키보드와 safe area, WebView 안에서의 로그인 유지 및 뒤로가기는 실기기 확인이 필요하다.
- 구글 로그인은 Firebase Android 설정 후 실기기 검증이 필요하다.
- 기존 웹 푸시는 앱에서 활성화하지 않는다. Android FCM 토큰 등록/서버 발송/알림 클릭 이동은 후속 구현이 필요하다. 앱 내 알림함은 기존 API를 사용한다.
- npm audit에 기존 및 전이 의존성 경고가 남아 있다. 스토어 출시 전 런타임 사용 여부와 수정 버전을 검토해야 한다. 이번 APK는 로컬 테스트용이다.
- 크레딧은 기존 테스트 충전이며 실제 앱 결제/스토어 출시 서명은 포함하지 않는다.

새 PC에서는 위 SHA 지문을 재사용하지 말고 본인의 debug 서명 지문을 확인한다. 현재 코드로 APK를 새로 빌드해야 최신 UI 수정이 포함된다.


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
