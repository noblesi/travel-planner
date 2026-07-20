# Travel Planner

여행 일정과 방문 장소 및 동선을 관리하는 JSP 기반 웹 애플리케이션입니다.

## 기술 구성

- Java 21
- Spring Boot 4 / Spring MVC
- JSP
- MyBatis 3
- Oracle Database
- Gradle
- Linux

## 디렉터리

```text
travel-planner/
├── backend/
│   ├── src/main/java/                 # 애플리케이션 및 공통 백엔드 코드
│   ├── src/main/resources/mapper/     # MyBatis XML Mapper
│   ├── src/main/resources/static/     # CSS, JavaScript, 이미지
│   └── src/main/webapp/WEB-INF/jsp/   # JSP 화면과 공통 조각
├── scripts/                           # Linux용 빌드 및 실행 스크립트
└── CONTRIBUTING.md                    # 팀 개발 규칙
```

상세한 패키지 구조, API 응답 규칙 및 브랜치 작업 방법은 [CONTRIBUTING.md](CONTRIBUTING.md)를 확인합니다.

## Eclipse에서 가져오기

1. **File → Import → Gradle → Existing Gradle Project**를 선택합니다.
2. 프로젝트 경로로 `backend` 폴더를 지정합니다.
3. Gradle JVM을 Java 21 이상으로 설정합니다.
4. `TravelPlannerApiApplication`을 **Spring Boot App**으로 실행합니다.

## Oracle 환경 변수

루트의 `.env.example`을 참고하되 실제 비밀번호가 포함된 `.env`는 커밋하지 않습니다.

```bash
export ORACLE_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export ORACLE_USERNAME='travel_planner'
export ORACLE_PASSWORD='change-me'
```

## 개발 실행

```bash
cd backend
./gradlew bootRun
```

애플리케이션은 기본적으로 `http://localhost:8080`에서 실행됩니다.

Windows에서는 `gradlew.bat bootRun`, macOS/Linux에서는 `./gradlew bootRun`을 사용합니다.

## 테스트

```bash
cd backend
./gradlew test
```

공통 API 코드는 다음 형식을 제공합니다.

- `ApiResponse<T>`: 정상 API 응답
- `BusinessException`: 예상 가능한 비즈니스 오류
- `GlobalExceptionHandler`: 비즈니스·Validation·서버 오류의 공통 JSON 변환

## Linux 빌드

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```

빌드 결과는 `backend/build/libs/travel-planner.war`에 생성됩니다.

## 향후 Vue 전환

화면 Controller와 `/api/**` REST API를 분리해 구현합니다. 이후 Vue 도입 시 JSP 화면을 기능 단위로 교체하고 기존 Service 및 MyBatis 계층은 그대로 재사용할 수 있습니다.
