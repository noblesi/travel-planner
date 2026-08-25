# WithTrip 배포·시연 체크리스트

- 기준일: `2026-08-18`
- 대상: Linux, Nginx 정적 Frontend, Spring Boot 실행 JAR, Oracle `WITHTRIP_DEV`

## EC2 HTTP 시연

현재 시연 서버처럼 HTTP만 사용하는 경우 [`deploy/README.md`](../../deploy/README.md)의 Docker Compose와 Nginx 설정을 적용합니다. 이 모드는 운영 HTTPS 배포와 명확히 분리합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1 `
  -DeploymentMode HttpDemo
```

- `FRONTEND_BASE_URL=http://EC2_PUBLIC_IP`
- `SESSION_COOKIE_SECURE=false`
- EC2 Security Group은 `22 → 관리자 IP`, `80 → 시연 접근 범위`만 허용
- Backend `8080`과 Oracle Port는 외부에 공개하지 않음
- Kakao Developers 허용 도메인에 `http://EC2_PUBLIC_IP` 등록

아래의 HTTPS 설정과 완료 조건은 운영 전환 시 적용합니다.

## 빌드 전 검증

실제 값을 출력하지 않고 필수 환경변수와 Release 산출물을 검증합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
```

Windows 실행 정책상 로컬 스크립트 실행이 제한된 환경에서도 위 명령으로 현재 프로세스에만 우회 정책을 적용할 수 있습니다.

검증 항목:

- Oracle Application 계정과 TourAPI·Kakao REST 키
- SMTP 계정·발신 주소와 실제 HTTPS `FRONTEND_BASE_URL`
- Frontend `/api` 경로와 Kakao JavaScript 키
- 배포 시 `SERVER_FORWARD_HEADERS_STRATEGY=framework`, `AUTH_ENFORCE_SECURITY=true`, `SESSION_COOKIE_SECURE=true`
- Backend 전체 Test와 `travel-planner.jar`
- Frontend Lint·전체 Test와 `frontend/dist`

## 권장 배치

```text
Browser
  └── https://service.example.com
      ├── /                 -> Nginx -> frontend/dist/index.html
      ├── /api/**           -> Nginx -> travel-planner.jar
      ├── /admin/**         -> Nginx -> travel-planner.jar (Thymeleaf)
      └── /assets/admin/**  -> Nginx -> travel-planner.jar (관리자 정적 자원)
                                      -> Oracle WITHTRIP_DEV
```

Frontend와 API를 같은 Origin으로 제공하면 별도 CORS 없이 Session Cookie와 CSRF를 사용할 수 있습니다.

## Nginx 핵심 설정 예시

인증서 경로와 서비스 도메인은 실제 환경에 맞게 지정합니다.

```nginx
server {
    listen 443 ssl http2;
    server_name service.example.com;

    root /opt/withtrip/frontend;
    index index.html;

    # 사용자 REST API와 Thymeleaf 관리자 화면은 URI를 유지해 Spring Boot로 전달합니다.
    location ~ ^/(api|admin)(/|$) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 관리자 CSS·JavaScript가 Vue 정적 파일 또는 SPA fallback으로 처리되지 않게 분리합니다.
    location ^~ /assets/admin/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 환경변수

Backend 필수값:

```dotenv
ORACLE_URL=jdbc:oracle:thin:@//db-host:1521/service
ORACLE_USERNAME=withtrip_dev
ORACLE_PASSWORD=secret
SERVER_FORWARD_HEADERS_STRATEGY=framework
AUTH_ENFORCE_SECURITY=true
SESSION_COOKIE_SECURE=true
TOUR_API_SERVICE_KEY=secret
KAKAO_REST_API_KEY=secret
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=secret
MAIL_PASSWORD=secret
MAIL_FROM=no-reply@service.example.com
MAIL_CONNECTION_TIMEOUT_MS=5000
MAIL_READ_TIMEOUT_MS=5000
MAIL_WRITE_TIMEOUT_MS=5000
FRONTEND_BASE_URL=https://service.example.com
```

Frontend Build 필수값:

```dotenv
VITE_API_BASE_URL=/api
VITE_KAKAO_MAP_KEY=javascript-key
```

`VITE_` 값은 Browser Bundle에 포함됩니다. 비밀값을 넣지 말고 Kakao JavaScript 키에는 허용 도메인을 반드시 설정합니다.

## 인증 운영 조건

- 현재 서버 session 저장소는 단일 인스턴스 기준입니다. Backend를 2대 이상 운영하기 전에는 Spring Session Redis 등 공유 저장소를 먼저 적용합니다.
- 회원·관리자 로그인 endpoint에는 Reverse Proxy 또는 API Gateway에서 IP 기준 rate limit과 반복 실패 모니터링을 적용합니다.
- HTTP 시연용 Nginx 설정은 로그인 POST 요청에 IP당 분당 5회와 순간 요청 3회의 제한을 적용합니다. 실제 NAT·사내망 사용자 패턴을 관찰한 뒤 조정합니다.
- 장소 검색 endpoint는 로그인 Session을 필수로 하고, Reverse Proxy 또는 API Gateway에서 별도의 IP 기준 rate limit과 TourAPI 할당량 경보를 적용합니다.
- 애플리케이션 내부의 단순 메모리 limiter는 다중 인스턴스에서 우회되고 재시작 시 상태가 사라지므로 현재 코드에 추가하지 않습니다.

## 로그 운영

- 운영 기본 로그 수준은 `INFO`로 유지하고 일시적인 장애 분석 외에는 `DEBUG`를 활성화하지 않습니다.
- 비밀번호, Session·CSRF·초대 Token, API Key, 전체 Query String과 요청·응답 본문이 Application·Nginx·Container 로그에 남지 않는지 확인합니다.
- Hibernate DB 연결 정보 로거는 `WARN`으로 유지해 접속 위치와 Schema 식별자가 시작 로그에 남지 않게 합니다.
- 이메일과 전화번호는 원문 대신 마스킹 값 또는 내부 식별자를 사용합니다.
- Docker `json-file`을 사용한다면 `max-size: 10m`, `max-file: 5` 이상의 Rotation 제한을 운영 `compose.yaml`에 설정하거나 동등한 중앙 Log 보존 정책을 적용합니다.
- 배포 전 Frontend Lint의 `no-console` 검증과 Backend Source의 `System.out`, `printStackTrace` 잔존 여부를 확인합니다.

## Kakao Map

- 개발 Origin은 정확히 `http://localhost:5173` 또는 실제 사용하는 Origin을 등록합니다.
- Vite는 5173이 사용 중일 때 다른 Port로 자동 이동하지 않고 실패하도록 구성되어 있습니다.
- 배포 전 실제 `https://service.example.com` Origin을 Kakao Developers JavaScript SDK 허용 도메인에 추가합니다.
- Path는 등록하지 않습니다.

## Oracle 데모 데이터

`docs/database/testdata/001_seed_demo_content.sql`은 `WITHTRIP_DEV`에서만 실행되며 영구 데모 회원 3명과 공개 플랜 6건을 멱등 유지합니다.

배포 전 확인:

1. `004_verify_travel_plan_schema.sql`로 `THUMBNAIL_IMG`, `PUBLISH_STATUS`, 제약·인덱스 누락이 없는지 확인
2. `008_backfill_plan_thumbnails.sql`을 실행한 뒤 `009_verify_plan_thumbnails.sql`의 두 Query가 0행인지 확인
3. `GET /api/health`가 Oracle 연결을 포함해 `UP`인지, `GET /api/health/live`가 Application Process 상태 `UP`인지 확인
4. `GET /api/plans?page=1&size=24`가 데모 플랜과 페이지 메타데이터를 반환하는지 확인
5. 공개 탐색 카드에서 관광 장소 이미지가 표시되고, 후보·정상 URL이 없으면 로컬 기본 이미지가 표시되는지 확인
6. `e2e.*@withtrip.test` 임시 회원이 0건인지 확인

환경파일의 비밀값을 명령행에 노출하지 않고 SQL을 실행하려면 다음 도구를 사용합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\invoke-oracle-sql.ps1 `
  -SqlFile .\docs\database\ddl\009_verify_plan_thumbnails.sql
```

## 배포 완료 조건

- HTTPS에서 로그인 후 Session Cookie에 `Secure`, `HttpOnly`, `SameSite=Lax`가 적용됨
- 로그인 후 새 CSRF 토큰으로 플랜 상태 변경이 성공함
- `/admin/login` 로그인 후 대시보드·회원·여행·공지·신고 화면이 정상 표시됨
- `/assets/admin/**` CSS·JavaScript가 `200`으로 제공되고 `/admin/**`가 Vue SPA fallback으로 처리되지 않음
- 공개 탐색·상세·Kakao Marker가 Desktop과 Mobile에서 표시됨
- 일정 추가 Request의 장소명·카테고리·이미지를 변조해도 서버 검색 결과 Snapshot과 대표 이미지가 유지됨
- Vue Router 직접 접근과 새로고침이 `404`가 아닌 `index.html`로 연결됨
- Oracle 접속정보와 API 키가 Git 및 정적 파일에 포함되지 않음
