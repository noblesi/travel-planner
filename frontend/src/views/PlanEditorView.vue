<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
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
const {
  status,
  errorMessage,
  plan,
  days,
  selectedDayId,
  selectedDay,
  morningItems,
  afternoonItems,
  isSelectedDayEmpty,
  isLoading,
  isReady,
} = storeToRefs(editorStore)

const editingDates = ref(false)
const dateSubmitting = ref(false)
const dateError = ref('')
const editStartDate = ref('')
const editEndDate = ref('')
const removalConfirmationOpen = ref(false)
const pendingDatePayload = ref(null)

const maxEditableEndDate = computed(() => {
  if (!editStartDate.value) return undefined

  const date = new Date(`${editStartDate.value}T00:00:00`)
  date.setDate(date.getDate() + 13)
  return toDateInputValue(date)
})

function toDateInputValue(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 10)
}

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

function syncDateForm(planValue = plan.value) {
  editStartDate.value = planValue?.startDate ?? ''
  editEndDate.value = planValue?.endDate ?? ''
}

function openDateEditor() {
  syncDateForm()
  dateError.value = ''
  editingDates.value = true
}

function closeDateEditor() {
  editingDates.value = false
  dateError.value = ''
  removalConfirmationOpen.value = false
  pendingDatePayload.value = null
  syncDateForm()
}

function validateDates() {
  if (!editStartDate.value || !editEndDate.value) {
    return '시작일과 종료일을 모두 선택해 주세요.'
  }
  if (editStartDate.value > editEndDate.value) {
    return '종료일은 시작일보다 빠를 수 없습니다.'
  }

  const start = Date.parse(`${editStartDate.value}T00:00:00Z`)
  const end = Date.parse(`${editEndDate.value}T00:00:00Z`)
  if (Math.floor((end - start) / 86_400_000) + 1 > 14) {
    return '여행 기간은 최대 14일까지 설정할 수 있습니다.'
  }
  return ''
}

