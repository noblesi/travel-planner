# WithTrip (Travel Planner)

여행 일정과 방문 장소, 이동 동선을 관리하는 4인 팀 프로젝트입니다. 현재는 **Spring MVC + JSP 기반 웹 애플리케이션**으로 개발하며, 화면 Controller와 `/api/**` REST API를 분리해 이후 Vue 화면으로 단계적으로 전환할 수 있게 구성합니다.

> 서비스명 `WithTrip`은 임시명이며, 저장소와 빌드 산출물 이름은 현재 `travel-planner`를 사용합니다.

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 개발 인원 | 4명 |
| 아키텍처 | Model 2, MVC 패턴 |
| Java | Java 21 |
| 백엔드 | Spring Boot 4.0.7, Spring MVC |
| 화면 | JSP (향후 Vue 단계적 전환) |
| 데이터 접근 | MyBatis 3 |
| 데이터베이스 | Oracle Database |
| 빌드 | Gradle Wrapper |
| 배포 | Linux, WAR |
| 문자 인코딩 | UTF-8 |
| 기본 포트 | 8080 |

세부 코딩 규칙, 패키지별 책임, API 응답 형식 및 PR 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 확인합니다.

## 1. 개발 전 필수 준비

### 필수 프로그램

| 프로그램 | 기준 | 확인 방법 |
| --- | --- | --- |
| Git | 팀 공통 최신 안정 버전 | `git --version` |
| JDK | 21 | `java -version`, `javac -version` |
| Oracle Database | 팀에서 지정한 버전 | DB 접속 테스트 |
| IDE | Eclipse 또는 IntelliJ IDEA | Gradle JVM 21 확인 |
| API 테스트 도구 | Postman 등 | 로컬 API 호출 |

Gradle은 별도로 설치하지 않습니다. 저장소에 포함된 Gradle Wrapper(`gradlew`, `gradlew.bat`)를 사용합니다.

### IDE 공통 설정

- 파일 인코딩: `UTF-8`
- 줄바꿈: `LF`
- Java 컴파일러 및 Gradle JVM: `Java 21`
- Java 들여쓰기: 4칸
- 저장 시 불필요한 import 정리
- `.idea`, `.settings`, `.project`, `.classpath` 등 개인 IDE 설정은 커밋하지 않기

## 2. 저장소 내려받기

```bash
git clone -b dev https://github.com/noblesi/travel-planner.git
cd travel-planner
git status
```

기능 개발을 시작하기 전에 `dev`가 최신 상태이고 프로젝트가 정상 실행되는지 먼저 확인합니다.

```bash
git switch dev
git pull origin dev
```

## 3. 디렉터리 구조

```text
travel-planner/
├── backend/
│   ├── src/main/java/                 # 애플리케이션 및 백엔드 코드
│   ├── src/main/resources/mapper/     # MyBatis XML Mapper
│   ├── src/main/resources/static/     # CSS, JavaScript, 이미지
│   ├── src/main/resources/application.yml
│   ├── src/main/webapp/WEB-INF/jsp/   # JSP 화면과 공통 조각
│   ├── build.gradle
│   ├── gradlew
│   └── gradlew.bat
├── scripts/                           # Linux 빌드 스크립트
├── .env.example                       # 환경변수 예시
├── CONTRIBUTING.md                    # 팀 개발 규칙
└── README.md
```

백엔드의 기능별 패키지는 아래 책임을 지킵니다.

| 패키지 | 책임 |
| --- | --- |
| `controller` | HTTP 요청·응답과 입력 검증 |
| `service` | 비즈니스 규칙과 트랜잭션 |
| `mapper` | MyBatis Mapper 인터페이스 |
| `domain` | DB 엔티티와 핵심 도메인 객체 |
| `dto` | 요청·응답 전용 객체 |
| `common` | 여러 기능에서 공통으로 사용하는 코드 |

## 4. Eclipse에서 가져오기

1. **File → Import → Gradle → Existing Gradle Project**를 선택합니다.
2. 프로젝트 경로로 저장소 루트가 아닌 `backend` 폴더를 지정합니다.
3. Gradle JVM과 프로젝트 JRE를 Java 21로 지정합니다.
4. Gradle 동기화가 끝날 때까지 기다립니다.
5. `TravelPlannerApiApplication`을 **Spring Boot App**으로 실행합니다.

한글이 깨지면 다음 항목을 모두 UTF-8로 맞춘 뒤 파일을 다시 엽니다.

