# 회원 인증 방식 결정

- 결정일: `2026-08-03`
- 상태: Oracle `MEMBER` 기반 이메일 로그인 구현 완료, Google OIDC 연동 대기
- 기준 스키마: `WITHTRIP_DEV`

## 결정 요약

- 서비스 회원의 기준 식별자는 `MEMBER.MEMBER_ID`입니다.
- 이메일 로그인은 기존 `MEMBER.EMAIL`과 `MEMBER.PASSWORD_HASH`를 사용합니다.
- Google 로그인은 기존 `GOOGLE_ACCOUNT_LINK.GOOGLE_SUBJECT`를 사용합니다.
- 별도 `LOCAL_CREDENTIAL`, `SOCIAL_IDENTITY` 테이블은 추가하지 않습니다.
- 업무 서비스는 로그인 방식을 직접 확인하지 않고 `CurrentMemberProvider`로 현재 회원 ID만 조회합니다.
- 인증 성공 후에는 Spring Security 서버 세션을 사용하며 상태 변경 요청은 CSRF 토큰을 요구합니다.

## 물리 구조

```text
MEMBER
├── MEMBER_ID              PK
├── EMAIL                  UK, 이메일 로그인 ID
├── PASSWORD_HASH          nullable, BCrypt
├── NICKNAME
├── MEMBER_STATUS          ACTIVE | WITHDRAWN
└── ...

GOOGLE_ACCOUNT_LINK
├── MEMBER_ID              FK -> MEMBER.MEMBER_ID
├── GOOGLE_SUBJECT         Google OIDC sub
└── ...
```

`PASSWORD_HASH IS NULL`인 회원은 이메일 로그인을 사용할 수 없습니다. 데모 전용 회원은 이 특성을 이용해 화면 데이터의 작성자로만 사용합니다.

## 이메일 로그인 규칙

1. 이메일 앞뒤 공백을 제거하고 소문자로 정규화합니다.
2. `LOWER(MEMBER.EMAIL)`로 회원을 조회합니다.
3. `MEMBER_STATUS = 'ACTIVE'`이고 `PASSWORD_HASH IS NOT NULL`인 회원만 인증 후보입니다.
4. `BCryptPasswordEncoder.matches()`로 비밀번호를 비교합니다.
5. 실패 원인과 관계없이 `401 INVALID_LOGIN_CREDENTIALS`를 반환해 회원 존재 여부를 노출하지 않습니다.
6. 성공 시 `MemberPrincipal`을 만들고 세션 ID와 CSRF 토큰을 회전합니다.

비밀번호 원문은 DB, 로그, 응답에 저장하지 않습니다. 신규 비밀번호 저장 시에도 BCrypt만 사용합니다.

## Google 로그인 규칙

Google OIDC 연동 시 이메일이 아닌 ID Token의 `sub`를 `GOOGLE_ACCOUNT_LINK.GOOGLE_SUBJECT`와 비교합니다.

- 이미 연결된 `sub`이면 해당 `MEMBER_ID`로 `MemberPrincipal`을 생성합니다.
- 이메일이 같다는 이유만으로 기존 회원과 자동 연결하지 않습니다.
- 계정 연결은 기존 계정 재인증을 거친 명시적 흐름에서 수행합니다.
- 로그인만 제공하는 단계에서는 Google Access Token과 Refresh Token을 애플리케이션 DB에 저장하지 않습니다.
- Google 전용 회원은 `PASSWORD_HASH = NULL`을 유지할 수 있습니다.

Google Client ID·Secret과 Redirect URI는 환경변수로 관리하고 Git에 커밋하지 않습니다.

## 세션과 CSRF

- 인증 상태는 Spring Security 서버 세션으로 유지합니다.
- 브라우저 JavaScript에서 세션 쿠키를 읽지 못하도록 `HttpOnly`를 사용합니다.
- 배포 HTTPS 환경에서는 `SESSION_COOKIE_SECURE=true`를 사용합니다.
- 로그인·로그아웃 후에는 `/api/auth/csrf`를 다시 호출해 새 토큰을 받아야 합니다.
- Frontend는 `credentials: include` 또는 Axios `withCredentials: true`를 사용합니다.

## 개발·테스트 데이터

- 영구 데모 회원: `demo.*@withtrip.example`, `PASSWORD_HASH = NULL`, 로그인 불가
- 임시 E2E 회원: `e2e.*@withtrip.test`, P2/P3 검증 후 정리
- 시드와 정리 절차: `docs/database/testdata/README.md`

## 남은 작업

1. Google OAuth Client 설정과 `oauth2Login()` 연결
2. `GOOGLE_ACCOUNT_LINK.GOOGLE_SUBJECT` 조회 Mapper 및 계정 연결 정책 구현
3. 이메일 회원가입·비밀번호 재설정 시 BCrypt 저장 흐름 구현
4. 배포 환경 HTTPS·Secure Cookie·세션 저장소 설정 검증