function dateApiErrorMessage(error) {
  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '여행 날짜를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

async function submitDateChange(force = false) {
  if (dateSubmitting.value) return

  const validationMessage = validateDates()
  if (validationMessage) {
    dateError.value = validationMessage
    return
  }

  const payload = force
    ? pendingDatePayload.value
    : {
        startDate: editStartDate.value,
        endDate: editEndDate.value,
        versionNo: plan.value.versionNo,
        force: false,
      }
  if (!payload) return

  dateSubmitting.value = true
  dateError.value = ''

  try {
    const data = await editorStore.savePlanDates({ ...payload, force })
    syncDateForm(data.plan)
    editingDates.value = false
    removalConfirmationOpen.value = false
    pendingDatePayload.value = null
  } catch (error) {
    if (error?.response?.data?.code === 'PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED') {
      pendingDatePayload.value = payload
      removalConfirmationOpen.value = true
    } else {
      removalConfirmationOpen.value = false
      pendingDatePayload.value = null
      dateError.value = dateApiErrorMessage(error)
    }
  } finally {
    dateSubmitting.value = false
  }
}

watch(() => props.planId, retryLoad, { immediate: true })
watch(
  plan,
  (planValue) => {
    if (!editingDates.value) syncDateForm(planValue)
  },
  { immediate: true },
)
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

          <section
            class="date-editor"
            :aria-label="editingDates ? undefined : '여행 날짜 변경'"
            :aria-labelledby="editingDates ? 'date-editor-heading' : undefined"
          >
            <button
              v-if="!editingDates"
              class="date-editor__open"
              type="button"
              @click="openDateEditor"
            >
              여행 날짜 변경
            </button>

            <form v-else class="date-editor__form" @submit.prevent="submitDateChange()">
              <div class="date-editor__heading">
                <div>
                  <span>DATE SETTINGS</span>
                  <h3 id="date-editor-heading">여행 날짜 변경</h3>
                </div>
                <button type="button" aria-label="날짜 변경 닫기" @click="closeDateEditor">
                  ×
                </button>
              </div>

              <div class="date-editor__grid">
                <label>
                  <span>시작일</span>
                  <input v-model="editStartDate" name="editStartDate" type="date" />
                </label>
                <label>
                  <span>종료일</span>
                  <input
                    v-model="editEndDate"
                    name="editEndDate"
                    type="date"
                    :min="editStartDate || undefined"
                    :max="maxEditableEndDate"
                  />
                </label>
              </div>

              <p class="date-editor__notice">
                같은 기간으로 이동하면 일정이 함께 이동하고, 제외되는 날짜의 일정은 확인 후
                삭제됩니다.
              </p>
              <p v-if="dateError" class="date-editor__error" role="alert">{{ dateError }}</p>

              <div class="date-editor__actions">
                <button type="button" :disabled="dateSubmitting" @click="closeDateEditor">
                  취소
                </button>
                <button type="submit" :disabled="dateSubmitting" :aria-busy="dateSubmitting">
                  {{ dateSubmitting ? '변경 중...' : '날짜 저장' }}
                </button>
              </div>
            </form>
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
              <p>장소 검색 기능이 연결되면 이 날짜의 오전·오후 일정을 추가할 수 있어요.</p>
            </div>

            <div v-else class="schedule-groups">
              <section class="schedule-group" aria-labelledby="morning-heading">
                <header class="schedule-group__header">
                  <h3 id="morning-heading">오전</h3>
                  <span>{{ morningItems.length }}곳</span>
                </header>

                <div v-if="morningItems.length" class="schedule-list">
                  <article
                    v-for="item in morningItems"
                    :key="item.scheduleItemId"
                    class="schedule-card"
                  >
                    <img
                      v-if="item.imageUrl"
                      :src="item.imageUrl"
                      :alt="`${item.placeName} 이미지`"
                    />
                    <div class="schedule-card__body">
                      <span>{{ item.positionNo }}번째 · {{ item.categoryName || '장소' }}</span>
                      <strong>{{ item.placeName }}</strong>
                      <p v-if="item.address">{{ item.address }}</p>
                      <p v-if="item.description" class="schedule-card__description">
                        {{ item.description }}
                      </p>
                    </div>
                  </article>
                </div>
                <p v-else class="schedule-group__empty">오전 일정이 없습니다.</p>
              </section>

              <section class="schedule-group" aria-labelledby="afternoon-heading">
                <header class="schedule-group__header">
                  <h3 id="afternoon-heading">오후</h3>
                  <span>{{ afternoonItems.length }}곳</span>
                </header>

                <div v-if="afternoonItems.length" class="schedule-list">
                  <article
                    v-for="item in afternoonItems"
                    :key="item.scheduleItemId"
                    class="schedule-card"
                  >
                    <img
                      v-if="item.imageUrl"
                      :src="item.imageUrl"
                      :alt="`${item.placeName} 이미지`"
                    />
                    <div class="schedule-card__body">
                      <span>{{ item.positionNo }}번째 · {{ item.categoryName || '장소' }}</span>
                      <strong>{{ item.placeName }}</strong>
                      <p v-if="item.address">{{ item.address }}</p>
                      <p v-if="item.description" class="schedule-card__description">
                        {{ item.description }}
                      </p>
                    </div>
                  </article>
                </div>
                <p v-else class="schedule-group__empty">오후 일정이 없습니다.</p>
              </section>
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

    <div
      v-if="removalConfirmationOpen"
      class="confirmation-backdrop"
      @click.self="removalConfirmationOpen = false"
    >
      <section
        class="confirmation-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="date-removal-title"
        aria-describedby="date-removal-description"
      >
        <span class="confirmation-dialog__icon" aria-hidden="true">!</span>
        <h2 id="date-removal-title">일정이 포함된 날짜를 제외할까요?</h2>
        <p id="date-removal-description">
          변경 범위에서 빠지는 DAY와 그 안의 오전·오후 일정이 삭제됩니다. 이 작업은 저장 후 되돌릴
          수 없습니다.
        </p>
        <div class="confirmation-dialog__actions">
          <button type="button" :disabled="dateSubmitting" @click="removalConfirmationOpen = false">
            다시 확인
          </button>
          <button type="button" :disabled="dateSubmitting" @click="submitDateChange(true)">
            {{ dateSubmitting ? '변경 중...' : '일정 삭제 후 변경' }}
          </button>
        </div>
      </section>
    </div>
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

.date-editor {
  margin-top: 14px;
}

.date-editor__open {
  width: 100%;
  min-height: 42px;
  color: #e8443a;
  border: 1px solid #ffc2bd;
  border-radius: 12px;
  background: #fff8f7;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.date-editor__form {
  padding: 16px;
  border: 1px solid #ffd0cc;
  border-radius: 16px;
  background: #fffafa;
}

.date-editor__heading,
.date-editor__actions,
.confirmation-dialog__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.date-editor__heading span {
  color: #ff5a4e;
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.date-editor__heading h3 {
  margin: 3px 0 0;
  color: #334155;
  font-size: 16px;
}

.date-editor__heading > button {
  width: 32px;
  height: 32px;
  color: #64748b;
  border: 0;
  border-radius: 9px;
  background: #f1f5f9;
  font-size: 20px;
  cursor: pointer;
}

.date-editor__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.date-editor__grid label {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.date-editor__grid label > span {
  color: #64748b;
  font-size: 11px;
  font-weight: 750;
}

.date-editor__grid input {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  padding: 0 9px;
  color: #334155;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
}

.date-editor__notice,
.date-editor__error {
  margin: 11px 0 0;
  font-size: 11px;
  line-height: 1.55;
  word-break: keep-all;
}

.date-editor__notice {
  color: #64748b;
}

.date-editor__error {
  color: #b91c1c;
}

.date-editor__actions {
  justify-content: flex-end;
  margin-top: 14px;
}

.date-editor__actions button,
.confirmation-dialog__actions button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
}

.date-editor__actions button:last-child,
.confirmation-dialog__actions button:last-child {
  color: #fff;
  border-color: #ff5a4e;
  background: #ff5a4e;
}

.date-editor__actions button:disabled,
.confirmation-dialog__actions button:disabled {
  cursor: wait;
  opacity: 0.65;
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

.schedule-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  border: 1px solid #e8edf3;
  border-radius: 13px;
  background: #fff;
}

.schedule-card > img {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  object-fit: cover;
}

.schedule-card__body {
  display: grid;
  min-width: 0;
  gap: 4px;
  align-content: center;
}

.schedule-card__body > span {
  color: #94a3b8;
  font-size: 10px;
  font-weight: 750;
}

.schedule-card__body > strong {
  overflow: hidden;
  color: #263247;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-card__body > p {
  overflow: hidden;
  margin: 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-card__body > .schedule-card__description {
  display: -webkit-box;
  overflow: hidden;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
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

.confirmation-backdrop {
  position: fixed;
  z-index: 50;
  inset: 0;
  display: grid;
  padding: 20px;
  place-items: center;
  background: rgb(15 23 42 / 48%);
  backdrop-filter: blur(4px);
}

.confirmation-dialog {
  width: min(100%, 440px);
  padding: 30px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 28px 80px rgb(15 23 42 / 24%);
}

.confirmation-dialog__icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: #b91c1c;
  border-radius: 50%;
  background: #fee2e2;
  font-size: 20px;
  font-weight: 850;
}

.confirmation-dialog h2 {
  margin: 18px 0 0;
  color: #1e293b;
  font-size: 21px;
  letter-spacing: -0.03em;
}

.confirmation-dialog p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
  word-break: keep-all;
}

.confirmation-dialog__actions {
  justify-content: flex-end;
  margin-top: 24px;
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

  .date-editor__grid {
    grid-template-columns: 1fr;
  }

  .map-search {
    top: 16px;
    right: 16px;
    width: calc(100% - 32px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner,
  .day-tab,
  .date-editor__open {
    animation: none;
    transition: none;
  }
}
</style>
