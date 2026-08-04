<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { onBeforeRouteLeave, RouterLink } from 'vue-router'

import KakaoMap from '@/components/map/KakaoMap.vue'
import PlanEditorSettings from '@/components/plan/PlanEditorSettings.vue'
import PlaceSearchPanel from '@/components/plan/PlaceSearchPanel.vue'
import ScheduleList from '@/components/plan/ScheduleList.vue'
import { usePlanEditorStore } from '@/stores/planEditor'

const props = defineProps({
  planId: {
    type: String,
    required: true,
  },
})

const editorStore = usePlanEditorStore()
const {
  status,
  errorMessage,
  plan,
  days,
  selectedDayId,
  selectedDay,
  scheduleItems,
  morningItems,
  afternoonItems,
  isSelectedDayEmpty,
  isLoading,
  isReady,
  isSaving,
  hasSaveError,
  canRetrySave,
  saveStatus,
  saveMessage,
  saveErrorMessage,
  pendingSaveCount,
} = storeToRefs(editorStore)

const settingsBusy = ref(false)
const searchResults = ref([])
const selectedSearchPlace = ref(null)

const selectedSearchPlaceId = computed(() => {
  if (!selectedSearchPlace.value) return null
  return `${selectedSearchPlace.value.placeProvider}:${selectedSearchPlace.value.externalPlaceId}`
})

const selectedMapPlaceId = computed(() =>
  selectedSearchPlaceId.value ? `search:${selectedSearchPlaceId.value}` : null,
)

const mapPlaces = computed(() => [
  ...scheduleItems.value.map((item) => ({
    ...item,
    mapPlaceId: `schedule:${item.scheduleItemId}`,
    markerSource: 'SCHEDULE',
  })),
  ...searchResults.value.map((place) => ({
    ...place,
    mapPlaceId: `search:${place.placeProvider}:${place.externalPlaceId}`,
    markerSource: 'SEARCH',
  })),
])

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

function selectDay(planDayId) {
  editorStore.selectDay(planDayId)
}

function updateSearchResults(places) {
  searchResults.value = places

  if (
    selectedSearchPlace.value &&
    !places.some(
      (place) =>
        place.placeProvider === selectedSearchPlace.value.placeProvider &&
        place.externalPlaceId === selectedSearchPlace.value.externalPlaceId,
    )
  ) {
    selectedSearchPlace.value = null
  }
}

function selectSearchPlace(place) {
  selectedSearchPlace.value = place
}

function selectMapPlace(place) {
  if (place.markerSource === 'SEARCH') {
    selectedSearchPlace.value = place
  }
}

async function runScheduleOperation(operation) {
  try {
    return await operation
  } catch {
    return null
  }
}

function addSearchPlace({ place, timeSlot }) {
  if (!selectedDay.value) return null
  return runScheduleOperation(editorStore.addPlaceToSchedule(place, timeSlot))
}

function moveScheduleItem(item, targetTimeSlot) {
  return runScheduleOperation(
    editorStore.moveScheduleItemTimeSlot(item.scheduleItemId, targetTimeSlot),
  )
}

function moveScheduleItemPosition(item, direction) {
  return runScheduleOperation(editorStore.moveScheduleItemPosition(item.scheduleItemId, direction))
}

function removeScheduleItem(item) {
  return runScheduleOperation(editorStore.removeScheduleItem(item.scheduleItemId))
}

function retryScheduleSave() {
  return runScheduleOperation(editorStore.retryLastSave())
}

watch(() => props.planId, retryLoad, { immediate: true })
onBeforeRouteLeave(async () => {
  if (pendingSaveCount.value > 0) await editorStore.waitForPendingSaves()
  return true
})
onBeforeUnmount(editorStore.resetEditor)
</script>

