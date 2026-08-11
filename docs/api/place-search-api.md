# 장소 검색 API

플랜 제작 페이지에서 TourAPI 관광정보를 검색하고 일정 항목에 저장할 수 있는 내부 계약입니다. Frontend는 TourAPI 응답을 직접 사용하지 않고 이 API가 정규화한 장소 구조를 사용합니다.

## 장소 검색

```http
GET /api/places/search?keyword=한강&regionCode=1&page=1&size=10
```

### Query Parameter

| 이름 | 필수 | 기본값 | 검증 및 의미 |
| --- | --- | --- | --- |
| `keyword` | 예 | 없음 | 공백 제외 1자 이상, 최대 100자 |
| `regionCode` | 아니요 | 없음 | TourAPI 지역코드, 1~2자리 양의 정수 |
| `page` | 아니요 | `1` | 1 이상 |
| `size` | 아니요 | `10` | 1~20 |

`keyword`의 앞뒤 공백은 Backend가 제거합니다. `regionCode`가 없으면 전국을 대상으로 검색합니다.

### 성공 응답

```json
{
  "success": true,
  "data": {
    "places": [
      {
        "placeProvider": "TOUR_API",
        "externalPlaceId": "1001",
        "placeName": "여의도 한강공원",
        "placeType": "ATTRACTION",
        "categoryName": "관광지",
        "address": "서울 영등포구 여의동로 330",
        "latitude": 37.5284,
        "longitude": 126.934,
        "imageUrl": "https://example.com/image.jpg"
      }
    ],
    "page": 1,
    "size": 10,
    "totalCount": 1,
    "hasNext": false
  }
}
```

- `externalPlaceId`는 TourAPI `contentid`이며 문자열로 반환합니다.
- `placeType`은 TourAPI `contenttypeid`를 서버 내부 유형으로 정규화한 값입니다. 썸네일 우선순위는 이 값을 사용합니다.
- `latitude`, `longitude`, `address`, `imageUrl`은 원본 관광정보에 값이 없으면 `null`입니다.
- 검색 결과는 `PLACE_MASTER`에 서버 권위 데이터로 저장됩니다. 일정 추가 시에는 `placeProvider + externalPlaceId`로 이 데이터를 다시 조회해 Snapshot을 생성합니다.
- `imageUrl`은 절대 `http` 또는 `https` URL이고 호스트가 있으며 사용자정보를 포함하지 않을 때만 저장합니다. 그 외에는 `null`로 정규화합니다.

### 외부 호출

Backend는 국문 관광정보 서비스의 `searchKeyword2`를 사용합니다. `serviceKey`, `MobileOS=ETC`, `MobileApp`, `_type=json`, `arrange=A`는 서버에서 설정하고 사용자 입력으로 받지 않습니다.

## 오류

| HTTP Status | Code | 조건 |
| --- | --- | --- |
| `400` | `INVALID_REQUEST_PARAMETER` | Query Parameter 형식 또는 범위 오류 |
| `502` | `TOUR_API_AUTHENTICATION_FAILED` | TourAPI 인증키 거부 |
| `502` | `TOUR_API_UNAVAILABLE` | TourAPI HTTP 또는 연결 오류 |
| `502` | `TOUR_API_INVALID_RESPONSE` | TourAPI 응답 형식 오류 |
| `503` | `TOUR_API_NOT_CONFIGURED` | Backend에 인증키가 설정되지 않음 |
| `504` | `TOUR_API_TIMEOUT` | TourAPI 연결 또는 응답 시간 초과 |

인증키, 공급자 원문 오류, 내부 예외 메시지는 API 응답에 노출하지 않습니다.

## 참고

- [공공데이터포털 국문 관광정보 서비스](https://www.data.go.kr/data/15101578/openapi.do)
