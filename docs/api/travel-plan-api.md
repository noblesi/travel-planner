# 여행 플랜 API 계약

- 계약 버전: `2026-08-11`
- 상태: 공개 탐색·상세와 일정 자동 저장 Backend 구현 기준
- 대상 화면: 공개 일정 탐색·상세, 여행 플랜 설정, 여행 플랜 제작·날짜 편집·일정 편집
- 관련 Schema: `docs/database/ddl/001_create_travel_plan_schema.sql`

## 범위

주요 플랜 API는 다음과 같습니다.

| Method | Endpoint | 목적 |
| --- | --- | --- |
| `GET` | `/api/regions` | 활성 국내 시·도 목록 조회 |
| `GET` | `/api/plans` | 공개 플랜 제목·지역 검색 |
| `GET` | `/api/plans/{planId}` | 공개 플랜 일정 상세 조회 |
| `POST` | `/api/plans/{planId}/like` | 공개 플랜 좋아요 등록·취소 |
| `POST` | `/api/plans/{planId}/report` | 공개 플랜 신고 |
| `POST` | `/api/plans/{planId}/copy` | 공개 플랜을 내 플랜으로 복사 |
| `POST` | `/api/plans` | 플랜, 생성자, 여행 일차 생성 |
| `GET` | `/api/plans/{planId}/editor` | 제작 페이지 초기 상태 조회 |
| `GET` | `/api/plans/mine` | 내가 생성했거나 초대받은 플랜 목록 조회 |
| `PATCH` | `/api/plans/{planId}/dates` | 여행 기간 및 일차 재구성 |
| `PATCH` | `/api/plans/{planId}/publication` | 작성 중·제작 완료 상태 변경 |
| `DELETE` | `/api/plans/{planId}` | 소유 플랜 소프트 삭제 |
| `POST` | `/api/plans/{planId}/restore` | 삭제한 소유 플랜 복구 |
| `POST` | `/api/plans/{planId}/days/{dayId}/items` | 일정 항목 추가 |
| `PATCH` | `/api/plans/{planId}/days/{dayId}/items/{itemId}` | 일정 항목 시간대 또는 DAY 수정 |
| `DELETE` | `/api/plans/{planId}/days/{dayId}/items/{itemId}` | 일정 항목 삭제 |
| `PUT` | `/api/plans/{planId}/days/{dayId}/items/order` | 시간대별 일정 순서 변경 |

장소 검색은 `place-search-api.md`, session·CSRF는 `authentication-api.md`를 따릅니다.

## Domain Enum

| 이름 | 값 |
| --- | --- |
| `RegionLevel` | `SIDO`, `SIGUNGU` |
| `PlanVisibility` | `PUBLIC`, `PRIVATE` |
| `PlanPublishStatus` | `DRAFT`, `PUBLISHED` |
| `PlanStatus` | `ACTIVE`, `DELETED` |
| `ParticipantType` | `CREATOR`, `INVITEE` |
| `TimeSlot` | `MORNING`, `AFTERNOON` |

## 작성 상태와 권한

- 신규 플랜은 공개 범위와 무관하게 `DRAFT`로 생성합니다.
- 공개 탐색은 `PUBLISH_STATUS = PUBLISHED`, `VISIBILITY = PUBLIC`, `PLAN_STATUS = ACTIVE`를 모두 만족하는 플랜만 반환합니다.
- 제작 완료(`PUBLISHED`)로 변경하려면 일정 장소가 한 곳 이상 있어야 합니다.
- 에디터 응답의 `currentMemberRole`은 `CREATOR` 또는 `INVITEE`, `canManagePlan`은 소유자 전용 설정 가능 여부입니다.
- `INVITEE`는 일정 편집만 가능하고 Metadata·날짜·발행·삭제·초대 관리는 할 수 없습니다.

## 내 플랜 관리

