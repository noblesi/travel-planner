<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'

import { usePlanEditorStore } from '@/stores/planEditor'

const emit = defineEmits(['busy-change'])
const editorStore = usePlanEditorStore()
const { plan, isSaving } = storeToRefs(editorStore)

const editingDates = ref(false)
const dateSubmitting = ref(false)
const dateError = ref('')
const editStartDate = ref('')
const editEndDate = ref('')
const removalConfirmationOpen = ref(false)
const pendingDatePayload = ref(null)
const editingMetadata = ref(false)
const metadataSubmitting = ref(false)
const metadataError = ref('')
const editTitle = ref('')
const editVisibility = ref('PRIVATE')
const busy = computed(() => dateSubmitting.value || metadataSubmitting.value)
const today = todayInKorea()
const isCompletedPlan = computed(() => Boolean(plan.value?.endDate && plan.value.endDate < today))
const isOngoingPlan = computed(() =>
  Boolean(
    plan.value?.startDate &&
      plan.value?.endDate &&
      plan.value.startDate <= today &&
      plan.value.endDate >= today,
  ),
)

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

function todayInKorea(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  return `${values.year}-${values.month}-${values.day}`
}

function syncDateForm(planValue = plan.value) {
  editStartDate.value = planValue?.startDate ?? ''
  editEndDate.value = planValue?.endDate ?? ''
}

function syncMetadataForm(planValue = plan.value) {
  editTitle.value = planValue?.title ?? ''
  editVisibility.value = planValue?.visibility ?? 'PRIVATE'
}

function openMetadataEditor() {
  closeDateEditor()
  syncMetadataForm()
  metadataError.value = ''
  editingMetadata.value = true
}

function closeMetadataEditor() {
  editingMetadata.value = false
  metadataError.value = ''
  syncMetadataForm()
}

function validateMetadata() {
  const normalizedTitle = editTitle.value.trim()
  if (!normalizedTitle) return '플랜 제목을 입력해 주세요.'
  if (normalizedTitle.length > 200) return '플랜 제목은 200자 이하로 입력해 주세요.'
  if (!['PUBLIC', 'PRIVATE'].includes(editVisibility.value)) {
    return '공개 범위를 다시 선택해 주세요.'
  }
  return ''
}

function metadataApiErrorMessage(error) {
  if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
    return '다른 변경이 먼저 저장되어 최신 플랜 정보를 불러왔습니다. 입력 내용을 확인한 뒤 다시 저장해 주세요.'
  }
  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '플랜 정보를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

async function submitMetadataChange() {
  if (metadataSubmitting.value) return
  const validationMessage = validateMetadata()
  if (validationMessage) {
    metadataError.value = validationMessage
    return
  }

  const normalizedTitle = editTitle.value.trim()
  if (normalizedTitle === plan.value.title && editVisibility.value === plan.value.visibility) {
    closeMetadataEditor()
    return
  }

  metadataSubmitting.value = true
  metadataError.value = ''
  try {
    const data = await editorStore.savePlanMetadata({
      title: normalizedTitle,
      visibility: editVisibility.value,
      versionNo: plan.value.versionNo,
    })
    syncMetadataForm(data.plan)
    editingMetadata.value = false
  } catch (error) {
    metadataError.value = metadataApiErrorMessage(error)
  } finally {
    metadataSubmitting.value = false
  }
}

