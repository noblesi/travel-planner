# 외부 API 설정

- 기준일: `2026-07-30`
- 대상: TourAPI 국문 관광정보 서비스, Kakao Map JavaScript SDK, Kakao REST API

실제 인증키는 Git 추적 파일에 기록하지 않습니다. 저장소의 `.env.example` 파일은 변수 이름과 기본 URL만 제공하며 `change-me`를 실제 키로 교체해 커밋하지 않습니다.

## 환경변수

### Backend

| 환경변수 | 기본값 | 용도 |
| --- | --- | --- |
| `TOUR_API_BASE_URL` | `https://apis.data.go.kr/B551011/KorService2` | TourAPI 국문 관광정보 서비스 Base URL |
| `TOUR_API_SERVICE_KEY` | 없음 | 공공데이터포털 TourAPI 인증키 |
| `TOUR_API_MOBILE_APP` | `WithTrip` | TourAPI 호출 서비스명 |
| `TOUR_API_CONNECT_TIMEOUT` | `3s` | TourAPI 연결 제한시간 |
| `TOUR_API_READ_TIMEOUT` | `5s` | TourAPI 응답 제한시간 |
| `KAKAO_REST_BASE_URL` | `https://dapi.kakao.com` | Kakao REST API Base URL |
| `KAKAO_REST_API_KEY` | 없음 | 서버 전용 Kakao REST API 키 |
| `KAKAO_REST_CONNECT_TIMEOUT` | `3s` | Kakao REST 연결 제한시간 |
| `KAKAO_REST_READ_TIMEOUT` | `5s` | Kakao REST 응답 제한시간 |

Spring Boot는 저장소 루트의 `.env`를 자동으로 읽지 않습니다. 실행 Terminal 또는 IDE Run Configuration에 환경변수를 설정합니다.

Windows PowerShell 예시:

```powershell
$env:TOUR_API_SERVICE_KEY = "발급받은-서비스-키"
$env:KAKAO_REST_API_KEY = "발급받은-REST-API-키"
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

### Frontend

`frontend/.env.example`을 참고해 Git에서 제외되는 `frontend/.env.local`을 만듭니다.

```dotenv
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
VITE_KAKAO_MAP_KEY=발급받은-JavaScript-키
```

`VITE_` 변수는 Browser Bundle에서 확인할 수 있으므로 비밀값 저장 용도로 사용하지 않습니다. Kakao JavaScript 키는 도메인 제한을 함께 적용하고 REST API 키는 Frontend에 넣지 않습니다.

## Kakao Developers 설정

1. 앱의 Kakao Map API 사용 상태를 활성화합니다.
2. JavaScript 키의 JavaScript SDK 도메인에 `http://localhost:5173`을 등록합니다.
3. 배포 환경이 준비되면 실제 `https://` 서비스 도메인을 추가합니다.
4. REST API 키는 Backend에서만 사용하며 필요한 경우 허용 IP를 제한합니다.

도메인에는 Path를 넣지 않습니다. 개발 Port가 달라지면 실제 Vite 접속 Origin을 추가로 등록합니다.

## 코드 바인딩

Backend 환경변수는 `ExternalApiProperties`의 `app.external-api.tour`, `app.external-api.kakao` 속성으로 바인딩됩니다. 키가 없더라도 일반 플랜 기능과 자동 테스트는 실행할 수 있으며, TourAPI 장소 검색 호출 시 `TOUR_API_NOT_CONFIGURED` 오류를 반환합니다.

## 참고

- [공공데이터포털 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do)
- [Kakao Map API 개요](https://developers.kakao.com/docs/en/kakaomap/common)
- [Kakao 앱 키 및 JavaScript SDK 도메인 설정](https://developers.kakao.com/docs/en/app-setting/app)
