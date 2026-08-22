<script setup>
import { computed, nextTick, ref, watch } from 'vue'

import PlanDateRemovalDialog from '@/components/plan/PlanDateRemovalDialog.vue'
import { usePlanEditorStore } from '@/stores/planEditor'
import { addDaysToDate, todayInKorea } from '@/utils/travelDate'
import {
  dateSaveErrorMessage,
  getPlanDateStatus,
  validatePlanDates,
} from '@/utils/planEditorSettings'

const props = defineProps({
  plan: { type: Object, default: null },
  open: { type: Boolean, required: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['request-open', 'close', 'busy-change'])
const editorStore = usePlanEditorStore()

const submitting = ref(false)
const errorMessage = ref('')
const startDate = ref('')
const endDate = ref('')
const removalConfirmationOpen = ref(false)
const pendingPayload = ref(null)
const submitButton = ref(null)
const today = todayInKorea()

const dateStatus = computed(() => getPlanDateStatus(props.plan, today))
const isCompletedPlan = computed(() => dateStatus.value.isCompleted)
const isOngoingPlan = computed(() => dateStatus.value.isOngoing)
const maxEditableEndDate = computed(() => {
  if (!startDate.value) return undefined
  return addDaysToDate(startDate.value, 13)
})

function syncForm(plan = props.plan) {
  startDate.value = plan?.startDate ?? ''
  endDate.value = plan?.endDate ?? ''
}

function closeEditor() {
  if (submitting.value) return
  emit('close')
}

function resetEditor() {
  errorMessage.value = ''
  removalConfirmationOpen.value = false
  pendingPayload.value = null
  syncForm()
  editorStore.clearDirectSaveFailure()
}

function openRemovalConfirmation(payload) {
  pendingPayload.value = payload
  removalConfirmationOpen.value = true
}

function closeRemovalConfirmation({ restoreFocus = true } = {}) {
  removalConfirmationOpen.value = false
  if (restoreFocus) nextTick(() => submitButton.value?.focus())
}

async function submitChange(force = false) {
  if (submitting.value) return

  const validationMessage = validatePlanDates({
    startDate: startDate.value,
    endDate: endDate.value,
    plan: props.plan,
    today,
  })
  if (validationMessage) {
    errorMessage.value = validationMessage
    return
  }

  const payload = force
    ? pendingPayload.value
    : {
        startDate: startDate.value,
        endDate: endDate.value,
        versionNo: props.plan.versionNo,
        force: false,
      }
  if (!payload) return

  submitting.value = true
  errorMessage.value = ''
  try {
    const data = await editorStore.savePlanDates({ ...payload, force })
    if (!data) {
      emit('close')
      return
    }
    syncForm(data.plan)
    removalConfirmationOpen.value = false
    pendingPayload.value = null
    emit('close')
  } catch (error) {
    if (error?.response?.data?.code === 'PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED') {
      openRemovalConfirmation(payload)
    } else {
      closeRemovalConfirmation({ restoreFocus: removalConfirmationOpen.value })
      pendingPayload.value = null
      errorMessage.value = dateSaveErrorMessage(error)
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.open,
  (open, wasOpen) => {
    if (open && !wasOpen) syncForm()
    if (!open && wasOpen) resetEditor()
  },
)
watch(
  () => props.plan,
  (plan) => {
    if (!props.open) syncForm(plan)
  },
  { immediate: true },
)
watch(submitting, (value) => emit('busy-change', value), { immediate: true })
</script>

<template>
  <section
    class="date-editor"
    :aria-label="open ? undefined : '여행 날짜 변경'"
    :aria-labelledby="open ? 'date-editor-heading' : undefined"
  >
    <button
      v-if="!open"
      class="date-editor__open"
      type="button"
      :disabled="disabled || isCompletedPlan"
      @click="$emit('request-open')"
    >
      여행 날짜 변경
    </button>
    <p v-if="isCompletedPlan && !open" class="date-editor__locked-notice">
      종료된 여행은 날짜를 변경할 수 없습니다.
    </p>
    <form v-if="open" class="date-editor__form" @submit.prevent="submitChange()">
      <div class="date-editor__heading">
        <div>
          <span>DATE SETTINGS</span>
          <h3 id="date-editor-heading">여행 날짜 변경</h3>
        </div>
        <button
          type="button"
          aria-label="날짜 변경 닫기"
          :disabled="submitting"
          @click="closeEditor"
        >
          ×
        </button>
      </div>
      <div class="date-editor__grid">
        <label
          ><span>시작일</span
          ><input
            v-model="startDate"
            name="editStartDate"
            type="date"
            :min="isOngoingPlan ? undefined : today"
            :disabled="submitting || isOngoingPlan"
        /></label>
        <label
          ><span>종료일</span
          ><input
            v-model="endDate"
            name="editEndDate"
            type="date"
            :min="isOngoingPlan ? today : startDate || today"
            :max="maxEditableEndDate"
            :disabled="submitting"
        /></label>
      </div>
      <p class="date-editor__notice">
        같은 기간으로 이동하면 일정이 함께 이동하고, 제외되는 날짜의 일정은 확인 후 삭제됩니다.
      </p>
      <p v-if="errorMessage" class="date-editor__error" role="alert">{{ errorMessage }}</p>
      <div class="date-editor__actions">
        <button type="button" :disabled="submitting" @click="closeEditor">취소</button>
        <button ref="submitButton" type="submit" :disabled="submitting" :aria-busy="submitting">
          {{ submitting ? '변경 중...' : '날짜 저장' }}
        </button>
      </div>
    </form>

    <PlanDateRemovalDialog
      :open="removalConfirmationOpen"
      :busy="submitting"
      @close="closeRemovalConfirmation()"
      @confirm="submitChange(true)"
    />
  </section>
</template>

<style scoped>
.date-editor { margin-top: 10px; }
.date-editor__locked-notice {
  margin: 7px 2px 0; color: #64748b; font-size: 11px; line-height: 1.5;
}
.date-editor__open {
  width: 100%; min-height: 42px; color: var(--color-brand);
  border: 1px solid var(--color-brand-border); border-radius: 12px;
  background: var(--color-brand-soft); font-size: 13px; font-weight: 800; cursor: pointer;
}
.date-editor__open:disabled { cursor: not-allowed; opacity: 0.48; }
.date-editor__form {
  padding: 16px; border: 1px solid var(--color-brand-border);
  border-radius: 16px; background: var(--color-brand-soft);
}
.date-editor__heading,
.date-editor__actions {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.date-editor__heading span {
  color: var(--color-brand); font-size: 9px; font-weight: 850; letter-spacing: 0.12em;
}
.date-editor__heading h3 { margin: 3px 0 0; color: #334155; font-size: 16px; }
.date-editor__heading > button {
  width: 32px; height: 32px; color: #64748b; border: 0; border-radius: 9px;
  background: #f1f5f9; font-size: 20px; cursor: pointer;
}
.date-editor__grid {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 14px;
}
.date-editor__grid label { display: grid; min-width: 0; gap: 6px; }
.date-editor__grid label > span { color: #64748b; font-size: 11px; font-weight: 750; }
.date-editor__grid input {
  width: 100%; min-width: 0; min-height: 40px; padding: 0 10px; color: #334155;
  border: 1px solid #d8dee8; border-radius: 10px; background: #fff;
  font: inherit; font-size: 12px;
}
.date-editor__notice,
.date-editor__error {
  margin: 11px 0 0; font-size: 11px; line-height: 1.55; word-break: keep-all;
}
.date-editor__notice { color: #64748b; }
.date-editor__error { color: #b91c1c; }
.date-editor__actions { justify-content: flex-end; margin-top: 14px; }
.date-editor__actions button {
  min-height: 38px; padding: 0 14px; border: 1px solid #d8dee8;
  border-radius: 10px; background: #fff; font-size: 12px; font-weight: 750; cursor: pointer;
}
.date-editor__actions button:last-child {
  color: #fff; border-color: var(--color-brand); background: var(--color-brand);
}
.date-editor__actions button:disabled { cursor: wait; opacity: 0.65; }
@media (prefers-reduced-motion: reduce) {
  .date-editor__open { transition: none; }
}
</style>