- **Window → Preferences → General → Workspace**
- **Window → Preferences → Web → JSP Files**
- **Project → Properties → Resource**

## 5. Oracle 및 환경변수 설정

애플리케이션은 다음 환경변수를 사용합니다.

| 환경변수 | 기본값 또는 예시 | 용도 |
| --- | --- | --- |
| `ORACLE_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | Oracle 접속 URL |
| `ORACLE_USERNAME` | `travel_planner` | DB 계정 |
| `ORACLE_PASSWORD` | `change-me` | DB 비밀번호 |
| `SERVER_PORT` | `8080` | 로컬 서버 포트 |

루트의 `.env.example`은 값의 형식을 확인하기 위한 예시입니다. 실제 비밀번호가 들어간 `.env` 파일은 커밋하지 않습니다.

### macOS/Linux

```bash
export ORACLE_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
export ORACLE_USERNAME='travel_planner'
export ORACLE_PASSWORD='change-me'
export SERVER_PORT='8080'
```

### Windows PowerShell

```powershell
$env:ORACLE_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:ORACLE_USERNAME='travel_planner'
$env:ORACLE_PASSWORD='change-me'
$env:SERVER_PORT='8080'
```

환경변수 설정 후 같은 터미널에서 애플리케이션을 실행합니다. 실제 운영 DB 정보나 외부 API 키는 README, 소스 코드, 메신저 캡처에 남기지 않습니다.

### DB 준비 체크

- [ ] Oracle Listener와 DB 서비스 실행
- [ ] 팀 공통 서비스명 확인(예: `FREEPDB1`)
- [ ] 로컬 개발 계정 생성 및 접속 확인
- [ ] 팀 공통 DDL과 초기 데이터 실행
- [ ] 애플리케이션에서 간단한 조회 확인

공유된 SQL을 변경할 때는 담당자와 실행 순서를 합의하고, `DROP`이나 전체 `DELETE` 실행 전 대상 DB를 반드시 확인합니다.

## 6. 개발 실행

### Windows

```bat
cd backend
gradlew.bat bootRun
```

### macOS/Linux

```bash
cd backend
./gradlew bootRun
```

정상 실행 후 [http://localhost:8080](http://localhost:8080)에 접속합니다. `SERVER_PORT`를 변경했다면 변경한 포트를 사용합니다.

권장 실행 순서:

1. Oracle 실행
2. 환경변수 설정
3. Spring Boot 실행
4. 브라우저에서 메인 화면 확인
5. 담당 기능의 화면과 API 호출 확인

## 7. 테스트와 빌드

### 테스트

Windows:

```bat
cd backend
gradlew.bat test
```

macOS/Linux:

```bash
cd backend
./gradlew test
```

### Linux 배포용 WAR 빌드

```bash
chmod +x scripts/*.sh
./scripts/build.sh
```

빌드 스크립트는 `clean`, `test`, `bootWar`를 차례대로 실행합니다. 결과 파일은 다음 위치에 생성됩니다.

```text
backend/build/libs/travel-planner.war
```

## 8. Git 작업 방법

직접 `dev`에서 기능을 개발하지 않습니다. 최신 `dev`에서 담당 기능 브랜치를 생성합니다.

```bash
git switch dev
git pull origin dev
git switch -c feature/trip-create
```

브랜치 이름:

| 형식 | 용도 | 예시 |
| --- | --- | --- |
| `feature/<기능명>` | 기능 개발 | `feature/trip-create` |
| `fix/<기능명>` | 버그 수정 | `fix/login-validation` |
| `docs/<주제>` | 문서 수정 | `docs/local-setup` |

커밋 메시지는 변경 목적을 알 수 있도록 작성합니다.

```text
feat: 여행 일정 등록 기능 추가
fix: 비로그인 헤더 메뉴 노출 오류 수정
docs: 로컬 실행 방법 보완
chore: Gradle 의존성 설정 수정
```

작업 완료 후 테스트를 통과시키고 Pull Request의 대상 브랜치를 `dev`로 지정합니다. 다른 팀원의 브랜치나 `dev`에 임의로 직접 push하지 않습니다.

## 9. 구현 시 공통 원칙

- Controller에서 SQL이나 비즈니스 규칙을 직접 처리하지 않습니다.
- Service에서 업무 규칙과 트랜잭션을 관리합니다.
- MyBatis XML은 `src/main/resources/mapper/<기능>/`에 둡니다.
- MyBatis SQL은 문자열 연결 대신 파라미터 바인딩을 사용합니다.
- 요청 DTO에는 Jakarta Validation을 적용합니다.
- 성공 API 응답은 `ApiResponse.success(data)` 형식을 사용합니다.
- 예상 가능한 오류는 `BusinessException`과 안정적인 영문 오류 코드를 사용합니다.
- JSP 공통 영역은 `WEB-INF/jsp/common/`의 조각 파일을 include합니다.
- 페이지별 CSS는 공통 `layout.css`에 몰아넣지 않고 별도 파일로 분리합니다.
- 비밀번호, API 키, 개인 `.env`, 로그 파일은 커밋하지 않습니다.
- 수정·삭제 기능은 로그인 여부뿐 아니라 대상 데이터의 소유권도 확인합니다.

## 10. 기능 완료 기준

화면이 한 번 정상적으로 열리는 것만으로 기능 완료로 판단하지 않습니다.

- [ ] 정상 입력과 정상 조회 확인
- [ ] 필수값 누락·잘못된 입력 확인
- [ ] 빈 목록·검색 결과 없음 확인
- [ ] 로그인 전후 및 권한 확인
- [ ] DB 저장·수정·삭제 결과 확인
- [ ] 새로고침과 중복 요청 확인
- [ ] 한글·공백·특수문자·날짜 경계값 확인
- [ ] `./gradlew test` 또는 `gradlew.bat test` 통과
- [ ] 비밀값과 담당 범위 밖 파일이 커밋에 포함되지 않음
- [ ] API·DB·화면 변경 시 관련 문서도 함께 수정

## 11. 최초 실행 체크리스트

모든 팀원은 기능 개발 전에 아래 항목을 완료합니다.

- [ ] GitHub 저장소 접근 및 clone 성공
- [ ] `dev` 브랜치 최신화 완료
- [ ] JDK와 Gradle JVM 모두 Java 21로 설정
- [ ] Oracle 접속 성공
- [ ] 환경변수 설정 완료
- [ ] Gradle 의존성 다운로드 및 테스트 성공
- [ ] Spring Boot 실행 성공
- [ ] `http://localhost:8080` 접속 성공
- [ ] JSP 한글 깨짐 없음
- [ ] 개인 비밀 설정 파일이 Git 추적 대상이 아님

문제가 발생하면 다음 내용을 함께 공유합니다.

```text
1. 실행한 명령 또는 작업 순서
2. 전체 오류 메시지와 첫 번째 Caused by
3. OS, JDK, Oracle 버전
4. 정상 동작한 마지막 단계
5. 이미 시도한 해결 방법
```

## 12. 자주 확인할 문제

| 증상 | 우선 확인할 항목 |
| --- | --- |
| Gradle 빌드 실패 | Gradle JVM과 `JAVA_HOME`이 Java 21인지 확인 |
| Oracle 연결 실패 | Listener, 서비스명, URL, 계정·비밀번호 확인 |
| `ORA-12505` | SID 대신 실제 서비스명과 `@//host:port/service` 형식인지 확인 |
| 8080 포트 충돌 | 기존 서버 종료 또는 `SERVER_PORT` 변경 |
| JSP 화면 404 | Controller 반환 경로와 `/WEB-INF/jsp/` 실제 파일명 확인 |
| Mapper 오류 | 인터페이스 경로, XML namespace, statement id 확인 |
| 한글 깨짐 | IDE·파일·응답 인코딩을 UTF-8로 통일 |
| Linux에서만 파일을 못 찾음 | import 경로와 실제 파일명의 대소문자 확인 |

## 13. 향후 Vue 전환

현재 화면은 JSP로 구현합니다. Vue 도입에 대비해 다음 원칙을 지킵니다.

- JSP용 화면 Controller와 `/api/**` REST Controller를 분리합니다.
- Service와 MyBatis 계층은 화면 기술에 의존하지 않게 작성합니다.
- API의 성공·실패 응답 구조를 통일합니다.
- 인증 상태에 따라 달라지는 공통 Header와 Footer 로직을 한 곳에서 관리합니다.
- Vue 전환 시 기능 단위로 JSP 화면을 교체하고 기존 API를 재사용합니다.

## 14. README 유지관리

설치 방법, 실행 명령, 포트, 환경변수, 폴더 구조가 바뀌면 기능 코드와 같은 PR에서 README도 수정합니다. 신규 팀원이 별도 설명 없이 이 문서만 보고 로컬 실행에 성공할 수 있는 상태를 유지합니다.
