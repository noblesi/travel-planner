<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { RouterLink } from 'vue-router'

import { usePlanEditorStore } from '@/stores/planEditor'

const props = defineProps({
  planId: {
    type: String,
    required: true,
  },
})

const editorStore = usePlanEditorStore()
const { status, errorMessage, plan, days, selectedDay, scheduleItems, isLoading, isEmpty, isReady } =
  storeToRefs(editorStore)

function formatDate(date) {
  if (!date) return ''

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date(`${date}T00:00:00`))
}

function retryLoad() {
  return editorStore.loadPlanEditor(props.planId)
}

watch(() => props.planId, retryLoad, { immediate: true })
onBeforeUnmount(editorStore.resetEditor)
</script>

<template>
  <div class="editor-page">
    <header class="editor-toolbar">
      <div class="editor-toolbar__inner">
        <RouterLink class="back-button" to="/plans/new" aria-label="여행 계획 설정으로 돌아가기">
          <span aria-hidden="true">←</span>
        </RouterLink>

        <div class="plan-heading">
          <span class="plan-heading__eyebrow">WITH TRIP PLANNER</span>
          <template v-if="plan">
            <h1>{{ plan.title }}</h1>
            <p>{{ plan.regionName }} · {{ formatDate(plan.startDate) }} - {{ formatDate(plan.endDate) }}</p>
          </template>
          <template v-else>
            <h1>여행 플랜 제작</h1>
            <p>여행 정보를 불러오는 중입니다.</p>
          </template>
        </div>

        <div class="editor-toolbar__actions">
          <span v-if="isReady" class="save-state">
            <span aria-hidden="true" />
            불러오기 완료
          </span>
          <RouterLink class="exit-button" to="/">나가기</RouterLink>
        </div>
      </div>
    </header>

    <main class="editor-main">
      <section v-if="isLoading || status === 'idle'" class="editor-state" aria-live="polite">
        <span class="editor-state__spinner" aria-hidden="true" />
        <strong>여행 계획을 불러오고 있어요.</strong>
        <p>일차와 저장된 일정을 준비하고 있습니다.</p>
      </section>

      <section v-else-if="status === 'error'" class="editor-state editor-state--error" role="alert">
        <span class="editor-state__icon" aria-hidden="true">!</span>
        <strong>{{ errorMessage }}</strong>
        <p>잠시 후 다시 시도하거나 여행 계획 설정으로 돌아가 주세요.</p>
        <button type="button" @click="retryLoad">다시 시도</button>
      </section>

      <template v-else-if="isReady">
        <aside class="schedule-panel" aria-label="여행 일정 편집 영역">
          <header class="schedule-panel__header">
            <div>
              <span>TRAVEL SCHEDULE</span>
              <h2>여행 일정</h2>
            </div>
            <span class="visibility-badge">{{ plan.visibility === 'PUBLIC' ? '공개' : '비공개' }}</span>
          </header>

          <div class="plan-summary">
            <div>
              <span>여행 기간</span>
              <strong>{{ days.length }}일</strong>
            </div>
            <div>
              <span>여행 지역</span>
              <strong>{{ plan.regionName }}</strong>
            </div>
          </div>

          <div class="day-preview">
            <div class="day-preview__label">
              <span>선택된 일정</span>
              <strong v-if="selectedDay">DAY {{ selectedDay.dayNo }}</strong>
              <strong v-else>일정 없음</strong>
              <small v-if="selectedDay">{{ formatDate(selectedDay.travelDate) }}</small>
            </div>

            <div v-if="isEmpty" class="empty-schedule" role="status">
              <span class="empty-schedule__mark" aria-hidden="true">+</span>
              <strong>아직 등록된 장소가 없습니다.</strong>
              <p>장소 검색과 지도 기능이 연결되면 오전·오후 일정을 추가할 수 있어요.</p>
            </div>

            <div v-else class="schedule-preview" role="status">
              <span>등록된 일정</span>
              <strong>{{ scheduleItems.length }}곳</strong>
              <p>일차별 오전·오후 일정은 다음 단계에서 편집할 수 있도록 연결됩니다.</p>
            </div>
          </div>

          <footer class="schedule-panel__footer">
            <span>플랜 ID</span>
            <code>{{ plan.planId }}</code>
          </footer>
        </aside>

        <section class="map-panel" aria-label="여행 장소 지도 영역">
          <div class="map-search">
            <span aria-hidden="true">⌕</span>
            <input type="search" placeholder="장소 검색 기능을 준비하고 있습니다" disabled />
          </div>

          <div class="map-canvas">
            <div class="map-grid" aria-hidden="true" />
            <span class="map-pin map-pin--one" aria-hidden="true" />
            <span class="map-pin map-pin--two" aria-hidden="true" />
            <div class="map-placeholder">
              <span class="map-placeholder__icon" aria-hidden="true">⌖</span>
              <strong>{{ plan.regionName }} 지도</strong>
              <p>카카오맵과 장소 검색은 다음 연동 단계에서 표시됩니다.</p>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.editor-page {
  min-height: 100vh;
  color: #172033;
  background: #eef2f7;
}

