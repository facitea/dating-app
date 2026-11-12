# 새 PC에서 이어서 작업하기

저장소는 어느 폴더에 clone해도 된다. 아래 경로는 저장소 루트 기준이며 AWS 서버 경로는 PC가 바뀌어도 유지된다.

## 1. 코드와 개발 도구

Git, Node.js 22 LTS(22.12 이상), JDK 17, MariaDB를 설치한다. Android 작업에는 Android Studio, SDK 36, JDK 21도 필요하다.

```powershell
git clone https://github.com/facitea/dating-app.git
cd dating-app
```

GitHub 인증은 새 PC에서 별도로 한다. Java/SDK는 GitHub에 포함되지 않는다. `java -version`, `node --version`으로 설치 상태를 확인한다.

## 2. 비밀 파일 복원

다음 파일은 Git에 없다. 기존 PC에서 암호화된 보관함 또는 암호화한 압축 파일로 옮긴다. 암호는 파일과 다른 경로로 전달한다. 평문 ZIP이나 공개 링크로 공유하지 않는다.

| 저장소 기준 위치 | 용도 / 없을 때 |
|---|---|
| frontend/.env.local | Firebase 웹 설정. Firebase 콘솔의 웹 앱 설정에서 다시 얻을 수 있다. |
| backend/.local/firebase-admin.json | 실제 로컬 Firebase 개인키 위치. 분실하면 Firebase 콘솔에서 새 키 발급 후 설정, 이전 키는 필요 시 폐기한다. |
| backend/.local/firebase.properties | 키 파일 위치와 프로젝트 설정. 아래 예시로 재작성 가능. |
| backend/.local/admin.properties | 로컬 JWT 및 관리자 설정. 새 로컬 DB에는 새 비밀값을 써도 된다. |
| backend/.local/push.properties | 선택: 웹 푸시 키와 로컬 테스트 충전 설정. |
| backend/.local/admin-first-login.txt | 기존 로컬 관리자 정보. 새 DB에는 적용되지 않는다. |
| .local/cloud/app.env | AWS 환경설정 사본. 로컬 DB용으로 가져다 쓰지 않는다. |
| .local/cloud/admin-first-login.txt | AWS 관리자 로그인 정보. |
| .local/cloud/initial-backup.tar.gz | 초기 시점 백업이며 최신 회원 데이터 백업이 아니다. 비밀값 포함. |
| 저장소 밖의 LightsailDefaultKey-ap-northeast-2.pem | SSH 개인키. 기존 PC에서는 Downloads에 보관했다. 새 PC에서는 안전한 임의 폴더에 둔다. |

기존 안내의 `.local/firebase-admin.json`과 혼동하지 말 것: 확인된 로컬 키는 **backend/.local/firebase-admin.json**이다. AWS 키는 서버의 `/etc/geureomuri/firebase-admin.json`이다.
기존 로컬 회원/사진이 필요하면 MariaDB 덤프와 backend/uploads도 별도 이전한다. clone만으로 회원 데이터가 복원되지는 않는다.
Android 출시 서명키는 아직 구성하지 않았다. 기존 debug APK 업데이트를 유지하려면 기존 PC의 사용자 홈 `.android/debug.keystore`도 별도로 보관한다. 새 debug 키를 쓰면 Firebase에 새 SHA 인증서 등록이 필요하고 기존 앱과 업데이트 서명이 달라진다.

## 3. 로컬 실행

1. 새 PC의 MariaDB에 로컬 전용 DB와 사용자를 만든다. AWS DB를 개발 DB로 사용하지 않는다.
2. frontend/.env.example을 frontend/.env.local로 복사하고 Firebase 웹 설정을 입력한다. VITE_API_BASE_URL은 빈 값으로 둔다(개발 프록시 사용).
3. 아래 예시를 backend/.local/admin.properties에 저장하고 CHANGE_ME 값들을 직접 바꾼다. properties 파일에서는 Windows 경로에 `/`를 사용한다.

```properties
spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/dating_local
spring.datasource.username=CHANGE_ME_LOCAL_DB_USER
spring.datasource.password=CHANGE_ME_LOCAL_DB_PASSWORD
app.jwt.secret=CHANGE_ME_RANDOM_SECRET_AT_LEAST_32_BYTES
app.admin.bootstrap-username=localadmin
app.admin.bootstrap-password=CHANGE_ME_RANDOM_PASSWORD_12_OR_MORE_CHARACTERS
app.credits.test-topup=true
```

backend/.local/firebase.properties:

```properties
app.firebase.project-id=dating-260d2
app.firebase.credentials-file=.local/firebase-admin.json
```

백엔드 터미널(JAVA_HOME은 이 PC의 JDK 17 설치 위치로 설정):

```powershell
cd backend
.\gradlew.bat bootRun --args="--server.port=8082"
```

프런트 터미널(저장소 루트에서):

```powershell
cd frontend
npm ci
npm run dev
```

http://localhost:5173 / 관리자 http://localhost:5173/admin 로 접속한다. 관리자 bootstrap은 관리자 테이블이 비어 있을 때만 생성된다.
환경변수 DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET 등이 남아 있으면 파일보다 우선될 수 있으니 확인한다. backend는 항상 backend 폴더에서 실행한다.
이메일 인증/비밀번호 재설정은 Firebase로 구현되어 있다. 이메일/비밀번호 공급자를 활성화하고 Firebase 웹·서버 설정을 복원한다. 로컬 Google 허용 도메인에 localhost가 있어야 한다.

## 4. AWS 접근 (PC 경로 독립)

SSH 키를 새 PC의 원하는 안전한 폴더에 옮기고 본인 계정만 읽도록 파일 권한을 설정한다.

```powershell
$sshKey = Read-Host '복원한 SSH 개인키 전체 경로'
ssh -i $sshKey ubuntu@3.34.208.199
```

호스트 키 확인 경고를 무조건 무시하지 말고, 변경이 의심되면 AWS Lightsail 브라우저 SSH에서 서버 지문을 확인한다.
키 파일이 없으면 AWS 프로젝트 페이지 → Ctrl Shift Believe → AWS Console → Lightsail → 서울 → geureomuri-server → 브라우저 SSH로 접근한다. 앱 관리자 계정과 AWS 계정은 서로 다르다.

## 5. 검증과 인수인계

- backend에서 `.\gradlew.bat test`, frontend에서 `npm run build`.
- Android는 docs/android-testing.md 참고. 새 PC에서 local.properties 및 JAVA_HOME을 다시 설정한다.
- GitHub push와 AWS 배포는 별개다. 서버 교체 전에 docs/lightsail-deployment.md를 읽는다.
- 다른 AI에는 AGENTS.md부터 읽고 현재 git status, 설정 파일 존재 여부, 서버 상태를 확인하도록 요청한다. 비밀 파일 내용을 채팅으로 전달하지 않는다.

암호화 백업은 이 작업에서 자동 생성하지 않았다. 암호를 정한 뒤 별도 생성하고, 새 PC에서 복호화·파일 복원을 확인해야 이전 완료다.
