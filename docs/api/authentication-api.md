# 인증 API 계약

Vue SPA와 Spring Boot API 사이의 서버 세션 인증 계약입니다. 현재 Oracle `MEMBER.EMAIL + MEMBER.PASSWORD_HASH` 기반 이메일 로그인이 구현되어 있습니다.

## 공통 규칙

- 모든 응답은 공통 `ApiResponse` 또는 `ErrorResponse` 형식을 사용합니다.
- 세션 Cookie는 `HttpOnly`이며 Frontend는 Axios `withCredentials: true`를 사용합니다.
- 상태 변경 요청은 `/api/auth/csrf`에서 받은 Header 이름과 토큰을 전송합니다.
- 로그인·로그아웃으로 인증 상태가 바뀌면 CSRF 토큰을 다시 조회합니다.
- 로그인 후 이동 경로는 `/`로 시작하고 `//`로 시작하지 않는 내부 경로만 허용합니다.

## CSRF 토큰

```http
GET /api/auth/csrf
```

```json
{
  "success": true,
  "data": {
    "headerName": "X-CSRF-TOKEN",
    "parameterName": "_csrf",
    "token": "server-generated-token"
  }
}
```

## 세션 조회

```http
GET /api/auth/session
```

로그인 상태:

```json
{
  "success": true,
  "data": {
    "authenticated": true,
    "member": {
      "memberId": "25",
      "email": "member@example.com",
      "displayName": "여행자"
    }
  }
}
```

비로그인 상태는 `authenticated: false`이며 `member`를 포함하지 않습니다.

## 이메일 로그인

```http
POST /api/auth/login
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "email": "member@example.com",
  "password": "raw-password"
}
```

### 처리 규칙

- 이메일은 앞뒤 공백 제거 후 소문자로 정규화합니다.
- `MEMBER_STATUS = 'ACTIVE'`이고 `PASSWORD_HASH`가 있는 회원만 로그인할 수 있습니다.
- 비밀번호는 `BCryptPasswordEncoder`로 검증합니다.
- 성공 시 세션 ID를 변경하고 기존 CSRF 토큰을 폐기합니다.
- 환경변수 기반 개발용 Credential은 사용하지 않습니다.

성공 응답의 `data` 구조는 세션 조회의 로그인 상태와 같습니다.

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 이메일 또는 비밀번호 누락·형식 오류 |
| `401` | `INVALID_LOGIN_CREDENTIALS` | 회원 없음, 비밀번호 불일치, 탈퇴 회원, 로컬 비밀번호 없음 |
| `403` | `ACCESS_DENIED` | CSRF 토큰 누락·불일치 |

## 로그아웃

```http
POST /api/auth/logout
X-CSRF-TOKEN: server-generated-token
```

인증 정보를 제거하고 세션을 무효화합니다. 완료 후 Frontend는 새 CSRF 토큰을 조회해야 합니다.

## 보호 API

- 공개 조회: `GET /api/regions`, `GET /api/plans`, `GET /api/plans/{planId}`, 초대 링크 조회
- 인증 필요: `GET /api/places/search`, 플랜 생성·편집·초대 수락을 포함한 나머지 상태 변경과 편집 조회
- 인증이 없으면 `401 CURRENT_MEMBER_NOT_AVAILABLE`을 반환합니다.
- 인증은 있지만 권한 또는 CSRF 검증에 실패하면 `403 ACCESS_DENIED`를 반환합니다.
