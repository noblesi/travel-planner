# Travel Planner 개발 가이드

## 작업 시작

1. 최신 `dev`를 받습니다.
2. 담당 기능 브랜치를 `dev`에서 생성합니다.
3. 하나의 브랜치에는 하나의 기능 또는 수정만 포함합니다.

```bash
git switch dev
git pull origin dev
git switch -c feature/<기능명>
```

브랜치 이름은 `feature/<기능명>`, `fix/<기능명>`, `docs/<주제>` 형식을 사용합니다.

## 백엔드 구조

기능별 클래스는 아래 역할을 지킵니다.

```text
controller/  HTTP 요청·응답과 입력 검증
service/     비즈니스 규칙과 트랜잭션
mapper/      MyBatis Mapper 인터페이스
domain/      DB 엔티티와 핵심 도메인 객체
dto/         요청·응답 전용 객체
common/      여러 기능에서 함께 사용하는 코드만 배치
```

- Controller에서 SQL 또는 비즈니스 규칙을 직접 처리하지 않습니다.
- 요청 DTO에는 Jakarta Validation 애너테이션을 사용합니다.
- API 성공 응답은 `ApiResponse.success(data)`로 감쌉니다.
- 의도된 오류는 `BusinessException`을 사용하고 안정적인 영문 오류 코드를 지정합니다.
- MyBatis XML은 `src/main/resources/mapper/<기능>/`에 둡니다.
- JSP 공통 영역은 `WEB-INF/jsp/common/`의 조각 파일을 include하여 사용합니다.
- 페이지별 스타일은 공통 `layout.css`를 변경하기보다 별도 CSS 파일에 작성합니다.

## API 응답 규칙

성공 응답 예시:

```json
{
  "success": true,
  "data": {}
}
```

오류 응답 예시:

```json
{
  "success": false,
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다.",
  "errors": [],
  "timestamp": "2026-07-20T00:00:00Z",
  "path": "/api/trips/1"
}
```

## 제출 전 확인

```bash
cd backend
./gradlew test
```

- 비밀번호, API 키, 개인 `.env` 파일을 커밋하지 않습니다.
- 테스트가 통과하는지 확인합니다.
- 포맷 변경만 섞인 파일이나 담당 범위 밖 변경이 없는지 확인합니다.
- PR 대상 브랜치는 `dev`로 지정하고 변경 이유와 테스트 결과를 작성합니다.
