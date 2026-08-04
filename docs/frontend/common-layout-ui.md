# 공통 레이아웃·UI Component 가이드

이 문서는 WithTrip 프론트엔드의 공통 레이아웃과 UI Component 구현 기준을 설명합니다. 코드와 문서의 기준일은 2026-08-03입니다.

## 1. 적용 범위

현재 공통 UI 작업은 다음 범위를 포함합니다.

- orange 계열 brand theme와 공통 design token
- 사용자 Header·Footer·본문 skip link를 제공하는 `DefaultLayout`
- 관리자 Sidebar·Header·scroll 영역을 제공하는 `AdminLayout`
- `BaseButton`, `BaseInput`, `BaseModal`, `AsyncState`
- Pinia 기반 전역 Toast와 `ToastRegion`
- Vue Router의 `RouterLink`, route scroll 처리와 404 fallback
- Desktop·Mobile 반응형 및 keyboard 접근성

## 2. 파일 구조와 책임

```text
frontend/src/
├── App.vue                         # RouterView와 전역 ToastRegion
├── assets/main.css                 # design token, reset, app-container
├── components/
│   ├── AppHeader.vue               # 사용자 navigation과 mobile menu
│   ├── AppFooter.vue               # 사용자 footer navigation
│   └── ui/
│       ├── BaseButton.vue
│       ├── BaseInput.vue
│       ├── BaseModal.vue
│       ├── AsyncState.vue
│       └── ToastRegion.vue
├── layouts/
│   ├── DefaultLayout.vue
│   └── AdminLayout.vue
└── stores/toast.js
```

`views`는 route 단위 데이터와 상태를 소유하고, `components/ui`는 business logic을 포함하지 않습니다. 화면 Component는 공통 UI를 조합하고 API 호출은 `src/api`에 둡니다.

## 3. Design token

모든 token은 `frontend/src/assets/main.css`의 `:root`에 정의합니다.

| 분류 | 주요 token | 용도 |
| --- | --- | --- |
| Brand | `--color-brand`, `--color-brand-hover`, `--color-brand-accent` | 주요 CTA, 강조, hover |
| Brand state | `--color-brand-soft`, `--color-brand-border`, `--color-brand-focus` | 배경, border, focus ring |
| Surface | `--color-page`, `--color-surface`, `--color-surface-muted` | 페이지와 card 배경 |
| Text | `--color-text`, `--color-text-muted` | 본문과 보조 문구 |
| Feedback | `--color-success`, `--color-danger`, `--color-info` | 성공·실패·안내 상태 |
| Layout | `--layout-content-width`, `--layout-shell-width`, `--layout-gutter`, `--layout-header-height` | 공통 너비와 간격 |

화면별 CSS에서 orange hex 값을 복제하지 않습니다. 새로운 의미가 필요하면 기존 token으로 표현할 수 있는지 먼저 확인하고, 공통 의미일 때만 `main.css`에 token을 추가합니다.

## 4. Layout

### DefaultLayout

사용자 화면의 기본 shell입니다.

- `본문 바로가기` skip link 제공
- `AppHeader`와 `AppFooter` 공유
- `<main id="main-content">` landmark 제공
- main 영역이 남은 viewport 높이를 채우도록 구성

```vue
<template>
  <DefaultLayout>
    <section class="page">...</section>
  </DefaultLayout>
</template>
```

페이지 내용의 최대 너비가 필요하면 `.app-container`를 사용합니다. 화면에서 Header 또는 Footer를 직접 다시 mount하지 않습니다.

### AdminLayout

관리자 route의 shell입니다. `route.meta.title`을 Header 제목으로 전달하고, Sidebar를 제외한 내용 영역만 독립적으로 scroll합니다. 사용자용 `DefaultLayout`과 중첩하지 않습니다.

## 5. UI Component API

### BaseButton

| 항목 | 값 | 기본값 |
| --- | --- | --- |
| `type` | `button`, `submit`, `reset` | `button` |
| `variant` | `primary`, `secondary`, `ghost`, `danger` | `primary` |
| `size` | `sm`, `md`, `lg` | `md` |
| `disabled` | Boolean | `false` |
| `loading` | Boolean | `false` |
| `block` | Boolean | `false` |

`loading`이면 button이 자동으로 disabled 되고 `aria-busy="true"`를 제공합니다.

```vue
<BaseButton type="submit" :loading="submitting" block>여행 계획 만들기</BaseButton>
```

### BaseInput

`v-model`을 지원하는 한 줄 input입니다. `label`, `hint`, `error`를 input과 자동 연결하고 error가 있으면 `aria-invalid`와 alert를 제공합니다. HTML input attribute는 `$attrs`로 전달할 수 있습니다.

주요 props는 `modelValue`, `id`, `label`, `type`, `hint`, `error`, `disabled`, `readonly`, `required`이며 `update:modelValue`, `blur` event를 발생시킵니다.

```vue
<BaseInput
  v-model="email"
  id="email"
  label="이메일"
  type="email"
  :error="fieldErrors.email"
  autocomplete="email"
  required
/>
```

여러 줄 입력, select, date range처럼 별도의 상호작용 구조가 필요한 입력은 의미에 맞는 전용 Component로 분리합니다.

### BaseModal

`Teleport`로 `body`에 렌더링되며 다음 동작을 공통 처리합니다.

