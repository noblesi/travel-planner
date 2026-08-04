# WithTrip 배포·시연 체크리스트

- 기준일: `2026-08-03`
- 대상: Linux, Nginx 정적 Frontend, Spring Boot 실행 JAR, Oracle `WITHTRIP_DEV`

## 빌드 전 검증

실제 값을 출력하지 않고 필수 환경변수와 Release 산출물을 검증합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-release.ps1
```

Windows 실행 정책상 로컬 스크립트 실행이 제한된 환경에서도 위 명령으로 현재 프로세스에만 우회 정책을 적용할 수 있습니다.

검증 항목:

- Oracle Application 계정과 TourAPI·Kakao REST 키
- Frontend `/api` 경로와 Kakao JavaScript 키
- 배포 시 `AUTH_ENFORCE_SECURITY=true`, `SESSION_COOKIE_SECURE=true`
- Backend 전체 Test와 `travel-planner.jar`
- Frontend 전체 Test와 `frontend/dist`

## 권장 배치

```text
Browser
  └── https://service.example.com
      ├── /          -> Nginx -> frontend/dist/index.html
      └── /api/**    -> Nginx -> http://127.0.0.1:8080/api/**
                              -> travel-planner.jar
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

    location /api/ {
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
```

Frontend Build 필수값:

```dotenv
VITE_API_BASE_URL=/api
VITE_KAKAO_MAP_KEY=javascript-key
```

`VITE_` 값은 Browser Bundle에 포함됩니다. 비밀값을 넣지 말고 Kakao JavaScript 키에는 허용 도메인을 반드시 설정합니다.

## Kakao Map

- 개발 Origin은 정확히 `http://localhost:5173` 또는 실제 사용하는 Origin을 등록합니다.
- Vite는 5173이 사용 중일 때 다른 Port로 자동 이동하지 않고 실패하도록 구성되어 있습니다.
- 배포 전 실제 `https://service.example.com` Origin을 Kakao Developers JavaScript SDK 허용 도메인에 추가합니다.
- Path는 등록하지 않습니다.

## Oracle 데모 데이터

`docs/database/testdata/001_seed_demo_content.sql`은 `WITHTRIP_DEV`에서만 실행되며 영구 데모 회원 3명과 공개 플랜 6건을 멱등 유지합니다.

배포 전 확인:

1. `GET /api/health`가 `UP`인지 확인
2. `GET /api/plans?page=1&size=24`가 데모 플랜과 페이지 메타데이터를 반환하는지 확인
3. 공개 상세에서 날짜·장소·조회수가 표시되는지 확인
4. `e2e.*@withtrip.test` 임시 회원이 0건인지 확인

## 배포 완료 조건

- HTTPS에서 로그인 후 Session Cookie에 `Secure`, `HttpOnly`, `SameSite=Lax`가 적용됨
- 로그인 후 새 CSRF 토큰으로 플랜 상태 변경이 성공함
- 공개 탐색·상세·Kakao Marker가 Desktop과 Mobile에서 표시됨
- Vue Router 직접 접근과 새로고침이 `404`가 아닌 `index.html`로 연결됨
- Oracle 접속정보와 API 키가 Git 및 정적 파일에 포함되지 않음