- `GET /api/plans/mine`은 활성 소유 플랜, 활성 초대 플랜, 삭제된 소유 플랜을 최근 수정 순으로 반환합니다.
- `DELETE /api/plans/{planId}?versionNo={versionNo}`는 데이터를 물리 삭제하지 않고 `PLAN_STATUS`를 `DELETED`로 변경합니다.
- `POST /api/plans/{planId}/restore`는 `{ "versionNo": 4 }` 형식으로 삭제 당시 최신 버전을 전송합니다.
- 발행·삭제·복구는 모두 `TRAVEL_PLAN.VERSION_NO` 낙관적 잠금을 사용합니다.

## DAY 간 일정 이동

일정 수정 요청에 `targetPlanDayId`와 `targetScheduleVersion`을 함께 보내면 다른 DAY의 선택 시간대 마지막으로 이동합니다. 서버는 원본 DAY와 대상 DAY의 일정 버전을 하나의 Transaction에서 모두 증가시키며, 어느 한쪽이라도 충돌하면 전체 작업을 롤백합니다.

---

## 공개 플랜 검색

```http
GET /api/plans?keyword=서울&page=1&size=8
```

- 인증 없이 호출할 수 있습니다.
- `VISIBILITY = 'PUBLIC'`, `PUBLISH_STATUS = 'PUBLISHED'`, `PLAN_STATUS = 'ACTIVE'`, 작성자 `MEMBER_STATUS = 'ACTIVE'`인 플랜만 반환합니다.
- `keyword`는 앞뒤 공백을 제거하고 제목 또는 지역명에서 대소문자 구분 없이 검색합니다.
- `page` 기본값은 1이며 1 미만이면 1로 보정합니다.
- `size` 기본값은 10이며 서버에서 1~100 범위로 보정합니다.
- 기존 호출 호환을 위해 `limit`도 지원하며, 전달하면 `size`보다 우선합니다.
- `VIEW_COUNT DESC`, `UPDATED_AT DESC`, `PLAN_ID DESC` 순으로 정렬합니다.
- 결과가 없으면 빈 `content` 배열을 반환합니다.

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "planId": "21",
        "title": "서울 궁궐과 골목 산책",
        "region": "서울특별시",
        "days": 2,
        "thumbnailImage": null,
        "authorName": "서울산책자",
        "authorImage": null,
        "likeCount": 2,
        "viewCount": 342
      }
    ],
    "pagination": {
      "page": 1,
      "size": 8,
      "totalCount": 1,
      "totalPages": 1,
      "startPage": 1,
      "endPage": 1,
      "offset": 0
    }
  }
}
```

19자리 ID는 모두 JSON 문자열로 반환합니다.

## 공개 플랜 상세

```http
GET /api/plans/{planId}
```

- 인증 없이 호출할 수 있습니다.
- 공개·활성 플랜만 반환하며 비공개·삭제·미존재 플랜은 모두 `404 PLAN_NOT_FOUND`입니다.
- 같은 Browser에서 24시간 안에 다시 조회하면 조회수를 중복 증가시키지 않습니다.
- `days`는 `DAY_NO`, `places`는 오전·오후와 `POSITION_NO` 순으로 반환합니다.
- 로그인 상태에서는 현재 회원의 좋아요 여부를 `liked`로 반환합니다.

```json
{
  "success": true,
  "data": {
    "planId": "21",
    "title": "서울 궁궐과 골목 산책",
    "authorName": "서울산책자",
    "startDate": "2026-08-17",
    "endDate": "2026-08-18",
    "likeCount": 2,
    "viewCount": 343,
    "liked": false,
    "days": [
      {
        "dayNumber": 1,
        "visitDate": "2026-08-17",
        "places": [
          {
            "timeSlot": "MORNING",
            "placeName": "경복궁",
            "latitude": 37.579617,
            "longitude": 126.977041,
            "address": "서울특별시 종로구 사직로 161"
          }
        ]
      }
    ]
  }
}
```

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `INVALID_PATH_PARAMETER` | `planId`가 1 이상의 숫자가 아니거나 64비트 범위를 벗어남 |
| `404` | `PLAN_NOT_FOUND` | 미존재·비공개·삭제 플랜 또는 비활성 작성자 |

---

## 공개 플랜 좋아요·신고·복사

세 API는 모두 로그인 Session과 유효한 CSRF Token이 필요합니다. 대상 플랜은 공개 상세와 동일하게 `PUBLIC + PUBLISHED + ACTIVE`이고 작성자도 활성 상태여야 합니다. 조건을 만족하지 않으면 존재 여부를 구분하지 않고 `404 PLAN_NOT_FOUND`를 반환합니다.

### 좋아요 등록·취소

```http
POST /api/plans/{planId}/like
X-CSRF-TOKEN: server-generated-token
```

현재 회원이 좋아요를 누르지 않은 플랜이면 등록하고 `data: true`, 이미 누른 플랜이면 취소하고 `data: false`를 반환합니다. 성공 Status는 `200 OK`입니다.

```json
{
  "success": true,
  "data": true
}
```

### 플랜 신고

```http
POST /api/plans/{planId}/report
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "reason": "FALSE_INFO",
  "detail": "운영 시간이 실제 정보와 다릅니다."
}
```

| Property | Required | Validation |
| --- | --- | --- |
| `reason` | Yes | `INAPPROPRIATE`, `FALSE_INFO`, `SPAM`, `OTHER` 중 하나 |
| `detail` | No | 최대 1000자, 공백만 입력하면 `null`로 저장 |

본인 소유 플랜은 신고할 수 없고, 같은 회원이 같은 플랜을 두 번 신고할 수 없습니다. 성공 Status는 `200 OK`이며 응답 `data`는 `null`입니다.

### 공개 플랜 복사

```http
POST /api/plans/{sourcePlanId}/copy
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "title": "서울 여행 복사본",
  "startDate": "2026-09-01",
  "endDate": "2026-09-03"
}
```

| Property | Required | Validation |
| --- | --- | --- |
| `title` | Yes | 공백 제거 후 1~200자 |
| `startDate` | Yes | 한국 시간 기준 오늘 이후 또는 오늘 |
| `endDate` | Yes | `startDate`와 같거나 이후 |

여행 기간은 최대 14일입니다. 복사본은 요청한 기간으로 DAY를 새로 만들고, 원본에서 새 기간의 DAY 수를 초과하는 일정은 복사하지 않습니다. 복사본은 `PRIVATE + DRAFT + ACTIVE` 상태로 생성됩니다.

성공 Status는 `200 OK`이고 `data`는 JavaScript 정밀도 손실을 방지한 새 플랜 ID 문자열입니다.

```json
{
  "success": true,
  "data": "202"
}
```

### 좋아요·신고·복사 오류

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `INVALID_PATH_PARAMETER` | `planId` 또는 `sourcePlanId`가 1 이상의 숫자가 아니거나 64비트 범위를 벗어남 |
| `400` | `VALIDATION_ERROR` | 신고 또는 복사 Request 필수 값·길이·Enum 오류 |
| `400` | `SELF_PLAN_REPORT_NOT_ALLOWED` | 본인 소유 플랜 신고 |
| `400` | `INVALID_TRAVEL_DATE_RANGE` | 복사 시작일이 종료일보다 늦음 |
| `400` | `TRAVEL_PLAN_DURATION_EXCEEDED` | 복사 기간이 14일을 초과함 |
| `400` | `PAST_TRAVEL_START_DATE` | 복사 시작일이 한국 시간 기준 오늘보다 빠름 |
| `401` | `CURRENT_MEMBER_NOT_AVAILABLE` | 로그인 Session 없음 |
| `403` | `ACCESS_DENIED` | CSRF Token 누락 또는 불일치 |
| `404` | `PLAN_NOT_FOUND` | 미존재·비공개·작성 중·삭제 플랜 또는 비활성 작성자 |
| `409` | `REPORT_ALREADY_EXISTS` | 같은 회원이 같은 플랜을 중복 신고 |

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
5. `Asia/Seoul` 기준 `startDate`가 오늘 이후인지 확인합니다.
6. 제목을 `{regionName} 여행`으로 생성합니다.
7. 하나의 Transaction에서 `TRAVEL_PLAN`, `PLAN_MEMBER`, `PLAN_DAY`를 생성합니다.
8. 생성자는 `PLAN_MEMBER.PARTICIPANT_TYPE = 'CREATOR'`로 등록합니다.
9. `PLAN_DAY`는 시작일부터 종료일까지 하루에 한 행씩 생성합니다.
10. 하나라도 실패하면 전체 Transaction을 Rollback합니다.

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
| `400` | `PAST_TRAVEL_START_DATE` | 한국 시간 기준 시작일이 오늘보다 빠름 |
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
- 생성자와 수락 완료한 `PLAN_MEMBER.INVITEE`가 Editor를 조회할 수 있습니다.
- 공개 플랜의 읽기 전용 조회는 별도 API로 구현하며 Editor 접근 권한으로 사용하지 않습니다.
- `INVITEE`는 일정 항목을 편집할 수 있지만 플랜 Metadata와 날짜는 변경할 수 없습니다.
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
      "thumbnailImageUrl": null,
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
| `plan.thumbnailImageUrl` | `string` | Yes | 공개 탐색 카드에 표시할 대표 이미지 URL |
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
2. 한국 시간(`Asia/Seoul`) 기준 출발 전 플랜은 오늘 이후 범위에서 날짜를 변경할 수 있습니다.
3. 진행 중 플랜은 기존 시작일을 유지하고 종료일만 오늘 이후로 변경할 수 있습니다.
4. 종료된 플랜은 날짜 범위를 변경할 수 없습니다.
5. 시작일과 종료일을 함께 같은 간격으로 이동해 여행 일수가 유지되면 기존 DAY ID, DAY 번호 및 일정을 유지하고 날짜만 이동합니다.
6. 기간이 늘어나면 기존 날짜의 DAY와 일정은 유지하고 새 날짜에 빈 DAY를 추가합니다.
7. 기간이 줄어들면 범위에 남는 날짜의 DAY와 일정은 유지하고 DAY 번호를 다시 계산합니다.
8. 제외되는 DAY에 일정이 있고 `force=false`이면 데이터를 변경하지 않고 `409 PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED`를 반환합니다.
9. 같은 요청을 `force=true`로 다시 보내면 제외 DAY와 그 일정을 하나의 Transaction에서 삭제합니다.
10. 성공 시 `TRAVEL_PLAN.VERSION_NO`를 1 증가시키고 제작 페이지 조회와 같은 응답을 반환합니다.

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
| `400` | `PAST_TRAVEL_START_DATE` | 출발 전 플랜을 오늘보다 과거로 변경함 |
| `400` | `ONGOING_TRAVEL_START_DATE_LOCKED` | 진행 중 플랜의 시작일을 변경함 |
| `400` | `PAST_TRAVEL_END_DATE` | 진행 중 플랜의 종료일을 오늘보다 과거로 변경함 |
| `400` | `COMPLETED_TRAVEL_DATES_LOCKED` | 종료된 플랜의 날짜를 변경함 |
| `404` | `PLAN_NOT_FOUND` | 플랜이 없거나 삭제 상태이거나 현재 회원 소유가 아님 |
| `409` | `PLAN_VERSION_CONFLICT` | Request Version과 현재 플랜 Version 불일치 |
| `409` | `PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED` | 제외 DAY에 일정이 있으나 삭제 확인이 없음 |

---

## 5. 일정 자동 저장 공통 계약

일정 추가·시간대 변경·삭제·정렬은 사용자 작업 한 건마다 즉시 저장합니다. 모든 요청은 UUID 형식의 `operationId`와 현재 `PLAN_DAY.SCHEDULE_VERSION`을 포함합니다.

### 공통 처리 규칙

1. 현재 회원이 소유한 활성 플랜과 그 플랜에 속한 DAY만 변경할 수 있습니다.
2. 서버는 `operationId`, 작업 종류, 대상, 기준 Version과 정규화된 Payload의 SHA-256 Hash를 `PLAN_EDIT_OPERATION`에 저장합니다.
3. 같은 `operationId`와 같은 Payload를 재요청하면 데이터를 다시 변경하지 않고 성공 응답을 반환합니다.
4. 같은 `operationId`를 다른 Payload에 재사용하면 `409 DUPLICATE_OPERATION`을 반환합니다.
5. 실제 변경 전에 Request의 `scheduleVersion`과 현재 DAY Version을 비교합니다. 다르면 `409 SCHEDULE_VERSION_CONFLICT`를 반환합니다.
6. 항목 수정·삭제는 `itemVersion`도 비교합니다. 다르면 `409 ITEM_VERSION_CONFLICT`를 반환합니다.
7. 변경과 Version 증가, 순서 보정, 작업 이력 저장은 하나의 Transaction으로 처리합니다.
8. 실제 상태가 바뀌는 작업만 `SCHEDULE_VERSION`을 1 증가시킵니다. 같은 시간대로 수정하거나 현재 순서를 그대로 전송한 요청은 성공하지만 Version을 증가시키지 않습니다.
9. 시간대별 일정은 최대 100개입니다.
10. 성공 응답의 `editor`는 제작 페이지 전체 최신 Snapshot이며 `GET /api/plans/{planId}/editor`의 `data`와 같은 구조입니다.

### 공통 성공 응답

```json
{
  "success": true,
  "data": {
    "operationId": "4d6cb776-4a3f-4fc0-9df8-d4375bfd8593",
    "scheduleItemId": "301",
    "resultScheduleVersion": 3,
    "editor": {
      "plan": {},
      "days": []
    }
  }
}
```

| Property | Type | Nullable | 설명 |
| --- | --- | --- | --- |
| `operationId` | `string(uuid)` | No | 처리하거나 재생한 작업 ID |
| `scheduleItemId` | `string` | Yes | 추가·수정·삭제 대상 ID, 정렬은 `null` |
| `resultScheduleVersion` | `integer` | No | 이 작업 처리 직후 DAY 일정 Version |
| `editor` | `object` | No | 응답 시점의 전체 제작 페이지 Snapshot |

재시도 응답의 `resultScheduleVersion`은 최초 작업 직후 Version입니다. 이후 다른 작업이 저장되었다면 `editor.days[].scheduleVersion`이 더 높을 수 있습니다.

## 6. 일정 추가

```http
POST /api/plans/{planId}/days/{dayId}/items
Content-Type: application/json
```

### Request

```json
{
  "operationId": "4d6cb776-4a3f-4fc0-9df8-d4375bfd8593",
  "scheduleVersion": 2,
  "timeSlot": "MORNING",
  "placeProvider": "TOUR_API",
  "externalPlaceId": "126508",
  "placeName": "경복궁",
  "categoryName": "관광지",
  "address": "서울특별시 종로구 사직로 161",
  "latitude": 37.579617,
  "longitude": 126.977041,
  "imageUrl": "https://example.com/images/126508.jpg",
  "description": null
}
```

일정 추가 전에 `GET /api/places/search`로 장소를 조회해야 합니다. `placeProvider`는 현재 `TOUR_API`만 허용합니다. 서버는 `placeProvider + externalPlaceId`로 `PLACE_MASTER`를 다시 조회하고, 서버가 보관한 장소명·유형·카테고리·주소·좌표·이미지를 `PLAN_SCHEDULE_ITEM` Snapshot으로 저장합니다. Request의 나머지 장소 필드는 하위 호환용이며 썸네일이나 Snapshot 결정에 사용하지 않습니다.

| Property | Required | Validation |
| --- | --- | --- |
| `operationId` | Yes | UUID |
| `scheduleVersion` | Yes | 현재 DAY Version, 0 이상 |
| `timeSlot` | Yes | `MORNING` 또는 `AFTERNOON` |
| `placeProvider` | Yes | `TOUR_API` |
| `externalPlaceId` | Yes | 1~100자 |
| `placeName` | No | 하위 호환용, 최대 200자, 서버 값 사용 |
| `categoryName` | No | 최대 100자 |
| `address` | No | 최대 500자 |
| `latitude` | No | -90~90 |
| `longitude` | No | -180~180 |
| `imageUrl` | No | 최대 1000자 |
| `description` | No | 최대 4000자 |

### 성공 응답

- Status: `201 Created`
- `Location`: `/api/plans/{planId}/days/{dayId}/items/{scheduleItemId}`
- `scheduleItemId`: 생성된 일정 항목 ID
- 새 항목의 `itemVersion`: `0`

같은 DAY·시간대에 `placeProvider`와 `externalPlaceId`가 같은 항목은 중복 추가할 수 없습니다.

## 7. 일정 시간대 수정

```http
PATCH /api/plans/{planId}/days/{dayId}/items/{itemId}
Content-Type: application/json
```

```json
{
  "operationId": "88364dac-e083-49a7-9806-483d44521e7a",
  "scheduleVersion": 3,
  "itemVersion": 0,
  "timeSlot": "AFTERNOON"
}
```

현재 수정 범위는 같은 DAY 안에서 오전·오후 시간대를 변경하는 것입니다. 이동한 항목은 대상 시간대의 마지막에 배치하고 기존 시간대의 순서를 1부터 다시 맞춥니다. 실제 이동 시 DAY `scheduleVersion`과 항목 `itemVersion`을 각각 1 증가시킵니다. 같은 시간대로 요청하면 무변경 성공합니다.

성공 Status는 `200 OK`입니다.

## 8. 일정 삭제

```http
DELETE /api/plans/{planId}/days/{dayId}/items/{itemId}
Content-Type: application/json
```

```json
{
  "operationId": "ec120125-81b8-411b-b2ef-c9c096f5ed51",
  "scheduleVersion": 4,
  "itemVersion": 1
}
```

삭제 후 같은 시간대의 뒤 항목을 앞으로 당겨 `positionNo`를 1부터 연속으로 유지합니다. 성공 Status는 `200 OK`이며 `scheduleItemId`는 삭제된 ID입니다.

## 9. 일정 순서 변경

```http
PUT /api/plans/{planId}/days/{dayId}/items/order
Content-Type: application/json
```

```json
{
  "operationId": "b986a022-a91a-461f-b2bf-2a008cf30d7f",
  "scheduleVersion": 5,
  "timeSlot": "MORNING",
  "scheduleItemIds": ["303", "301", "302"]
}
```

`scheduleItemIds`는 선택한 DAY·시간대에 현재 존재하는 모든 일정 ID를 중복 없이 정확히 한 번씩 포함해야 합니다. 배열 순서대로 `positionNo`를 1부터 부여합니다. 현재 순서와 같으면 무변경 성공합니다. 성공 Status는 `200 OK`이며 `scheduleItemId`는 `null`입니다.

### 일정 변경 오류

| Status | Code | 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수 값, UUID, Snapshot 길이 또는 좌표 범위 오류 |
| `400` | `MALFORMED_JSON` | JSON 또는 Enum 형식 오류 |
| `400` | `INVALID_PATH_PARAMETER` | `planId`, `dayId` 또는 `itemId` 형식 오류 |
| `400` | `INVALID_SCHEDULE_ORDER` | 정렬 목록 ID 형식 오류 또는 목록의 누락·추가·중복 |
| `400` | `PLACE_REFERENCE_NOT_FOUND` | 서버 장소 검색 이력이 없거나 비활성 장소를 추가하려고 함 |
| `404` | `PLAN_NOT_FOUND` | 플랜이 없거나 삭제 상태이거나 현재 회원 소유가 아님 |
| `404` | `PLAN_DAY_NOT_FOUND` | DAY가 없거나 대상 플랜에 속하지 않음 |
| `404` | `SCHEDULE_ITEM_NOT_FOUND` | 항목이 없거나 대상 DAY에 속하지 않음 |
| `409` | `SCHEDULE_VERSION_CONFLICT` | Request DAY Version 불일치 |
| `409` | `ITEM_VERSION_CONFLICT` | Request 항목 Version 불일치 |
| `409` | `DUPLICATE_OPERATION` | 같은 작업 ID를 다른 요청에 재사용 |
| `409` | `SCHEDULE_ITEM_ALREADY_EXISTS` | 같은 DAY·시간대에 동일 장소가 존재 |
| `409` | `SCHEDULE_ITEM_LIMIT_EXCEEDED` | 시간대별 100개 제한 초과 |

## 10. 플랜 Metadata 수정

```http
PATCH /api/plans/{planId}
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "title": "서울 맛집 여행",
  "visibility": "PUBLIC",
  "versionNo": 3
}
```

- 생성자만 제목과 공개 범위를 변경할 수 있습니다.
- 제목은 공백 제거 후 1~200자이고 공개 범위는 `PUBLIC` 또는 `PRIVATE`입니다.
- 실제 값이 변경되면 `VERSION_NO`가 1 증가하고 전체 Editor Snapshot을 반환합니다.
- 같은 값이면 무변경 성공하며 Version을 증가시키지 않습니다.
- 현재 Version과 다르면 `409 PLAN_VERSION_CONFLICT`입니다.

## 11. 플랜 대표 이미지 자동 결정

- 사용자는 대표 이미지를 직접 선택하거나 업로드하지 않습니다.
- `PLACE_MASTER.PLACE_TYPE`을 우선 사용하고, 기존 일정은 카테고리 Snapshot을 호환 기준으로 사용합니다.
- 우선순위는 `관광지 → 문화시설 → 축제·공연·행사 → 여행코스 → 레포츠 → 관광정보 → 쇼핑`입니다.
- 같은 카테고리 안에서는 DAY, 오전·오후, 일정 순서를 따릅니다.
- `음식점`, `숙박` 이미지는 대표 이미지 후보에서 제외합니다.
- 절대 `http` 또는 `https` URL만 후보로 인정하며, Frontend는 실제 이미지 로딩 실패 시 로컬 기본 썸네일로 교체합니다.
- 제작 완료와 일정 추가·수정·삭제·정렬 시 `THUMBNAIL_IMG`를 다시 계산합니다.
- 이미 `PUBLISHED`인 플랜도 제작 완료 요청을 다시 받으면 썸네일을 재계산하며 Version은 증가시키지 않습니다.
- 계산 결과가 기존 값과 같으면 DB Update를 수행하지 않습니다.
- 후보가 없으면 `THUMBNAIL_IMG = null`을 유지하고 Frontend가 로컬 기본 썸네일을 표시합니다.

## 12. 플랜 초대

### 초대 생성

```http
POST /api/plans/{planId}/invitations
Content-Type: application/json
X-CSRF-TOKEN: server-generated-token
```

```json
{
  "inviteeEmails": ["friend@example.com"]
}
```

성공 Status는 `201 Created`입니다. 이메일은 앞뒤 공백 제거와 소문자 변환 후 중복을 제거하며 한 번에 최대 20개까지 처리합니다. 같은 플랜·이메일의 기존 `PENDING` 초대는 `CANCELED`로 바꾸고 새 링크를 발급합니다. 초대 메일 발송은 초대 데이터 Transaction이 커밋된 뒤 시작하며, Transaction이 롤백되면 메일을 발송하지 않습니다.

```json
{
  "success": true,
  "data": {
    "planId": "101",
    "invitations": [
      {
        "invitationId": "501",
        "inviteeEmail": "friend@example.com",
        "token": "raw-token-returned-once",
        "expiresAt": "2026-08-02T09:00:00Z"
      }
    ]
  }
}
```

Token 원문은 생성 응답에만 반환하고 DB에는 SHA-256 Hash만 저장합니다. 유효시간은 생성 시점부터 24시간입니다.

`inviteeEmail`은 초대 메일의 전달 대상과 화면 표시용 정보이며 수락 권한을 증명하는 값은 아닙니다. 초대 Token은 링크를 소지한 사람이 사용할 수 있는 Bearer Credential입니다. 따라서 로그인한 활성 회원은 계정 이메일이 `inviteeEmail`과 달라도 유효한 Token을 수락할 수 있고, 링크를 전달하면 수락 권한도 함께 전달됩니다.

Bearer Token 보호를 위해 다음 운영 규칙을 적용합니다.

- 초대 Token 원문과 전체 초대 URL을 애플리케이션·Reverse Proxy·분석 도구의 로그에 남기지 않습니다.
- 초대 수락 화면에서는 외부 리소스로 URL이 전달되지 않도록 `Referrer-Policy: no-referrer`를 적용합니다.
- 오류·모니터링 이벤트에는 Token 원문 대신 오류 코드와 초대 식별자만 기록합니다.

### 초대 조회와 수락

```http
GET  /api/plan-invitations/{token}
POST /api/plan-invitations/{token}/accept
```

- 조회는 로그인하지 않아도 가능하며 플랜 제목·지역·기간·초대 이메일·상태·만료 시각을 반환합니다.
- 수락은 로그인 session과 CSRF token이 필요합니다.
- 비로그인 사용자는 Token을 유지한 채 회원가입·로그인으로 이동하고, 인증 완료 후 원래 초대 수락 화면으로 복귀합니다.
- 로그인 사용자는 계정 이메일 일치 여부를 추가로 검사하지 않고 유효한 Token을 바로 수락합니다.
- 수락 회원을 `PLAN_MEMBER.INVITEE`로 등록하고 이후 일정 조회·편집을 허용합니다.
- 초대 참여자는 플랜 Metadata와 날짜를 변경할 수 없습니다.
- 같은 회원의 같은 token 재수락은 멱등 성공합니다.
- 초대한 본인은 자기 초대를 수락할 수 없습니다.

| Status | Code | 조건 |
| --- | --- | --- |
| `404` | `INVITATION_NOT_FOUND` | token 형식이 잘못됐거나 존재하지 않음 |
| `409` | `INVITATION_NOT_AVAILABLE` | 취소·거절 등 사용할 수 없는 상태 |
| `409` | `INVITATION_SELF_ACCEPTANCE_NOT_ALLOWED` | 초대한 본인이 수락 시도 |
| `410` | `INVITATION_EXPIRED` | 24시간 만료 |

## Version 의미

| 변경 범위 | 기준 Version | 충돌 오류 |
| --- | --- | --- |
| 제목, 공개 범위 등 플랜 Metadata | `TRAVEL_PLAN.VERSION_NO` | `PLAN_VERSION_CONFLICT` |
| 여행 기간 변경에 따른 DAY 추가, 삭제, 날짜 이동 | `TRAVEL_PLAN.VERSION_NO` | `PLAN_VERSION_CONFLICT` |
| 일정 추가·삭제·시간대 이동·정렬 | `PLAN_DAY.SCHEDULE_VERSION` | `SCHEDULE_VERSION_CONFLICT` |
| 일정 항목 시간대 이동·삭제 | `PLAN_SCHEDULE_ITEM.ITEM_VERSION` | `ITEM_VERSION_CONFLICT` |

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
├── UpdateTravelPlanMetadataRequest
├── UpdateTravelPlanDatesRequest
├── CreatePlanInvitationsRequest
├── CreatePlanInvitationsResponse
├── PlanInvitationResponse
├── AcceptPlanInvitationResponse
├── AddScheduleItemRequest
├── UpdateScheduleItemRequest
├── DeleteScheduleItemRequest
├── ReorderScheduleItemsRequest
└── ScheduleMutationResponse
```

