# 여행 플랜 Oracle DDL

`travelplanner_v2.exerd`와 KMS 인수인계 문서에서 여행 플랜 핵심 영역을 분리한 Oracle 19c+ 기준 Schema입니다.

## 파일과 실행 순서

1. `001_create_travel_plan_schema.sql`: 핵심 8개 Table, 4개 Sequence, Constraint, Index 생성
2. `002_seed_region_master.sql`: TourAPI 시·도 17개 초기 데이터 입력
3. `004_verify_travel_plan_schema.sql`: 생성 결과를 읽기 전용 Query로 검증
4. `003_add_identity_foreign_keys.sql`: 인증 Table 확정 후에만 선택 실행

빈 애플리케이션 Schema에서 다음과 같이 실행합니다. 비밀번호는 명령행에 넣지 않고 Prompt에서 입력합니다.

```powershell
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/001_create_travel_plan_schema.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/002_seed_region_master.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/004_verify_travel_plan_schema.sql
```

`001`은 일회성 생성 Script입니다. 일부 Object가 이미 있는 Schema에서 재실행하면 실패합니다. 자동 Drop은 데이터 손실 위험이 있어 제공하지 않습니다.

## Oracle 접속 전 로컬 개발

실제 Oracle 접속 정보가 준비되기 전에는 Spring의 `local` Profile을 사용합니다.

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

- `backend/src/main/resources/application-local.yml`에서만 H2 Memory Database를 활성화합니다.
- 로컬 초기화 Script는 `backend/src/main/resources/db/local/`에 둡니다.
- 현재 로컬 Schema는 `GET /api/regions` 개발 범위만 제공합니다.
- Oracle DDL이 최종 기준이며 로컬 Script의 변경은 관련 Oracle DDL과 의미가 일치해야 합니다.
- 실제 Oracle 접속 정보가 준비되면 이 문서의 `001`, `002`, `004` 순서로 반드시 다시 검증합니다.

## 지역코드 규칙

- `SIDO`: TourAPI `areaCode2` 응답의 `code`를 `REGION_CODE`에 그대로 저장합니다.
- `SIGUNGU`: `{areaCode}-{sigunguCode}` 형식으로 저장합니다. 예: 서울 종로구가 `areaCode=1`, `sigunguCode=1`이면 `1-1`입니다.
- `PARENT_REGION_CODE`: 시·군·구가 속한 시·도의 TourAPI `areaCode`입니다.
- 지역명과 활성 여부는 TourAPI 동기화 시 `MERGE`로 갱신합니다.

TourAPI의 현재 국문 관광정보 Service Base URL은 `apis.data.go.kr/B551011/KorService2`이고 지역코드 기능은 `GET /areaCode2`입니다. 초기 데이터는 개발 시작용이며 운영에서는 해당 API 응답으로 정기 검증합니다.

## ERD 반영 및 보정 사항

- ERD의 핵심 8개 Table과 Column Type, 주요 Constraint를 유지했습니다.
- ERD에는 `CK_PLAN_PARTICIPANT_TYPE`이 있지만 대상 Column이 없어서 `PLAN_MEMBER.PARTICIPANT_TYPE`을 추가했습니다.
- 인증 방식은 확정됐지만 `MEMBER`와 `ADMIN`의 실제 Schema가 아직 확정되지 않아 관련 외래키는 `003`으로 분리했습니다. 회원 Table 생성 전에는 실행하지 않습니다.
- `PLAN_EDIT_OPERATION.OPERATION_ID`는 중복 자동 저장 요청을 식별하는 UUID 문자열이므로 별도 Sequence를 만들지 않습니다.
- `PLACE_MASTER`의 PK는 외부 제공자와 외부 장소 ID의 복합키이며, 일정에는 장소 Snapshot을 보관합니다.
- 자동 저장 충돌 검사를 위해 `TRAVEL_PLAN.VERSION_NO`, `PLAN_DAY.SCHEDULE_VERSION`, `PLAN_SCHEDULE_ITEM.ITEM_VERSION`은 0 이상으로 제한했습니다.

## 애플리케이션 Insert 규칙

MyBatis에서는 다음 Sequence를 직접 사용합니다.

```sql
SELECT SEQ_TRAVEL_PLAN.NEXTVAL FROM DUAL;
SELECT SEQ_PLAN_DAY.NEXTVAL FROM DUAL;
SELECT SEQ_PLAN_SCHEDULE_ITEM.NEXTVAL FROM DUAL;
SELECT SEQ_PLAN_INVITATION.NEXTVAL FROM DUAL;
```

플랜 생성은 하나의 Spring Transaction에서 `TRAVEL_PLAN`, `PLAN_MEMBER`, 여행 기간만큼의 `PLAN_DAY`를 순서대로 Insert해야 합니다. DDL은 Application Transaction을 대신하는 Trigger를 만들지 않습니다.