.editor-toolbar {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid #e1e7ef;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 4px 18px rgb(15 23 42 / 5%);
  backdrop-filter: blur(12px);
}

.editor-toolbar__inner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  min-height: 82px;
  padding: 12px clamp(18px, 3vw, 42px);
}

.back-button,
.exit-button {
  display: inline-grid;
  min-height: 44px;
  place-items: center;
  border: 1px solid #d8dee8;
  border-radius: 12px;
  background: #fff;
  font-weight: 750;
}

.back-button {
  width: 44px;
  color: #475569;
  font-size: 24px;
}

.plan-heading {
  min-width: 0;
}

.plan-heading__eyebrow {
  color: #ff5a4e;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.plan-heading h1 {
  overflow: hidden;
  margin: 3px 0 2px;
  font-size: clamp(19px, 2vw, 24px);
  letter-spacing: -0.035em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-heading p {
  overflow: hidden;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-toolbar__actions {
  display: flex;
  gap: 14px;
  align-items: center;
}

.save-state {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  color: #64748b;
  font-size: 13px;
  font-weight: 650;
}

.save-state span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgb(34 197 94 / 12%);
}

.exit-button {
  padding: 0 18px;
  color: #334155;
}

.editor-main {
  min-height: calc(100vh - 82px);
}

.editor-state {
  display: grid;
  min-height: calc(100vh - 82px);
  padding: 40px 20px;
  place-content: center;
  justify-items: center;
  text-align: center;
}

.editor-state strong {
  margin-top: 20px;
  color: #1e293b;
  font-size: 20px;
}

.editor-state p {
  margin: 8px 0 0;
  color: #64748b;
}

.editor-state button {
  min-height: 44px;
  margin-top: 22px;
  padding: 0 20px;
  color: #fff;
  border: 0;
  border-radius: 12px;
  background: #ff5a4e;
  font-weight: 750;
  cursor: pointer;
}

.editor-state__spinner {
  width: 42px;
  height: 42px;
  border: 4px solid #ffdcd8;
  border-top-color: #ff5a4e;
  border-radius: 50%;
  animation: spin 800ms linear infinite;
}

.editor-state__icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  color: #dc2626;
  border-radius: 50%;
  background: #fee2e2;
  font-size: 24px;
  font-weight: 850;
}

.editor-main:has(.schedule-panel) {
  display: grid;
  grid-template-columns: minmax(340px, 41%) minmax(0, 1fr);
}

.schedule-panel {
  display: flex;
  min-height: calc(100vh - 82px);
  flex-direction: column;
  padding: 32px clamp(20px, 3vw, 38px);
  border-right: 1px solid #e1e7ef;
  background: #fff;
}

.schedule-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.schedule-panel__header span:first-child {
  color: #ff5a4e;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.schedule-panel__header h2 {
  margin: 4px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.visibility-badge {
  padding: 7px 11px;
  color: #475569;
  border-radius: 999px;
  background: #f1f5f9;
  font-size: 12px;
  font-weight: 750;
}

.plan-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 26px;
}

.plan-summary div {
  padding: 16px;
  border: 1px solid #e5eaf1;
  border-radius: 14px;
  background: #f8fafc;
}

.plan-summary span,
.day-preview__label > span,
.schedule-preview > span,
.schedule-panel__footer span {
  display: block;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.04em;
}

