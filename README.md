# Travel Planner

여행 일정을 계획하고 방문 장소와 동선을 관리하는 웹 애플리케이션입니다.

## 기술 구성

- Backend: Java 21, Spring Boot 4, Spring Framework, MyBatis 3
- Frontend: Vue 3, TypeScript, Vite
- Database: Oracle Database
- Target: Linux

## 디렉터리

```text
travel-planner/
├── backend/    # Spring Boot REST API
├── frontend/   # Vue 웹 애플리케이션
└── scripts/    # Linux용 빌드 및 실행 스크립트
```

## 환경 변수

백엔드를 실행하기 전에 Oracle 접속 정보를 설정합니다.

```bash
export ORACLE_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export ORACLE_USERNAME='travel_planner'
export ORACLE_PASSWORD='change-me'
```

## 개발 실행

### Eclipse에서 백엔드 가져오기

1. Eclipse에서 **File → Import → Gradle → Existing Gradle Project**를 선택합니다.
2. 프로젝트 경로로 `backend` 폴더를 지정합니다.
3. Gradle JVM은 Java 21 이상으로 설정합니다.
4. `TravelPlannerApiApplication`을 **Spring Boot App**으로 실행합니다.

Vue 프런트엔드는 Eclipse의 터미널에서 다음 명령으로 실행할 수 있습니다.

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend (새 터미널)
cd frontend
pnpm install
pnpm dev
```

프런트엔드는 기본적으로 `http://localhost:5173`, 백엔드는 `http://localhost:8080`에서 실행됩니다.

## 빌드

Linux에서 다음 명령을 실행합니다.

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```
