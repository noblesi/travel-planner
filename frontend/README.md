# WithTrip Frontend

이 프로젝트는 Release script와 CI의 `npm ci` 재현성을 기준으로 npm만 사용합니다. 다른 package manager의 lockfile을 함께 유지하면 dependency 해석 결과가 달라질 수 있으므로 `package-lock.json`을 단일 기준으로 사용합니다.

WithTrip의 Vue 3 + Vite 프론트엔드입니다. 전체 프로젝트 실행 방법과 협업 규칙은 루트의 `README.md`와 `CONTRIBUTING.md`를 확인합니다.

## 구조

```text
src/
├── api/                 # Axios instance와 기능별 API 함수
├── assets/main.css      # 전역 reset, design token, 공통 container
├── components/
│   ├── ui/              # BaseButton, BaseInput, BaseModal, AsyncState, ToastRegion
│   ├── AppHeader.vue
│   └── AppFooter.vue
├── layouts/             # 사용자 화면의 DefaultLayout
├── router/              # 사용자 route와 404 fallback
├── stores/              # Pinia 전역 상태와 Toast store
└── views/               # route 단위 화면
```

사용자 화면은 `DefaultLayout`을 사용해 skip link, Header, main landmark, Footer를 공유합니다. 관리자 화면은 프론트엔드 SPA 범위에 포함하지 않으며 Backend의 Spring MVC + Thymeleaf로 제공합니다. 상세 구조와 Component API는 [공통 레이아웃·UI Component 가이드](../docs/frontend/common-layout-ui.md)를 확인합니다.

## 권장 IDE

[VS Code](https://code.visualstudio.com/)와 [Vue - Official](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 확장을 권장합니다. Vetur가 설치되어 있다면 비활성화합니다.

## 설치

```sh
npm ci
```

## 개발 실행

```sh
npm run dev
```

## 프로덕션 빌드

```sh
npm run build
```

## 단위 테스트

```sh
npm run test:unit -- --run
```

## 린트

```sh
npm run lint
```

## 공통 UI 사용 원칙

- brand 색상과 layout 수치는 `src/assets/main.css`의 CSS variable을 사용합니다.
- 일반 동작 버튼은 `BaseButton`, 한 줄 입력은 `BaseInput`, dialog는 `BaseModal`을 우선 사용합니다.
- API loading·empty·error 상태는 `AsyncState`로 표현합니다.
- route가 바뀌어도 유지되어야 하는 사용자 알림은 `useToastStore()`로 표시합니다. `ToastRegion`은 `App.vue`에 한 번만 mount합니다.
- 화면 전용 Component는 공통 Component를 조합하며 focus 관리, scroll lock, ARIA 연결을 중복 구현하지 않습니다.

예시:

```vue
<script setup>
import BaseButton from '@/components/ui/BaseButton.vue'
import AsyncState from '@/components/ui/AsyncState.vue'
</script>

<template>
  <AsyncState
    v-if="status === 'error'"
    variant="error"
    title="목록을 불러오지 못했습니다."
    action-label="다시 시도"
    @action="loadItems"
  />
  <BaseButton v-else :loading="saving" @click="save">저장</BaseButton>
</template>
```

## 변경 후 확인

```sh
npm run lint
npm run test:unit -- --run
npm run build
```

공통 레이아웃이나 UI Component 변경은 최소 Desktop 1280px와 Mobile 390px에서 확인합니다. Modal은 focus 진입·Tab 순환·ESC 종료·배경 scroll 복원까지 확인합니다.
