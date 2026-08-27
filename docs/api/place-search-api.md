# 장소 검색 API

플랜 제작 페이지에서 Kakao Local 장소를 검색하고 TourAPI 관광정보와 사진을 보강한 뒤 일정 항목에 저장할 수 있는 내부 계약입니다. Frontend는 외부 응답을 직접 사용하지 않고 이 API가 정규화한 장소 구조를 사용합니다.

## 장소 검색

```http
GET /api/places/search?keyword=한강&regionCode=1&category=관광지&page=1&size=10
```

로그인 Session이 필요한 플랜 제작용 API입니다. 비로그인 요청은 외부 TourAPI를 호출하기 전에 `401 CURRENT_MEMBER_NOT_AVAILABLE`로 차단합니다. 조회 요청이므로 CSRF Token은 필요하지 않습니다.

### Query Parameter

| 이름 | 필수 | 기본값 | 검증 및 의미 |
| --- | --- | --- | --- |
| `keyword` | 예 | 없음 | 공백 제외 1자 이상, 최대 100자 |
| `regionCode` | 아니요 | 없음 | 여행 지역코드, 1~2자리 양의 정수 |
| `category` | 아니요 | 없음 | 응답의 `categories` 중 선택한 카테고리, 최대 100자 |
| `page` | 아니요 | `1` | 1~45 |
| `size` | 아니요 | `10` | 1~15 |

`keyword`와 `category`의 앞뒤 공백은 Backend가 제거합니다. `regionCode`가 있으면 활성 시도 지역명을 검색어 앞에 결합해 정확도순으로 조회하고, 없으면 전국을 대상으로 검색합니다. 카테고리는 여행 관련 Kakao 결과와 TourAPI 보완 결과 전체를 분류한 뒤 적용하므로 현재 응답 페이지 밖의 결과도 필터 대상에 포함됩니다.

### 성공 응답

```json
{
  "success": true,
  "data": {
    "places": [
      {
        "placeProvider": "KAKAO",
        "externalPlaceId": "1001",
        "placeName": "여의도 한강공원",
        "placeType": "ATTRACTION",
        "categoryName": "관광지",
        "address": "서울 영등포구 여의동로 330",
        "latitude": 37.5284,
        "longitude": 126.934,
        "imageUrl": "https://example.com/hanriver.jpg"
      }
    ],
    "page": 1,
    "size": 10,
    "totalCount": 1,
    "hasNext": false,
    "categories": ["관광지", "음식점"]
  }
}
```

- `placeProvider`는 Kakao 결과 또는 TourAPI 보완 결과에 따라 `KAKAO`, `TOUR_API` 중 하나입니다.
- `externalPlaceId`는 해당 제공자의 장소 ID이며 문자열로 반환합니다.
- `placeType`은 각 제공자의 카테고리를 서버 내부 유형으로 정규화한 값입니다. 썸네일 우선순위는 이 값을 사용합니다.
- `latitude`, `longitude`, `address`는 원본 장소정보에 값이 없으면 `null`입니다.
- Kakao Local 키워드 검색은 이미지를 제공하지 않습니다. 이름과 좌표가 일치하는 TourAPI 장소가 있으면 Kakao 결과에 관광 이미지를 보강하고, 일치하지 않은 TourAPI 관광 콘텐츠는 별도 `TOUR_API` 결과로 합칩니다.
- 검색 결과는 `PLACE_MASTER`에 서버 권위 데이터로 저장됩니다. 일정 추가 시에는 `placeProvider + externalPlaceId`로 이 데이터를 다시 조회해 Snapshot을 생성합니다.
- `categories`는 현재 페이지가 아니라 여행 관련 Kakao 결과와 TourAPI 보완 결과 전체에서 중복을 제거한 목록입니다. `category`가 있으면 먼저 전체 결과를 필터링한 뒤 `page`와 `size`를 적용합니다.
- `imageUrl`은 절대 `http` 또는 `https` URL이고 호스트가 있으며 사용자정보를 포함하지 않을 때만 저장합니다. 그 외에는 `null`로 정규화합니다.

### 외부 호출

Backend는 Kakao Local의 `GET /v2/local/search/keyword.json`을 기본 검색으로 사용합니다. REST API 키와 `sort=accuracy`는 서버에서 설정하고 사용자 입력으로 받지 않습니다. 지역이 정해진 플랜에서는 `지역명 + 검색어` 형태로 조회해 동일 키워드의 타 지역 결과를 줄입니다. 카테고리 필터가 페이지 경계를 넘어서 동작하도록 Kakao가 노출하는 최대 45건을 15건씩 최대 3페이지 조회합니다.

Kakao 결과에서는 관광지, 문화시설, 숙박, 음식점, 카페, 쇼핑, 레포츠와 공항·터미널·항구 같은 여행 거점만 유지합니다. 주차장, 입출구, 주유소, 병원, 약국, 부동산 등 `TOURIST_INFORMATION`으로 분류되는 일반 생활·부대시설과 스포츠·아웃도어 용품점은 기본 결과에서 제외합니다.

Kakao 검색이 성공하면 같은 원본 검색어와 지역코드로 TourAPI `searchKeyword2`를 한 번 호출합니다. Kakao 장소명과 TourAPI 장소명을 정규화한 뒤, 동일 이름은 최대 1km, 부분 일치 이름은 최대 300m 안에서만 같은 장소로 판단합니다. 매칭된 TourAPI 이미지와 부족한 관광 분류는 Kakao 결과에 보강하고, 매칭되지 않은 TourAPI 관광 콘텐츠는 `TOUR_API` 제공자 결과로 중복 없이 합칩니다. 정렬은 검색어와 장소명이 정확히 일치하는 결과, 검색어 의도와 카테고리가 일치하는 결과, 사진이 있는 결과, Kakao 결과 순으로 가중치를 적용하며 동점이면 제공자의 검색 순서를 유지합니다. TourAPI가 설정되지 않았거나 호출에 실패하면 보강만 생략하고 여행 관련 Kakao 결과는 정상 반환합니다.

## 오류

| HTTP Status | Code | 조건 |
| --- | --- | --- |
| `401` | `CURRENT_MEMBER_NOT_AVAILABLE` | 로그인 Session이 없거나 만료됨 |
| `400` | `INVALID_REQUEST_PARAMETER` | Query Parameter 형식 또는 범위 오류 |
| `502` | `KAKAO_LOCAL_AUTHENTICATION_FAILED` | Kakao REST API 인증키 거부 |
| `502` | `KAKAO_LOCAL_UNAVAILABLE` | Kakao Local HTTP 또는 연결 오류 |
| `502` | `KAKAO_LOCAL_INVALID_RESPONSE` | Kakao Local 응답 형식 오류 |
| `503` | `KAKAO_LOCAL_NOT_CONFIGURED` | Backend에 Kakao REST API 키가 설정되지 않음 |
| `504` | `KAKAO_LOCAL_TIMEOUT` | Kakao Local 연결 또는 응답 시간 초과 |

인증키, 공급자 원문 오류, 내부 예외 메시지는 API 응답에 노출하지 않습니다.

## 참고

- [Kakao Local 키워드 장소 검색](https://developers.kakao.com/docs/ko/local/dev-guide#search-by-keyword)