- `role="dialog"`, `aria-modal`, 제목·설명 연결
- 열릴 때 닫기 버튼 또는 dialog로 focus 이동
- Tab focus 순환과 ESC 종료
- 배경 body scroll 잠금 및 종료 후 복원
- Modal을 열었던 요소로 focus 복원
- overlay click 종료와 mobile 너비 제한

| Prop | 기본값 | 설명 |
| --- | --- | --- |
| `open` | `true` | 표시 여부 |
| `title` | 빈 문자열 | dialog 제목과 accessible name |
| `description` | 빈 문자열 | 제목 아래 설명 |
| `ariaLabel` | 빈 문자열 | `title`이 없을 때 accessible name |
| `closeLabel` | `닫기` | 닫기 버튼 accessible name |
| `showClose` | `true` | 닫기 버튼 표시 |
| `closeOnOverlay` | `true` | overlay click 종료 |
| `closeOnEscape` | `true` | ESC 종료 |
| `width` | `520px` | 최대 dialog 너비 |

기본 slot은 본문, `footer` slot은 action 영역입니다. 종료 시 `close` event를 발생시키며 open state는 부모가 소유합니다.

```vue
<BaseModal title="일정 가져오기" width="520px" @close="showModal = false">
  <BaseInput v-model="planName" label="플랜 이름" required />
  <template #footer>
    <BaseButton variant="secondary" @click="showModal = false">취소</BaseButton>
    <BaseButton @click="submit">가져오기</BaseButton>
  </template>
</BaseModal>
```

### AsyncState

API 기반 화면의 loading·empty·error 표현을 통일합니다.

| Prop | 값 |
| --- | --- |
| `variant` | `loading`, `empty`, `error` |
| `title` | 필수 상태 제목 |
| `message` | 선택 설명 |
| `actionLabel` | 선택 action 버튼 문구 |

`actionLabel`이 있고 loading 상태가 아니면 버튼을 표시하고 `action` event를 발생시킵니다. Error는 assertive alert, 나머지는 polite status로 전달합니다.

### ToastRegion과 toast store

`ToastRegion`은 `App.vue`에 한 번만 mount되어 있습니다. 화면에서는 `useToastStore()`만 호출합니다.

```js
const toastStore = useToastStore()

toastStore.success('여행 계획이 만들어졌습니다.')
toastStore.error('저장하지 못했습니다.', { duration: 6000 })
toastStore.info('새로운 안내가 있습니다.', { duration: 0 })
```

지원 type은 `success`, `error`, `info`이며 기본 표시 시간은 4초입니다. `duration: 0`이면 자동으로 닫히지 않습니다. 빈 메시지는 등록하지 않습니다.

## 6. 현재 적용 화면

| 화면 | 적용 내용 |
| --- | --- |
| 전체 사용자 화면 | `DefaultLayout`, 공통 Header·Footer, orange token |
| 전체 관리자 화면 | `AdminLayout` token 정렬 |
| 여행 계획 생성 | `AsyncState`, 생성 성공 Toast |
| 여행 상세 신고 | `BaseModal`, `BaseButton` |
| 여행 일정 가져오기 | `BaseModal`, `BaseInput`, `BaseButton` |
| 미등록 URL | 404 route와 `NotFoundView` |

새로운 화면부터 공통 Component를 우선 적용하고, 기존 화면은 기능 수정 시 점진적으로 교체합니다. 전체 화면을 한 번에 재작성하지 않습니다.

## 7. 접근성과 반응형 기준

- 모든 route는 keyboard만으로 주요 기능에 접근할 수 있어야 합니다.
- focus가 보이는 전역 `:focus-visible` style을 유지합니다.
- Modal을 닫으면 실행 버튼으로 focus가 복원되어야 합니다.
- 상태 변화는 `status`, `alert`, `aria-live`, `aria-busy`로 전달합니다.
- animation은 `prefers-reduced-motion: reduce`에서 제거합니다.
- 기본 확인 viewport는 Desktop `1280 × 720`, Mobile `390 × 844`입니다.
- Mobile에서 dialog는 viewport 좌우 12px 이상 여백을 유지하고 date field는 1열로 배치합니다.

## 8. 테스트와 검증

Component test는 `frontend/src/components/ui/__tests__`, store test는 `frontend/src/stores/__tests__`에 둡니다.

```powershell
cd frontend
npm run lint
npm run test:unit -- --run
npm run build
```

2026-08-03 기준 검증 결과:

- Vitest: 22개 test file, 83개 test 통과
- ESLint와 Oxlint 통과
- Vite production build 통과
- Desktop 신고 Modal과 Mobile 일정 가져오기 Modal 시각 검증 완료
- Modal focus, ESC 종료, body scroll 복원 확인
- Mobile 공통 ErrorState viewport 내부 배치 확인
- `PlanDetailView`를 `320px`, `390 × 844`, `760px`에서 검증하고 페이지 가로 overflow가 없음을 확인

## 9. 남은 작업

- 기존 화면의 개별 button·input을 공통 Component로 점진적 전환
- API 연동 완료 후 성공 Toast와 실제 server error 흐름에 대한 E2E 보강
- 공통 select, textarea가 두 화면 이상에서 반복될 때 Component 후보 재검토

공통 Component API나 design token이 바뀌면 구현, test, 이 문서를 같은 변경 단위에서 갱신합니다.
