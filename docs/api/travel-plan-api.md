# 여행 플랜 1차 API 계약

- 계약 버전: `2026-07-29`
- 상태: 날짜 편집 구현 기준
- 대상 화면: 여행 플랜 설정, 여행 플랜 제작 및 날짜 편집
- 관련 Schema: `docs/database/ddl/001_create_travel_plan_schema.sql`

## 범위

이번 계약에서 확정하는 API는 다음 네 가지입니다.

| Method | Endpoint | 목적 |
| --- | --- | --- |
| `GET` | `/api/regions` | 활성 국내 시·도 목록 조회 |
| `POST` | `/api/plans` | 플랜, 생성자, 여행 일차 생성 |
| `GET` | `/api/plans/{planId}/editor` | 제작 페이지 초기 상태 조회 |
| `PATCH` | `/api/plans/{planId}/dates` | 여행 기간 및 일차 재구성 |

장소 검색, 일정 자동 저장, 제목·공개 범위 수정, 초대 API는 후속 계약에서 추가합니다.

## Domain Enum

| 이름 | 값 |
| --- | --- |
| `RegionLevel` | `SIDO`, `SIGUNGU` |
| `PlanVisibility` | `PUBLIC`, `PRIVATE` |
| `PlanStatus` | `ACTIVE`, `DELETED` |
| `ParticipantType` | `CREATOR`, `INVITEE` |
| `TimeSlot` | `MORNING`, `AFTERNOON` |

---

## 1. 국내 시·도 목록 조회

```http
GET /api/regions
```

### 처리 규칙

- `REGION_LEVEL = 'SIDO'`이고 `ACTIVE_YN = 'Y'`인 지역만 반환합니다.
- `DISPLAY_ORDER`, `REGION_NAME` 오름차순으로 정렬합니다.
- 결과가 없으면 `404`가 아니라 빈 `regions` 배열을 반환합니다.

### 성공 응답

Status: `200 OK`

```json
{
  "success": true,
  "data": {
    "regions": [
      {
        "regionCode": "1",
        "regionName": "서울특별시",
        "regionLevel": "SIDO",
        "parentRegionCode": null
      },
      {
        "regionCode": "6",
        "regionName": "부산광역시",
        "regionLevel": "SIDO",
        "parentRegionCode": null
      }
    ]
  }
}
```

### Response Schema

| Property | Type | Nullable | 설명 |
| --- | --- | --- | --- |
| `regions` | `array` | No | 지역 목록 |
| `regions[].regionCode` | `string` | No | TourAPI 시·도 `areaCode` |
| `regions[].regionName` | `string` | No | 지역 표시명 |
| `regions[].regionLevel` | `string` | No | 현재 API에서는 항상 `SIDO` |
| `regions[].parentRegionCode` | `string` | Yes | 시·도이므로 항상 `null` |

---

## 2. 여행 플랜 생성

```http
POST /api/plans
Content-Type: application/json
```

### Request

```json
{
  "regionCode": "1",
  "startDate": "2026-08-10",
  "endDate": "2026-08-12",
  "visibility": "PRIVATE"
}
```

### Request Schema

| Property | Type | Required | Validation |
| --- | --- | --- | --- |
| `regionCode` | `string` | Yes | 공백 불가, 최대 20자, 활성 `SIDO` 코드 |
| `startDate` | `string(date)` | Yes | `YYYY-MM-DD` |
| `endDate` | `string(date)` | Yes | `YYYY-MM-DD`, `startDate` 이상 |
| `visibility` | `string` | Yes | `PUBLIC` 또는 `PRIVATE` |

`title`, `ownerMemberId`, `versionNo`는 Client가 전달하지 않습니다.

### Business Rule

1. `CurrentMemberProvider`에서 현재 회원 ID를 조회합니다.
2. `regionCode`에 해당하는 활성 `SIDO`를 조회합니다.
3. `startDate <= endDate`인지 확인합니다.
4. 시작일과 종료일을 포함한 여행 기간이 1~14일인지 확인합니다.
5. 제목을 `{regionName} 여행`으로 생성합니다.
6. 하나의 Transaction에서 `TRAVEL_PLAN`, `PLAN_MEMBER`, `PLAN_DAY`를 생성합니다.
7. 생성자는 `PLAN_MEMBER.PARTICIPANT_TYPE = 'CREATOR'`로 등록합니다.
8. `PLAN_DAY`는 시작일부터 종료일까지 하루에 한 행씩 생성합니다.
9. 하나라도 실패하면 전체 Transaction을 Rollback합니다.

