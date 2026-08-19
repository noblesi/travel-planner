# WithTrip 개발 가이드

## 작업 시작

1. 최신 `dev`를 받습니다.
2. 담당 기능 브랜치를 `dev`에서 생성합니다.
3. 하나의 브랜치에는 하나의 기능 또는 수정만 포함합니다.

```bash
git switch dev
git pull origin dev
git switch -c feature/<기능명>
```

브랜치는 `feature/<기능명>`, `fix/<기능명>`, `docs/<주제>`, `refactor/<대상>` 형식을 사용합니다.

## 기능 단위 작업 순서

1. UI 설계, API 명세, ERD에서 담당 범위를 확인합니다.
2. 요청·응답 데이터와 예외·권한 조건을 먼저 정합니다.
3. 백엔드 API와 프론트엔드 API 모듈의 계약을 맞춥니다.
4. 정상·로딩·빈 결과·오류 화면을 구현합니다.
5. 프론트엔드와 백엔드 테스트를 실행합니다.
6. 관련 문서와 함께 Pull Request를 생성합니다.

## 백엔드 구조

```text
controller/  HTTP 요청·응답과 입력 검증
service/     비즈니스 규칙과 트랜잭션
mapper/      MyBatis Mapper 인터페이스
domain/      DB 엔티티와 핵심 도메인 객체
dto/         요청·응답 전용 객체
common/      여러 기능에서 함께 사용하는 코드
```

- 프론트엔드용 URL은 `/api/**` 아래에 둡니다.
- Controller에서 SQL 또는 비즈니스 규칙을 직접 처리하지 않습니다.
- 요청 DTO에는 Jakarta Validation을 사용합니다.
- 성공 응답은 `ApiResponse.success(data)`로 감쌉니다.
- 의도된 오류는 `BusinessException`과 안정적인 영문 오류 코드를 사용합니다.
- MyBatis XML은 `src/main/resources/mapper/<기능>/`에 둡니다.
- Mapper XML namespace와 Mapper 인터페이스 경로를 일치시킵니다.

## 프론트엔드 구조

```text
api/         Axios 인스턴스와 기능별 API 함수
assets/      전역 스타일과 정적 자원
components/  여러 화면에서 재사용하는 UI
layouts/     Header·Footer를 포함한 공통 레이아웃
router/      URL과 화면 연결
stores/      여러 화면이 공유하는 Pinia 상태
views/       라우트 단위 화면
```

- 페이지에서 Axios를 직접 호출하지 않고 `src/api`의 기능별 함수로 분리합니다.
- 단일 컴포넌트에서만 쓰는 상태는 Pinia에 넣지 않습니다.
- 컴포넌트 입력은 props, 결과 통지는 emit을 우선합니다.
- 공통 CSS를 수정할 때 다른 화면에 미치는 영향을 확인합니다.
- 비동기 데이터는 로딩·성공·빈 결과·실패 상태를 모두 처리합니다.
- 버튼과 링크는 키보드 접근과 의미에 맞는 HTML 요소를 사용합니다.

### 공통 레이아웃과 UI Component

- 사용자 route는 특별한 이유가 없으면 `DefaultLayout`을 사용합니다. 관리자 화면은 Vue route를 추가하지 않고 Backend의 `/admin/**` Spring MVC + Thymeleaf 구조에서 구현합니다.
- 새 버튼·입력·Modal·비동기 상태 UI를 만들기 전에 `src/components/ui`의 기존 Component로 조합할 수 있는지 확인합니다.
- brand 색상, surface, border, text, layout 너비는 `src/assets/main.css`의 CSS variable을 사용합니다.
- `BaseModal`을 사용하는 화면은 `close` event로 open state를 소유하며 dialog 내부에 별도 overlay나 document keydown listener를 추가하지 않습니다.
- 비동기 성공·실패 알림이 route 밖에서도 보여야 하면 `useToastStore()`를 사용합니다. 화면별 Toast container는 만들지 않습니다.
- 공통 UI를 변경하면 해당 Component test와 실제 적용 화면의 회귀 test를 함께 수정합니다.
- 반응형 확인 기준은 Desktop 1280px와 Mobile 390px이며 keyboard focus와 `prefers-reduced-motion`도 확인합니다.

전체 API와 사용 예시는 [`docs/frontend/common-layout-ui.md`](docs/frontend/common-layout-ui.md)를 확인합니다.

## API 응답 규칙

성공 응답:

```json
{
  "success": true,
  "data": {}
}
```

오류 응답:

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

- 날짜·시간 형식, 페이지 번호 기준과 null 가능 여부를 API 명세에 적습니다.
- 프론트엔드에는 사용자용 메시지를 표시하고 서버 상세 오류나 스택 추적은 노출하지 않습니다.
- 수정·삭제 API는 로그인 여부와 대상 데이터 소유권을 확인합니다.

## 커밋 메시지

```text
feat: 여행 일정 등록 기능 추가
fix: 일정 검색 조건 오류 수정
refactor: 일정 조회 로직 서비스로 분리
test: 일정 등록 API 검증 테스트 추가
docs: API 명세 보완
chore: Vue 의존성 설정 변경
```

- 한 커밋에는 하나의 목적만 담습니다.
- `수정`, `작업`, `최종`처럼 내용을 알 수 없는 메시지는 사용하지 않습니다.
- 다른 팀원의 변경이나 포맷 변경을 담당 기능 커밋에 섞지 않습니다.

## 제출 전 확인

프론트엔드:

```bash
cd frontend
npm run lint
npm run test:unit -- --run
npm run build
```

백엔드:

```bash
cd backend
./gradlew test
```

- 비밀번호, API 키, 개인 `.env` 파일을 커밋하지 않습니다.
- 두 영역의 테스트와 빌드 결과를 확인합니다.
- 담당 범위 밖 파일이 포함되지 않았는지 `git diff --staged`로 확인합니다.
- API·DB·화면 구조 변경 시 관련 문서도 수정합니다.
- PR 대상 브랜치는 `dev`로 지정하고 변경 이유와 확인 방법을 작성합니다.

## Pull Request 본문

```markdown
## 작업 내용
- 구현하거나 수정한 내용

## 확인 방법
1. 실행 경로 또는 API
2. 테스트 순서

## 관련 화면/API/DB
- 변경된 화면, 엔드포인트, 테이블

## 체크리스트
- [ ] 프론트엔드 린트·테스트·빌드 통과
- [ ] 백엔드 테스트 통과
- [ ] 비밀값 미포함
- [ ] 관련 문서 수정
```
