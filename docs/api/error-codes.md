# API 오류 코드

오류 응답은 기존 `ErrorResponse` 형식을 그대로 사용합니다. `code`는 Frontend 분기 처리에 사용하는 안정적인 값이고 `message`는 사용자 또는 개발자에게 보여줄 설명입니다.

## 응답 형식

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [
    {
      "field": "startDate",
      "message": "필수 값입니다."
    }
  ],
  "timestamp": "2026-07-23T13:30:00Z",
  "path": "/api/plans"
}
```

- `errors`는 Field Validation 오류가 아니면 빈 배열입니다.
- `timestamp`는 Backend의 `Instant`를 UTC ISO 8601로 직렬화한 값입니다.
- 예상하지 못한 Exception의 내부 Message, SQL, Stack Trace는 응답에 포함하지 않습니다.

## 공통 오류

| HTTP Status | Code | 사용 조건 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | Jakarta Validation 실패 |
| `400` | `MALFORMED_JSON` | JSON 문법 오류, 잘못된 날짜 또는 Enum 형식 |
| `400` | `INVALID_PATH_PARAMETER` | Path Parameter 형식 또는 범위 오류 |
| `400` | `INVALID_REQUEST_PARAMETER` | Query Parameter 형식 또는 범위 오류 |
| `401` | `CURRENT_MEMBER_NOT_AVAILABLE` | 현재 회원을 확인할 수 없음 |
| `401` | `INVALID_LOGIN_CREDENTIALS` | local 로그인 이메일 또는 비밀번호 불일치, local Credential 미설정 |
| `403` | `CSRF_TOKEN_INVALID` | CSRF token 누락·불일치·만료. Frontend는 token을 갱신한 뒤 한 번만 재시도 |
| `403` | `ACCESS_DENIED` | CSRF 이외의 Spring Security 접근 거부 |
| `404` | `RESOURCE_NOT_FOUND` | 등록되지 않은 API 또는 정적 리소스 경로 요청 |
| `500` | `INTERNAL_SERVER_ERROR` | 처리되지 않은 서버 오류 |

## 여행 플랜 오류

| HTTP Status | Code | 사용 조건 |
| --- | --- | --- |
| `400` | `INVALID_TRAVEL_DATE_RANGE` | `startDate`가 `endDate`보다 늦음 |
| `400` | `TRAVEL_PLAN_DURATION_EXCEEDED` | 여행 기간이 14일을 초과함 |
| `400` | `PAST_TRAVEL_START_DATE` | 한국 시간 기준 여행 시작일이 오늘보다 빠름 |
| `400` | `ONGOING_TRAVEL_START_DATE_LOCKED` | 진행 중인 여행의 기존 시작일을 변경함 |
| `400` | `PAST_TRAVEL_END_DATE` | 진행 중인 여행의 종료일을 오늘보다 과거로 변경함 |
| `400` | `COMPLETED_TRAVEL_DATES_LOCKED` | 종료된 여행 플랜의 날짜를 변경함 |
| `400` | `SELF_PLAN_REPORT_NOT_ALLOWED` | 본인 소유 공개 플랜을 신고함 |
| `403` | `PLAN_ACCESS_DENIED` | 현재 회원에게 플랜 조회 또는 편집 권한이 없음 |
| `404` | `REGION_NOT_FOUND` | 활성화된 시·도 지역코드가 존재하지 않음 |
| `404` | `PLAN_NOT_FOUND` | 플랜이 없거나 삭제 상태여서 조회할 수 없음 |
| `404` | `PLAN_DAY_NOT_FOUND` | 일차가 없거나 대상 플랜에 속하지 않음 |
| `404` | `SCHEDULE_ITEM_NOT_FOUND` | 일정 항목이 없거나 대상 일차에 속하지 않음 |
| `400` | `INVALID_SCHEDULE_ORDER` | 정렬 목록이 현재 시간대의 항목과 일치하지 않음 |
| `409` | `PLAN_VERSION_CONFLICT` | 플랜 Metadata Version 불일치 |
| `409` | `PLAN_SCHEDULE_REQUIRED` | 일정이 없는 플랜을 제작 완료 상태로 변경함 |
| `409` | `PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED` | 날짜 변경으로 일정이 포함된 DAY가 제외되지만 삭제 확인이 없음 |
| `409` | `SCHEDULE_VERSION_CONFLICT` | 일차 일정 Version 불일치 |
| `409` | `ITEM_VERSION_CONFLICT` | 일정 항목 Version 불일치 |
| `409` | `DUPLICATE_OPERATION` | 이미 처리된 `operationId`를 다른 Payload로 재요청 |
| `409` | `SCHEDULE_ITEM_ALREADY_EXISTS` | 같은 일차·시간대에 동일 장소가 존재 |
| `409` | `SCHEDULE_ITEM_LIMIT_EXCEEDED` | 시간대별 일정 100개 제한 초과 |
| `409` | `REPORT_ALREADY_EXISTS` | 같은 회원이 같은 공개 플랜을 중복 신고함 |
| `400` | `TARGET_SCHEDULE_VERSION_REQUIRED` | DAY 간 이동 요청에 대상 DAY 일정 Version이 없음 |

Version 및 Operation 오류는 자동 저장 API에서 사용합니다. 동일한 `operationId`와 동일한 Payload 재요청은 오류가 아니라 멱등 성공으로 처리합니다.

## 플랜 초대 오류

| HTTP Status | Code | 사용 조건 |
| --- | --- | --- |
| `404` | `INVITATION_NOT_FOUND` | token 형식이 잘못됐거나 초대가 존재하지 않음 |
| `409` | `INVITATION_NOT_AVAILABLE` | 초대가 취소·거절됐거나 다른 회원이 처리해 사용할 수 없음 |
| `409` | `INVITATION_SELF_ACCEPTANCE_NOT_ALLOWED` | 초대한 회원이 자기 초대를 수락함 |
| `410` | `INVITATION_EXPIRED` | 초대 유효시간 24시간 경과 |

## 장소 검색 오류

| HTTP Status | Code | 사용 조건 |
| --- | --- | --- |
| `502` | `TOUR_API_AUTHENTICATION_FAILED` | TourAPI가 인증키를 거부함 |
| `502` | `TOUR_API_UNAVAILABLE` | TourAPI HTTP 또는 연결 오류 |
| `502` | `TOUR_API_INVALID_RESPONSE` | TourAPI 응답 형식 오류 |
| `503` | `TOUR_API_NOT_CONFIGURED` | Backend에 TourAPI 인증키가 설정되지 않음 |
| `504` | `TOUR_API_TIMEOUT` | TourAPI 연결 또는 응답 시간 초과 |
| `502` | `KAKAO_LOCAL_AUTHENTICATION_FAILED` | Kakao Local이 REST API 키를 거부함 |
| `502` | `KAKAO_LOCAL_UNAVAILABLE` | Kakao Local HTTP 또는 연결 오류 |
| `502` | `KAKAO_LOCAL_INVALID_RESPONSE` | Kakao Local 응답 형식 오류 |
| `503` | `KAKAO_LOCAL_NOT_CONFIGURED` | Backend에 Kakao REST API 키가 설정되지 않음 |
| `504` | `KAKAO_LOCAL_TIMEOUT` | Kakao Local 연결 또는 응답 시간 초과 |

## 구현 반영 사항

현재 `GlobalExceptionHandler`는 다음 오류를 처리합니다.

- `HttpMessageNotReadableException` → `MALFORMED_JSON`
- `MethodArgumentTypeMismatchException`, `HandlerMethodValidationException` → `INVALID_REQUEST_PARAMETER`
- `NoResourceFoundException` → `RESOURCE_NOT_FOUND`
- `BusinessException` → 기능별 안정적인 오류 코드
- 그 외 처리되지 않은 예외 → `INTERNAL_SERVER_ERROR`

Spring Security Filter 단계의 인증·인가 오류도 같은 `ErrorResponse` 형식으로 반환합니다.

Business Error는 `BusinessException(HttpStatus, code, message)`로 전달하고 Field Error가 필요한 경우에만 별도 Validation 처리로 확장합니다.
