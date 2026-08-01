# 인증 API

Vue SPA와 Spring Boot API 사이의 서버 session 인증 계약입니다. 현재 구현은 session·CSRF·`MemberPrincipal` 기반과 `local` Profile 로그인까지 포함합니다. Oracle 회원 Credential과 Google OIDC는 아직 연결하지 않았습니다.

## 공통 규칙

- 인증 상태는 서버의 `HttpSession`에 저장합니다.
- Session Cookie는 `HttpOnly`, `SameSite=Lax`를 사용하고 기본 Profile에서는 `Secure`가 기본값입니다.
- 상태 변경 요청은 CSRF token이 필요합니다.
- Frontend는 `withCredentials: true`로 요청합니다.
- 회원 식별자는 JSON number가 아닌 문자열로 반환합니다.
- 로그인 후 URL 이동은 `/`로 시작하고 `//`로 시작하지 않는 내부 경로만 허용합니다.

## CSRF token 조회

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

Frontend는 `POST`, `PATCH`, `PUT`, `DELETE` 요청 전에 이 Endpoint에서 받은 `headerName`과 `token`을 Header로 전송합니다. 로그인과 로그아웃으로 인증 상태가 바뀐 뒤에는 새 token을 다시 조회합니다.

## 현재 session 조회

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
      "memberId": "7",
      "email": "member@example.com",
      "displayName": "여행자"
    }
  }
}
```

비로그인 상태:

```json
{
  "success": true,
  "data": {
    "authenticated": false,
    "member": null
  }
}
```

## local 로그인

```http
POST /api/auth/login
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "email": "member@example.com",
  "password": "local-secret"
}
```

- 이 Endpoint는 `local` Profile에서만 등록됩니다.
- Credential은 `LOCAL_AUTH_EMAIL`, `LOCAL_AUTH_PASSWORD` 환경변수로 설정합니다.
- Password는 Application 시작 시 BCrypt Hash로 변환한 뒤 비교합니다.
- 로그인 성공 시 session ID를 변경하고 기존 CSRF token을 폐기합니다.
- 잘못된 Credential은 `401 INVALID_LOGIN_CREDENTIALS`입니다.
- local Credential이 설정되지 않은 상태의 로그인도 같은 오류를 반환해 설정 여부를 노출하지 않습니다.

## 로그아웃

```http
POST /api/auth/logout
X-CSRF-TOKEN: server-generated-token
```

로그아웃은 session을 무효화하고 Security Context를 제거합니다.

## 보안 적용 모드

- 기본 Profile: `AUTH_ENFORCE_SECURITY=true`가 기본이며 공개 Endpoint 이외의 요청은 인증을 요구합니다.
- `local` Profile: 기존 플랜 통합 개발 호환성을 위해 기본값은 `false`입니다. 실제 session 기반 확인 시 `AUTH_ENFORCE_SECURITY=true`로 실행합니다.
- 공개 Endpoint: health, 지역 조회, 장소 검색, 초대 token 조회, 인증 API입니다.
- 보호 Endpoint: 플랜 생성·편집·일정 변경·초대 생성 및 수락 등 상태 변경 기능입니다.

## 남은 연동

1. 실제 Oracle `MEMBER`, `LOCAL_CREDENTIAL`, `SOCIAL_IDENTITY` Schema 확정
2. DB 기반 local Credential 조회와 회원가입 연결
3. Google OAuth2 Client 설정과 OIDC `sub` 기반 Identity 조회
4. 두 로그인 수단에서 동일한 `MemberPrincipal` 생성
5. 실제 Oracle 회원으로 플랜 생성·초대 수락 E2E 검증
