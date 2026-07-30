# API 계약

Frontend와 Backend가 함께 사용하는 HTTP 계약의 기준 문서입니다. 구현이 계약과 다르면 구현을 임의로 해석하지 않고 계약 변경 여부를 먼저 확인합니다.

## 문서

- [여행 플랜 1차 API](travel-plan-api.md): 지역 조회, 플랜 생성, 제작 페이지 초기 조회
- [외부 API 설정](external-api-setup.md): TourAPI·Kakao 환경변수, 키 보관, JavaScript SDK 도메인
- [오류 코드](error-codes.md): 공통 오류 응답 형식과 안정적인 오류 코드

## 공통 규칙

- Base path는 `/api`입니다.
- JSON Property는 `camelCase`를 사용합니다.
- 요청과 응답의 `Content-Type`은 `application/json`입니다.
- 날짜는 `YYYY-MM-DD` 형식입니다.
- 일시는 UTC ISO 8601 형식입니다. 예: `2026-07-23T13:30:00Z`
- Oracle `NUMBER(19)` 식별자는 JavaScript 정밀도 손실을 방지하기 위해 JSON 문자열로 반환합니다.
- Version, 순서, 개수처럼 `NUMBER(10)` 이하인 값은 JSON 정수로 반환합니다.
- Enum은 영문 대문자 문자열을 사용합니다.
- 현재 회원 식별자는 Request Body나 Query Parameter로 받지 않고 `CurrentMemberProvider`에서 가져옵니다.

## 성공 응답

모든 성공 응답은 기존 `ApiResponse<T>` 형식을 사용합니다.

```json
{
  "success": true,
  "data": {}
}
```

목록도 `data` 내부에 이름 있는 Property로 반환합니다. 향후 Pagination이나 Metadata를 추가할 때 응답 최상위 구조를 변경하지 않기 위해서입니다.

## 변경 절차

API 계약을 변경할 때는 다음 항목을 함께 수정합니다.

1. Request와 Response Example
2. Validation과 Business Rule
3. HTTP Status와 Error Code
4. Backend DTO와 Controller Test
5. Frontend API Module과 Mock Data
6. 관련 인수인계 문서

기존 Property를 삭제하거나 의미를 변경하는 수정은 Breaking Change로 취급합니다. 새 Optional Property 추가는 하위 호환 변경으로 취급합니다.
