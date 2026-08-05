# Oracle 데모·E2E 데이터

이 디렉터리의 SQL은 `WITHTRIP_DEV` 개발·시연 스키마 전용입니다. 운영 스키마나 `SYSTEM`, `SCOTT`, `TRAVEL_PLANNER` 계정에서는 실행하지 않습니다.

## 실행 순서

1. `001_seed_demo_content.sql`: 화면 시연용 영구 데이터 입력
2. `002_seed_e2e_members.sql`: P2·P3 검증용 임시 회원 입력
3. 애플리케이션을 통한 로그인·플랜·초대 흐름 검증
4. `003_cleanup_e2e_data.sql`: E2E 회원과 해당 회원이 만든 데이터만 정리

기존 `seed_data.sql`의 예약 ID(`900xxx`) 데이터가 이미 입력된 개발 DB는
`004_repair_seed_data.sql`을 한 번 실행해 초대 Token, 수락 멤버십, 신고 처리 상태,
동기화 집계와 편집 작업 이력을 보정합니다. 이 Script는 기존 Seed 행의 필수 개수를
먼저 검증하며, 검증 실패 시 아무 변경도 Commit하지 않습니다.

새 개발 DB 또는 예약 ID 테스트 데이터를 완전히 다시 구성할 때는
`005_seed_full_test_data.sql`만 실행합니다. 이 파일은 기존 외부 `seed_data.sql`과
사후 복구 Script의 최종 상태를 하나로 통합한 기준 Seed이며, 반복 실행할 수 있습니다.

일반 데모·E2E 데이터는 아래 순서로 사용합니다.

```powershell
$env:NLS_LANG = 'KOREAN_KOREA.AL32UTF8'
sqlplus withtrip_dev@//<host>:1521/orcl `@docs/database/testdata/001_seed_demo_content.sql
sqlplus withtrip_dev@//<host>:1521/orcl `@docs/database/testdata/002_seed_e2e_members.sql
sqlplus withtrip_dev@//<host>:1521/orcl `@docs/database/testdata/003_cleanup_e2e_data.sql
```

예약 ID(`900xxx`) 테스트 데이터는 현재 DB 상태에 따라 둘 중 하나만 실행합니다.

```powershell
# 기존 seed_data.sql 데이터를 유지하면서 잘못된 관계와 Hash만 보정
sqlplus withtrip_dev@//<host>:1521/orcl `@docs/database/testdata/004_repair_seed_data.sql

# 예약 ID 테스트 데이터를 삭제한 뒤 최종 상태로 완전히 재생성
sqlplus withtrip_dev@//<host>:1521/orcl `@docs/database/testdata/005_seed_full_test_data.sql
```

Backend JAR를 만든 뒤 `scripts/verify-oracle-auth-flow.ps1`을 실행하면 보안 강제 모드에서 소유자 로그인, 플랜 생성·수정, 초대, 초대자 수락·공동 편집, 로그아웃을 순서대로 검증할 수 있습니다. 실행 전 `002` 시드를 적용하고 성공·실패 여부와 관계없이 마지막에 `003` 정리를 실행합니다.

비밀번호와 Oracle 접속 정보는 명령행이나 SQL 파일에 넣지 않고 SQL*Plus Prompt와 `.env.local`을 사용합니다.

## 데이터 범위

- 데모 회원은 `demo.*@withtrip.example`을 사용하며 `PASSWORD_HASH`가 `NULL`이라 로그인할 수 없습니다.
- 데모 플랜은 `PUBLIC`, `ACTIVE`로 유지하고 Cleanup 대상에서 제외합니다.
- E2E 회원은 `e2e.*@withtrip.test`만 사용합니다.
- E2E 기본 비밀번호는 테스트 전용 `WithTrip-E2E-2026!`이며 공개·운영 환경에서 사용하지 않습니다.
- Cleanup은 위 E2E Email을 기준으로 관련 데이터를 FK 역순으로 삭제합니다.

모든 Seed는 반복 실행해도 동일한 전용 Email·플랜·장소를 중복 생성하지 않도록 작성합니다.