### 성공 응답

Status: `201 Created`

Header:

```http
Location: /api/plans/101/editor
```

Body:

```json
{
  "success": true,
  "data": {
    "planId": "101",
    "title": "서울특별시 여행",
    "region": {
      "regionCode": "1",
      "regionName": "서울특별시"
    },
    "startDate": "2026-08-10",
    "endDate": "2026-08-12",
    "visibility": "PRIVATE",
    "versionNo": 0,
    "days": [
      {
        "planDayId": "201",
        "dayNo": 1,
        "travelDate": "2026-08-10",
        "scheduleVersion": 0
      },
      {
        "planDayId": "202",
        "dayNo": 2,
        "travelDate": "2026-08-11",
        "scheduleVersion": 0
      },
      {
        "planDayId": "203",
        "dayNo": 3,
        "travelDate": "2026-08-12",
        "scheduleVersion": 0
      }
    ]
  }
}
```

### Response Schema

| Property | Type | Nullable | 설명 |
| --- | --- | --- | --- |
| `planId` | `string` | No | 생성된 플랜 ID |
| `title` | `string` | No | 서버가 생성한 기본 제목 |
| `region.regionCode` | `string` | No | 선택한 지역코드 |
| `region.regionName` | `string` | No | 선택한 지역명 |
| `startDate` | `string(date)` | No | 여행 시작일 |
| `endDate` | `string(date)` | No | 여행 종료일 |
| `visibility` | `string` | No | 공개 범위 |
| `versionNo` | `integer` | No | 플랜 Metadata Version, 최초 `0` |
| `days` | `array` | No | `dayNo` 오름차순 일차 목록 |
| `days[].planDayId` | `string` | No | 여행 일차 ID |
| `days[].dayNo` | `integer` | No | 1부터 시작하는 일차 |
| `days[].travelDate` | `string(date)` | No | 해당 일차 날짜 |
| `days[].scheduleVersion` | `integer` | No | 일정 Version, 최초 `0` |

### 오류

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수값, 길이, Enum Validation 실패 |
| `400` | `MALFORMED_JSON` | 날짜 또는 JSON 형식 오류 |
| `400` | `INVALID_TRAVEL_DATE_RANGE` | 시작일이 종료일보다 늦음 |
| `400` | `TRAVEL_PLAN_DURATION_EXCEEDED` | 여행 기간이 14일 초과 |
| `401` | `CURRENT_MEMBER_NOT_AVAILABLE` | 현재 회원 ID 조회 실패 |
| `404` | `REGION_NOT_FOUND` | 지역이 없거나 비활성 또는 `SIDO`가 아님 |

인증 구현 완료 전 개발 Profile에서는 `CurrentMemberProvider`의 Mock 구현을 사용할 수 있습니다. 회원 ID를 Request에 임시로 추가하지 않습니다.

---

## 3. 제작 페이지 초기 조회

```http
GET /api/plans/{planId}/editor
```

### Path Parameter

| 이름 | Type | Validation |
| --- | --- | --- |
| `planId` | `string` | 1 이상의 숫자로 구성된 플랜 ID |

### 접근 규칙

- `PLAN_STATUS = 'ACTIVE'`인 플랜만 조회합니다.
- 1차 구현에서는 `OWNER_MEMBER_ID`가 현재 회원 ID인 경우에만 접근할 수 있습니다.
- 공개 플랜의 읽기 전용 조회는 별도 API로 구현하며 Editor 접근 권한으로 사용하지 않습니다.
- `INVITEE` 편집 권한은 인증 및 초대 권한 범위 확정 후 계약을 확장합니다.
- 삭제되었거나 존재하지 않는 플랜은 모두 `PLAN_NOT_FOUND`로 반환합니다.
- 타인 소유 플랜도 존재 여부를 노출하지 않도록 `PLAN_NOT_FOUND`로 반환합니다.

### 성공 응답

Status: `200 OK`

