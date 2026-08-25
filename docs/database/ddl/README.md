# 여행 플랜 Oracle DDL

`travelplanner_v2.exerd`와 KMS 인수인계 문서에서 여행 플랜 핵심 영역을 분리한 Oracle 19c+ 기준 Schema입니다.

## 파일과 실행 순서

1. `001_create_travel_plan_schema.sql`: 핵심 8개 Table, 4개 Sequence, Constraint, Index 생성
2. `002_seed_region_master.sql`: TourAPI 시·도 17개 초기 데이터 입력
3. `004_verify_travel_plan_schema.sql`: 생성 결과를 읽기 전용 Query로 검증
4. `005_add_plan_operation_request_hash.sql`: 구형 Schema에만 자동 저장 요청 Hash Column 추가
5. `006_add_plan_publish_status.sql`: 구형 Schema에만 발행 상태 Column과 Index 추가
6. `007_add_report_integrity_constraints.sql`: 동일 회원의 중복 신고와 정의되지 않은 신고 사유 차단
7. `008_backfill_plan_thumbnails.sql`: 기존 공개·제작 완료 플랜의 대표 이미지 멱등 재계산
8. `009_verify_plan_thumbnails.sql`: 장소 유형과 저장된 대표 이미지 일치 여부 읽기 전용 검증
9. `010_align_schedule_coordinate_types.sql`: 과거 통합 DDL로 만든 Schema의 문자열 좌표를 숫자형으로 변환할 때만 실행
10. `011_rebuild_public_plan_indexes.sql`: `006`까지 적용된 기존 Schema의 공개 플랜 Index를 canonical 이름과 발행 상태 기준으로 정리
11. `012_add_tour_sync_execution.sql`: 다중 Instance TOUR API 동기화 Lease와 실행 이력 Table 추가. 구형 통합 DDL의 이력은 신형 구조로 보존 전환
12. `003_add_identity_foreign_keys.sql`: 인증 Table 확정 후에만 선택 실행

빈 애플리케이션 Schema에서 다음과 같이 실행합니다. 비밀번호는 명령행에 넣지 않고 Prompt에서 입력합니다.

```powershell
$env:NLS_LANG = 'KOREAN_KOREA.AL32UTF8'
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/001_create_travel_plan_schema.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/002_seed_region_master.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/012_add_tour_sync_execution.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/004_verify_travel_plan_schema.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/008_backfill_plan_thumbnails.sql
sqlplus travel_planner@//localhost:1521/FREEPDB1 `@docs/database/ddl/009_verify_plan_thumbnails.sql
```

Windows Oracle Client의 문자셋이 UTF-8이 아니면 한글 Seed 실행 중 `ORA-01756`이 발생할 수 있으므로 위 `NLS_LANG`을 먼저 설정합니다.

`001`은 일회성 생성 Script입니다. 일부 Object가 이미 있는 Schema에서 재실행하면 실패합니다. 자동 Drop은 데이터 손실 위험이 있어 제공하지 않습니다.

`travelplanner_final.sql`은 전체 Schema를 초기화하는 파괴적 개발용 Script입니다. 기존 데이터가 있는 환경에는 실행하지 않습니다. 신규 여행 플랜 Schema의 canonical 기준은 `001`, `002`, `012`입니다. `001`에는 `005`와 `006`의 변경이 이미 반영되어 있으므로 신규 Schema에는 두 파일을 다시 실행하지 않습니다. 과거 `travelplanner_final.sql`로 생성한 Schema에서 좌표 Column이 `VARCHAR2`인 경우에만 백업 후 `010`을 실행합니다. `011`은 `006`까지 적용되어 `IX_TRAVEL_PLAN_PUBLISH`가 존재하고, 기존 `IX_PLAN_PUBLIC_UPDATED`와 `IX_PLAN_REGION_PUBLIC`에는 `PUBLISH_STATUS`가 없는 Schema에서만 한 번 실행합니다. 구형 `TOUR_SYNC_HISTORY`가 있는 Schema에서도 `012`를 한 번 실행하면 기존 이력을 신형 구조로 옮기고 더 이상 사용하지 않는 Sequence를 제거합니다.

## Oracle 접속 전 로컬 개발

실제 Oracle 접속 정보가 준비되기 전에는 Spring의 `local` Profile을 사용합니다.

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

- `backend/src/main/resources/application-local.yml`에서만 H2 Memory Database를 활성화합니다.
- 로컬 초기화 Script는 `backend/src/main/resources/db/local/`에 둡니다.
- 현재 로컬 Schema는 지역 조회와 플랜 생성·편집·일정 자동 저장 개발 범위를 제공합니다.
- Oracle DDL이 최종 기준이며 로컬 Script의 변경은 관련 Oracle DDL과 의미가 일치해야 합니다.
- 실제 Oracle 접속 정보가 준비되면 이 문서의 `001`, `002`, `012`, `004` 순서로 반드시 다시 검증합니다.

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
- `PLAN_EDIT_OPERATION.REQUEST_HASH`는 같은 작업 ID의 동일 재시도와 다른 Payload 재사용을 구분하는 SHA-256 값입니다. `001` 적용이 끝난 기존 Schema에만 `005`를 한 번 실행합니다.
- `TRAVEL_PLAN.PUBLISH_STATUS`는 자동 저장 상태와 공개 탐색 노출을 분리합니다. 신규 플랜은 `DRAFT`, 제작 완료 플랜은 `PUBLISHED`입니다.
- `PLACE_MASTER`의 PK는 외부 제공자와 외부 장소 ID의 복합키이며, 일정에는 장소 Snapshot을 보관합니다.
- TourAPI 검색 결과는 `PLACE_MASTER.PLACE_TYPE`에 서버 정규화 유형을 저장합니다. 일정 추가 Request의 표시용 장소 필드는 신뢰하지 않고 이 Master를 다시 조회합니다.
- `008`은 `PUBLIC + PUBLISHED + ACTIVE` 플랜에만 적용되며 여러 번 실행해도 같은 결과입니다. 관광 후보가 없으면 `THUMBNAIL_IMG`를 `NULL`로 유지합니다.
- `009`의 두 Query는 모두 0행이어야 합니다.
- `TOUR_SYNC_STATE`의 단일 행은 동기화 Lease를 관리하며, 만료된 Lease는 다음 실행이 인수할 수 있습니다.
- `TOUR_SYNC_HISTORY`는 재시작 후에도 관리자 화면의 최근 동기화 결과를 유지합니다.
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