DTO의 19자리 ID Property는 Java 내부에서 `long` 또는 `Long`을 사용할 수 있지만 JSON에서는 문자열로 직렬화해야 합니다. Domain Object나 Mapper 결과를 Controller에서 직접 반환하지 않습니다.

## 구현 완료 조건

- 지역·플랜·일정 API가 이 문서의 Status, Header, Body 구조를 반환합니다.
- 날짜 경계값 1일, 14일, 15일 Test가 존재합니다.
- 생성 실패 시 `TRAVEL_PLAN`, `PLAN_MEMBER`, `PLAN_DAY`에 부분 Data가 남지 않습니다.
- 현재 회원 ID를 Request에서 받지 않습니다.
- 삭제 플랜과 타인 소유 플랜이 모두 `PLAN_NOT_FOUND`를 반환하는지 Test합니다.
- 빈 일정은 `items: []`로 반환하고 `null`로 반환하지 않습니다.
- ID가 JavaScript 안전 정수 범위를 넘어도 문자열로 손실 없이 반환됩니다.
- 기존 `ApiResponse`와 `ErrorResponse` 형식을 유지합니다.
- 일정 변경은 Version 충돌 시 부분 Data나 작업 이력을 남기지 않습니다.
- 같은 `operationId`와 같은 Payload 재시도는 멱등 성공하고, 다른 Payload 재사용은 `DUPLICATE_OPERATION`을 반환합니다.