```json
{
  "success": true,
  "data": {
    "plan": {
      "planId": "101",
      "title": "서울특별시 여행",
      "regionCode": "1",
      "regionName": "서울특별시",
      "startDate": "2026-08-10",
      "endDate": "2026-08-12",
      "visibility": "PRIVATE",
      "versionNo": 0
    },
    "days": [
      {
        "planDayId": "201",
        "dayNo": 1,
        "travelDate": "2026-08-10",
        "scheduleVersion": 2,
        "items": [
          {
            "scheduleItemId": "301",
            "timeSlot": "MORNING",
            "positionNo": 1,
            "placeProvider": "TOUR_API",
            "externalPlaceId": "126508",
            "placeName": "경복궁",
            "categoryName": "관광지",
            "address": "서울특별시 종로구 사직로 161",
            "latitude": 37.579617,
            "longitude": 126.977041,
            "imageUrl": "https://example.com/images/126508.jpg",
            "description": null,
            "itemVersion": 0
          }
        ]
      }
    ]
  }
}
```

### Response Schema

| Property | Type | Nullable | 설명 |
| --- | --- | --- | --- |
| `plan.planId` | `string` | No | 플랜 ID |
| `plan.title` | `string` | No | 플랜 제목 |
| `plan.regionCode` | `string` | No | 여행지역 코드 |
| `plan.regionName` | `string` | No | 여행지역 이름 |
| `plan.startDate` | `string(date)` | No | 여행 시작일 |
| `plan.endDate` | `string(date)` | No | 여행 종료일 |
| `plan.visibility` | `string` | No | 공개 범위 |
| `plan.versionNo` | `integer` | No | Metadata Version |
| `days` | `array` | No | `dayNo` 오름차순 일차 목록 |
| `days[].planDayId` | `string` | No | 일차 ID |
| `days[].dayNo` | `integer` | No | 일차 번호 |
| `days[].travelDate` | `string(date)` | No | 여행 날짜 |
| `days[].scheduleVersion` | `integer` | No | 일정 목록 Version |
| `days[].items` | `array` | No | 시간대, 표시순서로 정렬된 일정 항목 |
| `days[].items[].scheduleItemId` | `string` | No | 일정 항목 ID |
| `days[].items[].timeSlot` | `string` | No | `MORNING` 또는 `AFTERNOON` |
| `days[].items[].positionNo` | `integer` | No | 시간대 안의 1부터 시작하는 표시순서 |
| `days[].items[].placeProvider` | `string` | No | 장소 제공자, TourAPI는 `TOUR_API` |
| `days[].items[].externalPlaceId` | `string` | No | 외부 장소 ID |
| `days[].items[].placeName` | `string` | No | 저장 시점 장소명 Snapshot |
| `days[].items[].categoryName` | `string` | Yes | 저장 시점 카테고리 Snapshot |
| `days[].items[].address` | `string` | Yes | 저장 시점 주소 Snapshot |
| `days[].items[].latitude` | `number` | Yes | 저장 시점 위도 Snapshot |
| `days[].items[].longitude` | `number` | Yes | 저장 시점 경도 Snapshot |
| `days[].items[].imageUrl` | `string` | Yes | 저장 시점 이미지 URL Snapshot |
| `days[].items[].description` | `string` | Yes | 저장 시점 설명 Snapshot |
| `days[].items[].itemVersion` | `integer` | No | 항목 Version |

### 정렬 규칙

1. `days`: `DAY_NO ASC`
2. `items`: `MORNING` 먼저, `AFTERNOON` 다음
3. 같은 시간대: `POSITION_NO ASC`

### 오류

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `INVALID_PATH_PARAMETER` | `planId` 형식 또는 범위 오류 |
| `401` | `CURRENT_MEMBER_NOT_AVAILABLE` | 현재 회원 ID 조회 실패 |
| `404` | `PLAN_NOT_FOUND` | 플랜이 없거나 삭제 상태이거나 현재 회원 소유가 아님 |

---

## 4. 여행 날짜 변경

```http
PATCH /api/plans/{planId}/dates
Content-Type: application/json
```

### Request

```json
{
  "startDate": "2026-08-09",
  "endDate": "2026-08-12",
  "versionNo": 0,
  "force": false
}
```

| Property | Type | Required | Validation |
| --- | --- | --- | --- |
| `startDate` | `string(date)` | Yes | `YYYY-MM-DD` |
| `endDate` | `string(date)` | Yes | `startDate` 이상, 포함 기간 최대 14일 |
| `versionNo` | `integer` | Yes | 현재 `TRAVEL_PLAN.VERSION_NO`, 0 이상 |
| `force` | `boolean` | No | 기본 `false`, 일정이 포함된 제외 DAY 삭제 확인 |

### 처리 규칙

