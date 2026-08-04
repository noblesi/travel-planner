<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { onBeforeRouteLeave } from 'vue-router'

import PlanEditorMapWorkspace from '@/components/plan/PlanEditorMapWorkspace.vue'
import PlanEditorSchedulePanel from '@/components/plan/PlanEditorSchedulePanel.vue'
import PlanEditorToolbar from '@/components/plan/PlanEditorToolbar.vue'
import { usePlanEditorStore } from '@/stores/planEditor'

const props = defineProps({
  planId: { type: String, required: true },
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
  hasUnsavedChanges,
  canRetrySave,
  saveStatus,
  saveMessage,
  saveErrorMessage,
  pendingSaveCount,
} = storeToRefs(editorStore)

const settingsBusy = ref(false)

function retryLoad() {
  return editorStore.loadPlanEditor(props.planId)
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

function handleBeforeUnload(event) {
  if (!isSaving.value && !hasUnsavedChanges.value) return
  event.preventDefault()
  event.returnValue = ''
}

watch(() => props.planId, retryLoad, { immediate: true })
onBeforeRouteLeave(async () => {
  if (isSaving.value) await editorStore.waitForPendingSaves()
  if (hasUnsavedChanges.value) {
    return window.confirm(
      '저장되지 않은 변경사항이 있습니다. 이 화면을 나가면 입력한 내용이 사라질 수 있습니다. 그래도 나갈까요?',
    )
  }
  return true
})
onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  editorStore.resetEditor()
})
</script>

<template>
  <div class="editor-page">
    <PlanEditorToolbar
      :plan="plan"
      :is-ready="isReady"
      :is-saving="isSaving"
      :save-status="saveStatus"
      :save-message="saveMessage"
      :pending-save-count="pendingSaveCount"
    />

    <main class="editor-main">
      <section v-if="isLoading || status === 'idle'" class="editor-state" aria-live="polite">
        <span class="editor-state__spinner" aria-hidden="true" />
        <strong>여행 계획을 불러오고 있어요.</strong>
        <p>일차와 저장된 일정을 준비하고 있습니다.</p>
      </section>
      <section v-else-if="status === 'error'" class="editor-state editor-state--error" role="alert">
        <span class="editor-state__icon" aria-hidden="true">!</span>
        <strong>{{ errorMessage }}</strong>
        <p>잠시 후 다시 시도하거나 홈으로 돌아가 주세요.</p>
        <button type="button" @click="retryLoad">다시 시도</button>
      </section>
      <template v-else-if="isReady">
        <PlanEditorSchedulePanel
          :plan="plan"
          :days="days"
          :selected-day-id="selectedDayId"
          :selected-day="selectedDay"
          :is-selected-day-empty="isSelectedDayEmpty"
          :morning-items="morningItems"
          :afternoon-items="afternoonItems"
          :has-save-error="hasSaveError"
          :save-status="saveStatus"
          :save-error-message="saveErrorMessage"
          :can-retry-save="canRetrySave"
          @busy-change="settingsBusy = $event"
          @select-day="editorStore.selectDay"
          @retry-save="retryScheduleSave"
          @discard-save="editorStore.discardFailedSave"
          @move-position="moveScheduleItemPosition"
          @move-time-slot="moveScheduleItem"
          @remove="removeScheduleItem"
        />
        <PlanEditorMapWorkspace
          :plan="plan"
          :selected-day="selectedDay"
          :schedule-items="scheduleItems"
          :settings-busy="settingsBusy"
          @add="addSearchPlace"
        />
      </template>
    </main>
  </div>
</template>

<style scoped>
.editor-page { min-height: 100vh; color: #172033; background: #eef2f7; }
.editor-main { display: grid; grid-template-columns: minmax(340px, 430px) minmax(0, 1fr); min-height: calc(100vh - 82px); }
.editor-state {
  display: grid;
  grid-column: 1 / -1;
  min-height: calc(100vh - 82px);
  align-content: center;
  justify-items: center;
  padding: 40px;
  color: #64748b;
  text-align: center;
}
.editor-state__spinner { width: 38px; height: 38px; margin-bottom: 16px; border: 4px solid #e2e8f0; border-top-color: #ff5a4e; border-radius: 50%; animation: spin .8s linear infinite; }
.editor-state__icon { display: grid; width: 44px; height: 44px; margin-bottom: 14px; place-items: center; color: #b91c1c; border-radius: 50%; background: #fee2e2; font-size: 20px; font-weight: 850; }
.editor-state strong { color: #334155; font-size: 17px; }
.editor-state p { margin: 8px 0 0; font-size: 12px; }
.editor-state button { min-height: 40px; margin-top: 18px; padding: 0 16px; color: #fff; border: 0; border-radius: 10px; background: #ff5a4e; font-weight: 800; cursor: pointer; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 880px) {
  .editor-main { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner { animation: none; }
}
</style>