<template>
  <div class="editor-page">
    <header class="editor-toolbar">
      <div class="editor-toolbar__inner">
        <RouterLink
          class="back-button"
          to="/plans/new"
          aria-label="여행 계획 설정으로 돌아가기"
          :aria-disabled="isSaving"
          :tabindex="isSaving ? -1 : undefined"
          @click="isSaving && $event.preventDefault()"
        >
          <span aria-hidden="true">←</span>
        </RouterLink>

        <div class="plan-heading">
          <span class="plan-heading__eyebrow">WITH TRIP PLANNER</span>
          <template v-if="plan">
            <h1>{{ plan.title }}</h1>
            <p>
              {{ plan.regionName }} · {{ formatDate(plan.startDate) }} -
              {{ formatDate(plan.endDate) }}
            </p>
          </template>
          <template v-else>
            <h1>여행 플랜 제작</h1>
            <p>여행 정보를 불러오는 중입니다.</p>
          </template>
        </div>

        <div class="editor-toolbar__actions">
          <span
            v-if="isReady"
            class="save-state"
            :class="`save-state--${saveStatus}`"
            role="status"
            aria-live="polite"
          >
            <span aria-hidden="true" />
            {{ saveMessage }}
            <small v-if="pendingSaveCount > 1">{{ pendingSaveCount }}건</small>
          </span>
          <RouterLink
            class="exit-button"
            to="/"
            :aria-disabled="isSaving"
            :tabindex="isSaving ? -1 : undefined"
            @click="isSaving && $event.preventDefault()"
          >
            {{ isSaving ? '저장 중' : '나가기' }}
          </RouterLink>
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
            <span class="visibility-badge">{{
              plan.visibility === 'PUBLIC' ? '공개' : '비공개'
            }}</span>
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

          <RouterLink
            class="invite-panel-link"
            :to="{ name: 'invite', params: { id: plan.planId } }"
          >
            동행자 초대 링크 만들기
          </RouterLink>

          <PlanEditorSettings @busy-change="settingsBusy = $event" />

          <section v-if="hasSaveError" class="save-error" role="alert">
            <div>
              <strong>{{
                saveStatus === 'conflict'
                  ? '일정 충돌을 확인해 주세요.'
                  : '자동 저장이 중단되었습니다.'
              }}</strong>
              <p>{{ saveErrorMessage }}</p>
            </div>
            <div class="save-error__actions">
              <button v-if="canRetrySave" type="button" @click="retryScheduleSave">
                같은 작업 다시 시도
              </button>
              <button type="button" @click="editorStore.discardFailedSave">닫기</button>
            </div>
          </section>

          <nav class="day-tabs" aria-label="여행 일차 선택">
            <button
              v-for="day in days"
              :key="day.planDayId"
              class="day-tab"
              :class="{ 'day-tab--active': day.planDayId === selectedDayId }"
              type="button"
              :aria-pressed="day.planDayId === selectedDayId"
              @click="selectDay(day.planDayId)"
            >
              <strong>DAY {{ day.dayNo }}</strong>
              <small>{{ formatDate(day.travelDate) }}</small>
            </button>
          </nav>

          <div class="day-preview">
            <div class="day-preview__label">
              <span>선택된 일정</span>
              <strong v-if="selectedDay">DAY {{ selectedDay.dayNo }}</strong>
              <strong v-else>일정 없음</strong>
              <small v-if="selectedDay">{{ formatDate(selectedDay.travelDate) }}</small>
            </div>

            <div v-if="!selectedDay" class="empty-schedule" role="status">
              <span class="empty-schedule__mark" aria-hidden="true">!</span>
              <strong>여행 일차를 불러오지 못했습니다.</strong>
              <p>여행 날짜를 다시 확인하거나 잠시 후 다시 시도해 주세요.</p>
            </div>

            <div v-else-if="isSelectedDayEmpty" class="empty-schedule" role="status">
              <span class="empty-schedule__mark" aria-hidden="true">+</span>
              <strong>DAY {{ selectedDay.dayNo }}에 등록된 장소가 없습니다.</strong>
              <p>장소를 검색한 뒤 오전 또는 오후 일정에 바로 추가할 수 있어요.</p>
            </div>

            <div v-else class="schedule-groups">
              <section class="schedule-group" aria-labelledby="morning-heading">
                <header class="schedule-group__header">
                  <h3 id="morning-heading">오전</h3>
                  <span>{{ morningItems.length }}곳</span>
                </header>

                <ScheduleList
                  v-if="morningItems.length"
                  :items="morningItems"
                  time-slot="MORNING"
                  :disabled="settingsBusy"
                  @move-up="moveScheduleItemPosition($event, -1)"
                  @move-down="moveScheduleItemPosition($event, 1)"
                  @move-time-slot="moveScheduleItem($event, 'AFTERNOON')"
                  @remove="removeScheduleItem"
                />
                <p v-else class="schedule-group__empty">오전 일정이 없습니다.</p>
              </section>

              <section class="schedule-group" aria-labelledby="afternoon-heading">
                <header class="schedule-group__header">
                  <h3 id="afternoon-heading">오후</h3>
                  <span>{{ afternoonItems.length }}곳</span>
                </header>

                <ScheduleList
                  v-if="afternoonItems.length"
                  :items="afternoonItems"
                  time-slot="AFTERNOON"
                  :disabled="settingsBusy"
                  @move-up="moveScheduleItemPosition($event, -1)"
                  @move-down="moveScheduleItemPosition($event, 1)"
                  @move-time-slot="moveScheduleItem($event, 'MORNING')"
                  @remove="removeScheduleItem"
                />
                <p v-else class="schedule-group__empty">오후 일정이 없습니다.</p>
              </section>
            </div>
          </div>
        </aside>

        <section class="map-panel" aria-label="여행 장소 지도 영역">
          <KakaoMap
            class="editor-map"
            :places="mapPlaces"
            :selected-place-id="selectedMapPlaceId"
            :empty-message="`${plan.regionName}의 장소를 검색하면 지도에 표시됩니다.`"
            @select="selectMapPlace"
          />

          <div class="map-overlay">
            <PlaceSearchPanel
              :region-code="plan.regionCode"
              :region-name="plan.regionName"
              :selected-place-id="selectedSearchPlaceId"
              :schedule-disabled="settingsBusy || !selectedDay"
              @results-change="updateSearchResults"
              @select="selectSearchPlace"
              @add="addSearchPlace"
            />
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

