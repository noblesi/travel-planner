# 인증 방식 결정

- 결정일: `2026-07-24`
- 상태: 구현 기준 확정
- 대상: Vue SPA, Spring Boot REST API

## 결정 사항

- 인증 상태는 Spring Security의 서버 세션으로 관리합니다.
- 로그인 수단은 로컬 계정과 Google OpenID Connect(OIDC)를 지원합니다.
- 두 로그인 수단은 별도 회원을 의미하지 않으며 하나의 `MEMBER.MEMBER_ID`에 연결할 수 있습니다.
- Backend 업무 기능은 로그인 수단을 직접 확인하지 않고 `CurrentMemberProvider`를 통해 현재 `MEMBER_ID`만 사용합니다.
- 운영 환경의 Session Cookie는 `HttpOnly`, `Secure`, `SameSite=Lax`로 설정하고 CSRF 보호를 유지합니다.
- Google Client Secret과 기타 인증 비밀값은 환경변수로 관리하며 Git에 Commit하지 않습니다.

## 회원과 로그인 수단

`MEMBER`는 서비스 회원의 기준 Entity입니다. 로그인 수단은 회원과 분리해 관리합니다.

```text
MEMBER
├── LOCAL_CREDENTIAL       0..1
└── SOCIAL_IDENTITY       0..N
```

논리적인 책임은 다음과 같습니다. 실제 Table과 Column 이름은 인증 담당 구현에서 확정하되 이 관계와 식별 규칙을 유지합니다.

| 영역 | 책임 |
| --- | --- |
| `MEMBER` | 서비스 내부 회원, `MEMBER_ID` 발급 |
| `LOCAL_CREDENTIAL` | 로그인 ID와 단방향 Password Hash 저장 |
| `SOCIAL_IDENTITY` | Provider와 Provider 고유 사용자 ID 저장 |

Google 계정의 고유 식별자는 Email이 아니라 OIDC ID Token의 `sub` Claim을 사용합니다. Provider 식별값은 `GOOGLE`로 통일합니다.

## 가입과 계정 연결

### Google 가입 후 같은 Email로 로컬 가입

새 `MEMBER`를 만들지 않습니다. Google로 다시 인증한 후 기존 회원에 로컬 Credential을 추가합니다.

### 로컬 가입 후 같은 Email로 Google 로그인

Email이 같다는 이유만으로 자동 연결하지 않습니다. 기존 로컬 계정으로 재인증한 후 Google Identity를 연결합니다.

### 서로 다른 Email

기본적으로 서로 다른 회원으로 처리합니다. 이름, 전화번호, 주소 등 Profile 정보가 같아도 자동으로 병합하지 않습니다. 계정 연결이나 병합을 제공할 경우 사용자가 양쪽 계정의 소유권을 모두 증명해야 합니다.

### 중복 판단에 사용하지 않는 정보

- 이름
- 전화번호
- 주소
- Profile Image

위 정보는 변경되거나 다른 사람과 중복될 수 있으므로 회원 식별자 또는 자동 병합 기준으로 사용하지 않습니다.

## Backend 연동 계약

여행 플랜을 포함한 업무 Service는 Spring Session, JWT, Google Claim을 직접 참조하지 않습니다.

```text
TravelPlanService
└── CurrentMemberProvider
    └── 현재 인증된 MEMBER_ID 반환
```

- 현재 회원을 확인할 수 없으면 `401 CURRENT_MEMBER_NOT_AVAILABLE`을 반환합니다.
- 회원 ID를 Request Body, Query Parameter 또는 임시 상수로 전달하지 않습니다.
- 인증 구현이 완료되기 전 Mock Provider가 필요하면 개발 Profile에서만 활성화합니다.
- 운영 Profile에는 기본 회원 ID나 Fallback 값을 두지 않습니다.

## 로컬 Password 원칙

- Password 원문과 복호화 가능한 값은 저장하지 않습니다.
- Spring Security `PasswordEncoder`의 적응형 단방향 Hash를 사용합니다.
- Google 로그인만 사용하는 회원에게 임의의 로컬 Password를 생성하지 않습니다.
- 로컬 로그인을 추가할 때는 기존 계정 재인증 후 사용자가 직접 Password를 설정합니다.

## Google 로그인 범위

- Spring Security OAuth2 Client의 Authorization Code 방식과 OIDC를 사용합니다.
- 최초 Scope는 `openid`, `profile`, `email`로 제한합니다.
- 로그인만 제공하는 단계에서는 Google Access Token과 Refresh Token을 Application DB에 저장하지 않습니다.
- 추후 Google API 접근이 필요해지면 목적, Scope, Token 암호화 및 폐기 정책을 별도로 결정합니다.

## 후속 작업

1. 인증 담당자가 `MEMBER`, 로컬 Credential, Social Identity의 실제 Schema를 확정합니다.
2. Spring Security 로컬 로그인과 `oauth2Login()`을 같은 `MemberPrincipal`로 통합합니다.
3. `CurrentMemberProvider`의 Spring Security 구현을 제공합니다.
4. 회원 Table 생성 후 `docs/database/ddl/003_add_identity_foreign_keys.sql`을 적용합니다.