.plan-summary strong {
  display: block;
  overflow: hidden;
  margin-top: 6px;
  color: #334155;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-preview {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

.day-preview__label {
  padding-bottom: 16px;
  border-bottom: 1px solid #e8edf3;
}

.day-preview__label strong {
  display: inline-block;
  margin-top: 6px;
  font-size: 21px;
}

.day-preview__label small {
  margin-left: 10px;
  color: #64748b;
}

.empty-schedule,
.schedule-preview {
  display: grid;
  min-height: 250px;
  padding: 28px;
  place-content: center;
  justify-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  background: #fbfcfe;
  text-align: center;
}

.empty-schedule__mark {
  display: grid;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  place-items: center;
  color: #ff5a4e;
  border-radius: 50%;
  background: #fff0ee;
  font-size: 29px;
  font-weight: 400;
}

.empty-schedule strong,
.schedule-preview strong {
  color: #334155;
  font-size: 17px;
}

.empty-schedule p,
.schedule-preview p {
  max-width: 310px;
  margin: 9px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
  word-break: keep-all;
}

.schedule-preview strong {
  margin-top: 8px;
  color: #ff5a4e;
  font-size: 34px;
}

.schedule-panel__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: auto;
  padding-top: 24px;
}

.schedule-panel__footer code {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  text-overflow: ellipsis;
}

.map-panel {
  position: relative;
  min-height: calc(100vh - 82px);
  overflow: hidden;
  background: #dbe9e4;
}

.map-search {
  position: absolute;
  z-index: 3;
  top: 28px;
  right: 28px;
  display: flex;
  width: min(390px, calc(100% - 56px));
  min-height: 50px;
  gap: 10px;
  align-items: center;
  padding: 0 17px;
  border: 1px solid rgb(148 163 184 / 60%);
  border-radius: 14px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 12px 36px rgb(15 23 42 / 12%);
}

.map-search span {
  color: #475569;
  font-size: 25px;
}

.map-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #64748b;
}

.map-canvas {
  position: relative;
  min-height: inherit;
  overflow: hidden;
  background:
    radial-gradient(circle at 74% 60%, rgb(120 194 211 / 70%) 0 16%, transparent 17%),
    radial-gradient(circle at 38% 38%, rgb(161 203 167 / 85%) 0 20%, transparent 21%),
    linear-gradient(135deg, #e9f2ec 0 46%, #d9e8e0 47% 60%, #b9dce4 61%);
}

.map-grid {
  position: absolute;
  inset: -20%;
  opacity: 0.32;
  background-image:
    linear-gradient(28deg, transparent 47%, #fff 48% 51%, transparent 52%),
    linear-gradient(118deg, transparent 47%, #fff 48% 51%, transparent 52%);
  background-size: 110px 90px;
  transform: rotate(-5deg);
}

.map-pin {
  position: absolute;
  z-index: 1;
  width: 24px;
  height: 24px;
  border: 5px solid #fff;
  border-radius: 50% 50% 50% 0;
  background: #ff5a4e;
  box-shadow: 0 5px 14px rgb(15 23 42 / 22%);
  transform: rotate(-45deg);
}

.map-pin--one {
  top: 31%;
  left: 31%;
}

.map-pin--two {
  right: 24%;
  bottom: 30%;
  background: #2c7be5;
}

.map-placeholder {
  position: absolute;
  z-index: 2;
  top: 50%;
  left: 50%;
  display: grid;
  width: min(420px, calc(100% - 48px));
  padding: 32px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 80%);
  border-radius: 20px;
  background: rgb(255 255 255 / 88%);
  box-shadow: 0 24px 60px rgb(15 23 42 / 13%);
  text-align: center;
  transform: translate(-50%, -50%);
  backdrop-filter: blur(10px);
}

.map-placeholder__icon {
  display: grid;
  width: 52px;
  height: 52px;
  margin-bottom: 14px;
  place-items: center;
  color: #fff;
  border-radius: 16px;
  background: #ff5a4e;
  font-size: 27px;
}

.map-placeholder strong {
  font-size: 21px;
}

.map-placeholder p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 860px) {
  .editor-toolbar__inner {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .editor-toolbar__actions {
    display: none;
  }

  .editor-main:has(.schedule-panel) {
    grid-template-columns: 1fr;
  }

  .schedule-panel {
    min-height: auto;
    border-right: 0;
  }

  .map-panel {
    min-height: 520px;
  }
}

@media (max-width: 520px) {
  .editor-toolbar__inner {
    gap: 12px;
    min-height: 74px;
    padding: 10px 14px;
  }

  .plan-heading__eyebrow,
  .plan-heading p {
    display: none;
  }

  .schedule-panel {
    padding: 26px 16px;
  }

  .plan-summary {
    grid-template-columns: 1fr;
  }

  .map-search {
    top: 16px;
    right: 16px;
    width: calc(100% - 32px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner {
    animation: none;
  }
}
</style>