.invite-panel-link {
  display: grid;
  min-height: 42px;
  margin-top: 14px;
  padding: 0 16px;
  place-items: center;
  color: #e8443a;
  border: 1px solid #ffc2bd;
  border-radius: 12px;
  background: #fff8f7;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
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

.save-state small {
  padding: 2px 6px;
  border-radius: 999px;
  background: #f1f5f9;
  font-size: 10px;
}

.save-state--saving span {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgb(245 158 11 / 14%);
  animation: save-pulse 1s ease-in-out infinite;
}

.save-state--error,
.save-state--conflict {
  color: #b91c1c;
}

.save-state--error span,
.save-state--conflict span {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgb(239 68 68 / 13%);
}

@keyframes save-pulse {
  50% {
    opacity: 0.35;
  }
}

.exit-button {
  padding: 0 18px;
  color: #334155;
}

.back-button[aria-disabled='true'],
.exit-button[aria-disabled='true'] {
  cursor: wait;
  opacity: 0.55;
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
.day-preview__label > span {
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

.save-error {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 13px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fff1f2;
}

.save-error strong,
.save-error p {
  display: block;
  margin: 0;
}

.save-error strong {
  color: #991b1b;
  font-size: 12px;
}

.save-error p {
  margin-top: 4px;
  color: #b91c1c;
  font-size: 11px;
  line-height: 1.45;
}

.save-error__actions {
  display: flex;
  gap: 7px;
}

.save-error__actions button {
  min-height: 31px;
  padding: 0 10px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fff;
  color: #991b1b;
  font-size: 10px;
  font-weight: 800;
  cursor: pointer;
}

.day-tabs {
  display: flex;
  gap: 10px;
  margin-top: 22px;
  padding-bottom: 4px;
  overflow-x: auto;
}

.day-tab {
  display: grid;
  min-width: 112px;
  gap: 4px;
  padding: 12px 14px;
  color: #64748b;
  border: 1px solid #dfe5ed;
  border-radius: 13px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    color 160ms ease,
    border-color 160ms ease,
    background 160ms ease;
}

.day-tab strong {
  font-size: 14px;
}

.day-tab small {
  font-size: 11px;
  white-space: nowrap;
}

.day-tab:hover,
.day-tab--active {
  color: #e8443a;
  border-color: #ff9b93;
  background: #fff5f4;
}

.day-tab:focus-visible {
  outline: 3px solid rgb(255 90 78 / 20%);
  outline-offset: 2px;
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

.empty-schedule {
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

.empty-schedule strong {
  color: #334155;
  font-size: 17px;
}

.empty-schedule p {
  max-width: 310px;
  margin: 9px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
  word-break: keep-all;
}

.schedule-groups {
  display: grid;
  gap: 16px;
}

.schedule-group {
  padding: 16px;
  border: 1px solid #e5eaf1;
  border-radius: 16px;
  background: #fbfcfe;
}

.schedule-group__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.schedule-group__header h3 {
  margin: 0;
  color: #334155;
  font-size: 16px;
}

.schedule-group__header span {
  color: #ff5a4e;
  font-size: 12px;
  font-weight: 800;
}

.schedule-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.schedule-group__empty {
  margin: 12px 0 0;
  padding: 16px;
  color: #94a3b8;
  border: 1px dashed #d6dce5;
  border-radius: 11px;
  background: #fff;
  font-size: 12px;
  text-align: center;
}

.map-panel {
  position: relative;
  min-height: calc(100vh - 82px);
  overflow: hidden;
  background: #dbe9e4;
}

.editor-map {
  position: absolute;
  inset: 0;
  border-radius: 0;
}

.map-overlay {
  position: absolute;
  z-index: 4;
  top: 28px;
  right: 28px;
  width: min(390px, calc(100% - 56px));
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 860px) {
  .editor-toolbar__inner {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .save-state {
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
    min-height: 680px;
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

  .map-overlay {
    top: 16px;
    right: 16px;
    width: calc(100% - 32px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner,
  .day-tab {
    animation: none;
    transition: none;
  }

  .save-state--saving span {
    animation: none;
  }
}
</style>