1. 현재 소유자의 활성 플랜만 변경할 수 있습니다.
2. 시작일과 종료일을 함께 같은 간격으로 이동해 여행 일수가 유지되면 기존 DAY ID, DAY 번호 및 일정을 유지하고 날짜만 이동합니다.
3. 기간이 늘어나면 기존 날짜의 DAY와 일정은 유지하고 새 날짜에 빈 DAY를 추가합니다.
4. 기간이 줄어들면 범위에 남는 날짜의 DAY와 일정은 유지하고 DAY 번호를 다시 계산합니다.
5. 제외되는 DAY에 일정이 있고 `force=false`이면 데이터를 변경하지 않고 `409 PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED`를 반환합니다.
6. 같은 요청을 `force=true`로 다시 보내면 제외 DAY와 그 일정을 하나의 Transaction에서 삭제합니다.
7. 성공 시 `TRAVEL_PLAN.VERSION_NO`를 1 증가시키고 제작 페이지 조회와 같은 응답을 반환합니다.

### 성공 응답

Status: `200 OK`

응답 Body는 `GET /api/plans/{planId}/editor`의 성공 응답과 동일하며 변경된 `plan`, `days`, `items`를 반환합니다.

### 오류

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수 날짜 또는 Version Validation 실패 |
| `400` | `MALFORMED_JSON` | 날짜 또는 JSON 형식 오류 |
| `400` | `INVALID_PATH_PARAMETER` | `planId` 형식 또는 범위 오류 |
| `400` | `INVALID_TRAVEL_DATE_RANGE` | 시작일이 종료일보다 늦음 |
| `400` | `TRAVEL_PLAN_DURATION_EXCEEDED` | 여행 기간이 14일 초과 |
| `404` | `PLAN_NOT_FOUND` | 플랜이 없거나 삭제 상태이거나 현재 회원 소유가 아님 |
| `409` | `PLAN_VERSION_CONFLICT` | Request Version과 현재 플랜 Version 불일치 |
| `409` | `PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED` | 제외 DAY에 일정이 있으나 삭제 확인이 없음 |

## Version 의미

| 변경 범위 | 기준 Version | 충돌 오류 |
| --- | --- | --- |
| 제목, 공개 범위 등 플랜 Metadata | `TRAVEL_PLAN.VERSION_NO` | `PLAN_VERSION_CONFLICT` |
| 여행 기간 변경에 따른 DAY 추가, 삭제, 날짜 이동 | `TRAVEL_PLAN.VERSION_NO` | `PLAN_VERSION_CONFLICT` |
| DAY 내부 일정 이동, 정렬 | `PLAN_DAY.SCHEDULE_VERSION` | `SCHEDULE_VERSION_CONFLICT` |
| 일정 항목 자체 수정 | `PLAN_SCHEDULE_ITEM.ITEM_VERSION` | `ITEM_VERSION_CONFLICT` |

Version은 성공한 변경마다 `현재 값 + 1`로 증가시킵니다. 자동 저장 API는 현재 Version을 Request에 포함하고, 불일치하면 데이터를 덮어쓰지 않고 `409 Conflict`를 반환해야 합니다.

## 권장 Backend DTO

```text
dto/region/
├── RegionListResponse
└── RegionSummaryResponse

dto/plan/
├── CreateTravelPlanRequest
├── CreateTravelPlanResponse
├── CreatedPlanDayResponse
├── PlanEditorResponse
├── PlanEditorSummaryResponse
├── PlanEditorDayResponse
├── PlanEditorItemResponse
└── UpdateTravelPlanDatesRequest
```

DTO의 19자리 ID Property는 Java 내부에서 `long` 또는 `Long`을 사용할 수 있지만 JSON에서는 문자열로 직렬화해야 합니다. Domain Object나 Mapper 결과를 Controller에서 직접 반환하지 않습니다.

## 구현 완료 조건

- 네 API가 이 문서의 Status, Header, Body 구조를 반환합니다.
- 날짜 경계값 1일, 14일, 15일 Test가 존재합니다.
- 생성 실패 시 `TRAVEL_PLAN`, `PLAN_MEMBER`, `PLAN_DAY`에 부분 Data가 남지 않습니다.
- 현재 회원 ID를 Request에서 받지 않습니다.
- 삭제 플랜과 타인 소유 플랜이 모두 `PLAN_NOT_FOUND`를 반환하는지 Test합니다.
- 빈 일정은 `items: []`로 반환하고 `null`로 반환하지 않습니다.
- ID가 JavaScript 안전 정수 범위를 넘어도 문자열로 손실 없이 반환됩니다.
- 기존 `ApiResponse`와 `ErrorResponse` 형식을 유지합니다.