function openDateEditor() {
  closeMetadataEditor()
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
  if (!editStartDate.value || !editEndDate.value) return '시작일과 종료일을 모두 선택해 주세요.'
  if (isCompletedPlan.value) return '종료된 여행 플랜의 날짜는 변경할 수 없습니다.'
  if (isOngoingPlan.value && editStartDate.value !== plan.value.startDate) {
    return '진행 중인 여행의 시작일은 변경할 수 없습니다.'
  }
  if (!isOngoingPlan.value && editStartDate.value < today) {
    return '여행 시작일은 오늘보다 빠를 수 없습니다.'
  }
  if (isOngoingPlan.value && editEndDate.value < today) {
    return '진행 중인 여행의 종료일은 오늘보다 빠를 수 없습니다.'
  }
  if (editStartDate.value > editEndDate.value) return '종료일은 시작일보다 빠를 수 없습니다.'

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

watch(
  plan,
  (planValue) => {
    if (!editingDates.value) syncDateForm(planValue)
    if (!editingMetadata.value) syncMetadataForm(planValue)
  },
  { immediate: true },
)
watch(busy, (value) => emit('busy-change', value), { immediate: true })
onBeforeUnmount(() => emit('busy-change', false))
</script>

<template>
  <div class="plan-editor-settings">
    <section
      class="metadata-editor"
      :aria-label="editingMetadata ? undefined : '플랜 정보 변경'"
      :aria-labelledby="editingMetadata ? 'metadata-editor-heading' : undefined"
    >
      <button
        v-if="!editingMetadata"
        class="metadata-editor__open"
        type="button"
        :disabled="isSaving || dateSubmitting"
        @click="openMetadataEditor"
      >
        플랜 제목·공개 범위 변경
      </button>
      <form v-else class="metadata-editor__form" @submit.prevent="submitMetadataChange">
        <div class="metadata-editor__heading">
          <div>
            <span>PLAN SETTINGS</span>
            <h3 id="metadata-editor-heading">플랜 정보 변경</h3>
          </div>
          <button
            type="button"
            aria-label="플랜 정보 변경 닫기"
            :disabled="metadataSubmitting"
            @click="closeMetadataEditor"
          >
            ×
          </button>
        </div>
        <div class="metadata-editor__fields">
          <label
            ><span>플랜 제목</span
            ><input
              v-model="editTitle"
              name="editTitle"
              type="text"
              maxlength="200"
              autocomplete="off"
              :disabled="metadataSubmitting"
          /></label>
          <label
            ><span>공개 범위</span
            ><select v-model="editVisibility" name="editVisibility" :disabled="metadataSubmitting">
              <option value="PRIVATE">비공개</option>
              <option value="PUBLIC">공개</option>
            </select></label
          >
        </div>
        <p class="metadata-editor__notice">공개 플랜은 다른 사용자가 탐색할 수 있습니다.</p>
        <p v-if="metadataError" class="metadata-editor__error" role="alert">{{ metadataError }}</p>
        <div class="metadata-editor__actions">
          <button type="button" :disabled="metadataSubmitting" @click="closeMetadataEditor">
            취소
          </button>
          <button type="submit" :disabled="metadataSubmitting" :aria-busy="metadataSubmitting">
            {{ metadataSubmitting ? '저장 중...' : '플랜 정보 저장' }}
          </button>
        </div>
      </form>
    </section>

    <section
      class="date-editor"
      :aria-label="editingDates ? undefined : '여행 날짜 변경'"
      :aria-labelledby="editingDates ? 'date-editor-heading' : undefined"
    >
      <button
        v-if="!editingDates"
        class="date-editor__open"
        type="button"
        :disabled="isSaving || metadataSubmitting || isCompletedPlan"
        @click="openDateEditor"
      >
        여행 날짜 변경
      </button>
      <p v-if="isCompletedPlan && !editingDates" class="date-editor__locked-notice">
        종료된 여행은 날짜를 변경할 수 없습니다.
      </p>
      <form v-if="editingDates" class="date-editor__form" @submit.prevent="submitDateChange()">
        <div class="date-editor__heading">
          <div>
            <span>DATE SETTINGS</span>
            <h3 id="date-editor-heading">여행 날짜 변경</h3>
          </div>
          <button
            type="button"
            aria-label="날짜 변경 닫기"
            :disabled="dateSubmitting"
            @click="closeDateEditor"
          >
            ×
          </button>
        </div>
        <div class="date-editor__grid">
          <label
            ><span>시작일</span
            ><input
              v-model="editStartDate"
              name="editStartDate"
              type="date"
              :min="isOngoingPlan ? undefined : today"
              :disabled="dateSubmitting || isOngoingPlan"
          /></label>
          <label
            ><span>종료일</span
            ><input
              v-model="editEndDate"
              name="editEndDate"
              type="date"
              :min="isOngoingPlan ? today : editStartDate || today"
              :max="maxEditableEndDate"
              :disabled="dateSubmitting"
          /></label>
        </div>
        <p class="date-editor__notice">
          같은 기간으로 이동하면 일정이 함께 이동하고, 제외되는 날짜의 일정은 확인 후 삭제됩니다.
        </p>
        <p v-if="dateError" class="date-editor__error" role="alert">{{ dateError }}</p>
        <div class="date-editor__actions">
          <button type="button" :disabled="dateSubmitting" @click="closeDateEditor">취소</button>
          <button type="submit" :disabled="dateSubmitting" :aria-busy="dateSubmitting">
            {{ dateSubmitting ? '변경 중...' : '날짜 저장' }}
          </button>
        </div>
      </form>
    </section>

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
.metadata-editor {
  margin-top: 14px;
}
.date-editor {
  margin-top: 10px;
}
.date-editor__locked-notice {
  margin: 7px 2px 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.5;
}
.metadata-editor__open,
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
.metadata-editor__open:disabled,
.date-editor__open:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}
.metadata-editor__form,
.date-editor__form {
  padding: 16px;
  border: 1px solid #ffd0cc;
  border-radius: 16px;
  background: #fffafa;
}
.metadata-editor__heading,
.metadata-editor__actions,
.date-editor__heading,
.date-editor__actions,
.confirmation-dialog__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.metadata-editor__heading span,
.date-editor__heading span {
  color: #ff5a4e;
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.12em;
}
.metadata-editor__heading h3,
.date-editor__heading h3 {
  margin: 3px 0 0;
  color: #334155;
  font-size: 16px;
}
.metadata-editor__heading > button,
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
.metadata-editor__fields {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.metadata-editor__fields label,
.date-editor__grid label {
  display: grid;
  min-width: 0;
  gap: 6px;
}
.metadata-editor__fields label > span,
.date-editor__grid label > span {
  color: #64748b;
  font-size: 11px;
  font-weight: 750;
}
.metadata-editor__fields input,
.metadata-editor__fields select,
.date-editor__grid input {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  padding: 0 10px;
  color: #334155;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  font: inherit;
  font-size: 12px;
}
.metadata-editor__notice,
.metadata-editor__error,
.date-editor__notice,
.date-editor__error {
  margin: 11px 0 0;
  font-size: 11px;
  line-height: 1.55;
  word-break: keep-all;
}
.metadata-editor__notice,
.date-editor__notice {
  color: #64748b;
}
.metadata-editor__error,
.date-editor__error {
  color: #b91c1c;
}
.metadata-editor__actions,
.date-editor__actions,
.confirmation-dialog__actions {
  justify-content: flex-end;
  margin-top: 14px;
}
.metadata-editor__actions button,
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
.metadata-editor__actions button:last-child,
.date-editor__actions button:last-child,
.confirmation-dialog__actions button:last-child {
  color: #fff;
  border-color: #ff5a4e;
  background: #ff5a4e;
}
.metadata-editor__actions button:disabled,
.date-editor__actions button:disabled,
.confirmation-dialog__actions button:disabled {
  cursor: wait;
  opacity: 0.65;
}
.date-editor__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
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
  margin-top: 24px;
}
@media (max-width: 520px) {
  .date-editor__grid {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .date-editor__open {
    transition: none;
  }
}
</style>
