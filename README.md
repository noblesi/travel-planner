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
│   ├── src/main/java/                 # Controller, Service, Mapper, Domain
│   ├── src/main/resources/mapper/     # MyBatis XML Mapper
│   └── src/main/webapp/WEB-INF/jsp/   # JSP 화면
└── scripts/                           # Linux용 빌드 및 실행 스크립트
```

## Eclipse에서 가져오기

1. **File → Import → Gradle → Existing Gradle Project**를 선택합니다.
2. 프로젝트 경로로 `backend` 폴더를 지정합니다.
3. Gradle JVM을 Java 21 이상으로 설정합니다.
4. `TravelPlannerApiApplication`을 **Spring Boot App**으로 실행합니다.

## Oracle 환경 변수

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

## Linux 빌드

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```

빌드 결과는 `backend/build/libs/travel-planner.war`에 생성됩니다.

## 향후 Vue 전환

화면 Controller와 `/api/**` REST API를 분리해 구현합니다. 이후 Vue 도입 시 JSP 화면을 기능 단위로 교체하고 기존 Service 및 MyBatis 계층은 그대로 재사용할 수 있습니다.
