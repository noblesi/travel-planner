# EC2 HTTP 시연 배포

이 디렉터리는 AWS EC2 Ubuntu 24.04에서 Nginx는 Host에 설치하고 Spring Boot만 Docker Compose로 실행하는 HTTP 시연 구성을 제공합니다. 운영 HTTPS 구성으로 전환할 때는 인증서와 `SESSION_COOKIE_SECURE=true`를 적용해야 합니다.

## 1. 로컬 산출물 생성

저장소 루트의 `.env.local`과 `frontend/.env.local`에 실제 값을 설정한 뒤 HTTP 데모 모드로 검증합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 `
  -DeploymentMode HttpDemo
```

성공하면 다음 산출물이 생성됩니다.

```text
backend/build/libs/travel-planner.jar
frontend/dist/
```

## 2. EC2 배치 구조

WinSCP 또는 PSCP로 다음 구조를 맞춥니다. `frontend/dist`의 내용은 `frontend` 바로 아래에 배치합니다.

```text
/opt/withtrip/
├── .dockerignore
├── .env
├── backend/
│   └── build/libs/travel-planner.jar
├── deploy/
│   ├── Dockerfile
│   ├── compose.yaml
│   └── nginx/withtrip-http.conf
└── frontend/
    ├── index.html
    └── assets/
```

`deploy/.env.example`을 참고해 EC2에서만 `/opt/withtrip/.env`를 작성하고 권한을 제한합니다.

```bash
cd /opt/withtrip
cp deploy/.env.example .env
chmod 600 .env
nano .env
```

HTTP 시연에서는 다음 값이 필요합니다.

```dotenv
AUTH_ENFORCE_SECURITY=true
SESSION_COOKIE_SECURE=false
SERVER_FORWARD_HEADERS_STRATEGY=framework
FRONTEND_BASE_URL=http://EC2_PUBLIC_IP
```

## 3. Backend 실행

```bash
cd /opt/withtrip
docker compose -f deploy/compose.yaml config --quiet
docker compose -f deploy/compose.yaml up -d --build backend
docker compose -f deploy/compose.yaml ps
docker compose -f deploy/compose.yaml logs --tail=100 backend
curl --fail http://127.0.0.1:8080/api/health
curl --fail http://127.0.0.1:8080/api/health/live
```

`/api/health`는 Application과 Oracle 연결을 함께 확인하는 readiness endpoint이고, `/api/health/live`는 Application Process만 확인하는 liveness endpoint입니다. Backend Port는 `127.0.0.1:8080`에만 열리므로 EC2 Security Group에서 8080을 공개하지 않습니다.

프로필 이미지는 Compose named volume `withtrip_profile-uploads`에 저장됩니다. 컨테이너를 재생성해도 유지되며, 서버를 완전히 이전할 때는 이 volume도 함께 백업해야 합니다.

## 4. Nginx 적용

```bash
sudo cp /opt/withtrip/deploy/nginx/withtrip-http.conf /etc/nginx/sites-available/withtrip
sudo ln -sfn /etc/nginx/sites-available/withtrip /etc/nginx/sites-enabled/withtrip
sudo nginx -t
sudo systemctl reload nginx
```

기본 Nginx Site가 같은 `default_server` Port를 사용한다면 `/etc/nginx/sites-enabled/default`를 비활성화한 뒤 `nginx -t`를 다시 실행해야 합니다.

제공 설정은 로그인 요청(`/api/auth/login`, `/admin/login`)과 계정 복구 요청(`/api/account-recovery/**`)을 IP당 분당 5회로 제한하고 짧은 순간의 요청 3회까지 허용합니다. 로그인 화면을 여는 GET 요청은 제한하지 않습니다. `/uploads/profile/**`는 Backend의 영속 volume에서 제공됩니다. `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`, `Permissions-Policy`도 정적 파일과 Proxy 응답에 적용됩니다.

## 5. 시연 검증

EC2 Security Group의 HTTP 80 Inbound가 열려 있는지 확인한 뒤 다음을 검증합니다.

```bash
curl --fail http://EC2_PUBLIC_IP/api/health
curl --fail http://EC2_PUBLIC_IP/api/health/live
```

- Public IP의 `/`에서 Vue 화면 표시
- 로그인 후 새로고침해도 Session 유지
- 플랜 생성·편집·발행·검색·복사·신고
- `/admin/login`과 `/assets/admin/**` 응답
- Vue Route 직접 접근 및 새로고침
- Kakao Developers에 `http://EC2_PUBLIC_IP` 허용 도메인 등록 후 지도 표시
- 초대 메일 링크가 `http://EC2_PUBLIC_IP`를 가리키는지 확인

## 6. 재배포와 되돌리기

새 산출물을 덮어쓰기 전에 기존 JAR와 Frontend 디렉터리를 Release 단위로 백업합니다. 업로드 후 Backend를 다시 빌드하고 Nginx를 Reload합니다.

```bash
docker compose -f /opt/withtrip/deploy/compose.yaml up -d --build backend
sudo nginx -t
sudo systemctl reload nginx
```

문제가 발생하면 이전 JAR와 Frontend 파일을 복원한 뒤 같은 명령을 다시 실행합니다. `docker compose ps`, Backend Log와 `/api/health`가 정상일 때만 배포 완료로 판단합니다.
