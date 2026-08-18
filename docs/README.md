# 설계 자료

프로젝트 구현 시 기준으로 사용하는 원본 설계 자료입니다.

- [UI 설계서](design/UI설계.pdf): 사용자 및 관리자 화면 62페이지
- [ERD 설계](database/travelplanner_v2.exerd): eXERD 원본 파일
- [여행 플랜 Oracle DDL](database/ddl/README.md): 핵심 Schema, 지역 초기 데이터, 검증 Script
- [API 계약](api/README.md): 공통 규칙, 인증, 여행 플랜·일정 자동 저장 API, 오류 코드
- [인증 방식 결정](auth/authentication-decision.md): 로컬·Google 로그인과 회원 계정 연결 원칙
- [공통 레이아웃·UI Component](frontend/common-layout-ui.md): Vue 사용자 layout, orange design token, 공통 Component API와 적용 규칙
- [배포·시연 체크리스트](deployment/release-checklist.md): Release 검증, HTTPS Reverse Proxy, Kakao 허용 도메인, Oracle 데모 점검

ERD 파일은 eXERD에서 열어 확인하고, 변경 시 원본 파일명과 형식을 유지합니다.
