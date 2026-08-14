<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { onBeforeRouteLeave } from 'vue-router'

import PlanEditorMapWorkspace from '@/components/plan/PlanEditorMapWorkspace.vue'
import PlanEditorSchedulePanel from '@/components/plan/PlanEditorSchedulePanel.vue'
import PlanEditorToolbar from '@/components/plan/PlanEditorToolbar.vue'
import { usePlanEditorStore } from '@/stores/planEditor'
import { readLocalStorage, writeLocalStorage } from '@/utils/browserStorage'
import { useToastStore } from '@/stores/toast'

const props = defineProps({
  planId: { type: String, required: true },
})

const editorStore = usePlanEditorStore()
const toastStore = useToastStore()
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
const publicationBusy = ref(false)
const selectedScheduleItemId = ref(null)
const draggedSchedule = ref(null)
const schedulePanelWidth = ref(Number(readLocalStorage('planEditorPanelWidth')) || 430)
const resizingPanel = ref(false)

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

function selectScheduleItem(item) {
  selectedScheduleItemId.value = item?.scheduleItemId ?? null
}

function startScheduleDrag(item) {
  draggedSchedule.value = {
    item,
    sourcePlanDayId: selectedDayId.value,
    sourceTimeSlot: item.timeSlot,
  }
}

function endScheduleDrag() {
  draggedSchedule.value = null
}

function dropSchedule({ targetPlanDayId, targetTimeSlot }) {
  const dragged = draggedSchedule.value
  if (!dragged || !targetPlanDayId) return
  const nextTimeSlot = targetTimeSlot || dragged.sourceTimeSlot
  const sameDay = String(targetPlanDayId) === String(dragged.sourcePlanDayId)
  const sameSlot = nextTimeSlot === dragged.sourceTimeSlot

  if (sameDay && sameSlot) {
    runScheduleOperation(
      editorStore.moveScheduleItemToEnd(
        dragged.item.scheduleItemId,
        dragged.sourcePlanDayId,
      ),
    )
  } else {
    runScheduleOperation(
      editorStore.moveScheduleItemTimeSlot(
        dragged.item.scheduleItemId,
        nextTimeSlot,
        dragged.sourcePlanDayId,
        targetPlanDayId,
      ),
    )
  }
  selectedScheduleItemId.value = dragged.item.scheduleItemId
  draggedSchedule.value = null
}

function dropScheduleBefore({ targetItem, targetPlanDayId, targetTimeSlot }) {
  const dragged = draggedSchedule.value
  if (!dragged || !targetItem) return
  const sameDay = String(targetPlanDayId) === String(dragged.sourcePlanDayId)
  const sameSlot = targetTimeSlot === dragged.sourceTimeSlot
  if (sameDay && sameSlot) {
    runScheduleOperation(
      editorStore.moveScheduleItemBefore(
        dragged.item.scheduleItemId,
        targetItem.scheduleItemId,
        targetPlanDayId,
      ),
    )
    draggedSchedule.value = null
    return
  }
  dropSchedule({ targetPlanDayId, targetTimeSlot })
}

function moveScheduleItemPosition(item, direction) {
  return runScheduleOperation(editorStore.moveScheduleItemPosition(item.scheduleItemId, direction))
}

async function removeScheduleItem(item) {
  const sourcePlanDayId = selectedDayId.value
  try {
    await editorStore.removeScheduleItem(item.scheduleItemId)
    if (String(selectedScheduleItemId.value) === String(item.scheduleItemId)) {
      selectedScheduleItemId.value = null
    }
    toastStore.show({
      message: `${item.placeName} 일정을 삭제했습니다.`,
      type: 'info',
      duration: 8000,
      actionLabel: '실행 취소',
      action: async () => {
        await editorStore.addPlaceToSchedule(item, item.timeSlot, sourcePlanDayId)
        toastStore.success(`${item.placeName} 일정을 복구했습니다.`)
      },
    })
  } catch {
    return null
  }
  return true
}

function resizePanelTo(clientX) {
  const nextWidth = Math.min(560, Math.max(340, clientX))
  schedulePanelWidth.value = nextWidth
}

function startPanelResize(event) {
  resizingPanel.value = true
  resizePanelTo(event.clientX)
  document.addEventListener('pointermove', handlePanelResize)
  document.addEventListener('pointerup', stopPanelResize, { once: true })
}

function handlePanelResize(event) {
  if (resizingPanel.value) resizePanelTo(event.clientX)
}

function stopPanelResize() {
  resizingPanel.value = false
  writeLocalStorage('planEditorPanelWidth', String(schedulePanelWidth.value))
  document.removeEventListener('pointermove', handlePanelResize)
}

function adjustPanelWidth(event) {
  if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return
  event.preventDefault()
  schedulePanelWidth.value = Math.min(
    560,
    Math.max(340, schedulePanelWidth.value + (event.key === 'ArrowRight' ? 20 : -20)),
  )
  writeLocalStorage('planEditorPanelWidth', String(schedulePanelWidth.value))
}

function retryScheduleSave() {
  return runScheduleOperation(editorStore.retryLastSave())
}

async function togglePublication() {
  if (!plan.value?.canManagePlan || publicationBusy.value) return
  publicationBusy.value = true
  const targetStatus = plan.value.publishStatus === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  try {
    await editorStore.savePlanPublication(targetStatus)
    toastStore.success(
      targetStatus === 'PUBLISHED'
        ? '플랜 제작을 완료했습니다.'
        : '플랜을 작성 중 상태로 전환했습니다.',
    )
  } catch (error) {
    const message = error?.response?.data?.message ?? error?.userMessage
    toastStore.error(
      typeof message === 'string' && message
        ? message
        : '플랜 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    publicationBusy.value = false
  }
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
  document.removeEventListener('pointermove', handlePanelResize)
})
</script>

<template>
  <div class="editor-page">
    <a class="editor-skip-link" href="#plan-editor-main">본문 바로가기</a>
    <PlanEditorToolbar
      :plan="plan"
      :is-ready="isReady"
      :is-saving="isSaving"
      :save-status="saveStatus"
      :save-message="saveMessage"
      :pending-save-count="pendingSaveCount"
      :publication-busy="publicationBusy"
      @toggle-publication="togglePublication"
    />

    <main
      id="plan-editor-main"
      class="editor-main"
      tabindex="-1"
      :style="{ '--schedule-panel-width': `${schedulePanelWidth}px` }"
    >
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
          :selected-schedule-item-id="selectedScheduleItemId"
          @busy-change="settingsBusy = $event"
          @select-day="editorStore.selectDay"
          @retry-save="retryScheduleSave"
          @discard-save="editorStore.discardFailedSave"
          @move-position="moveScheduleItemPosition"
          @move-time-slot="moveScheduleItem"
          @remove="removeScheduleItem"
          @select-item="selectScheduleItem"
          @drag-start="startScheduleDrag"
          @drag-end="endScheduleDrag"
          @drop-schedule="dropSchedule"
          @drop-before="dropScheduleBefore"
        />
        <div
          class="editor-resizer"
          role="separator"
          tabindex="0"
          aria-label="일정 패널 너비 조절"
          aria-orientation="vertical"
          :aria-valuenow="schedulePanelWidth"
          aria-valuemin="340"
          aria-valuemax="560"
          @pointerdown.prevent="startPanelResize"
          @keydown="adjustPanelWidth"
        />
        <PlanEditorMapWorkspace
          :plan="plan"
          :selected-day="selectedDay"
          :schedule-items="scheduleItems"
          :settings-busy="settingsBusy"
          :selected-schedule-item-id="selectedScheduleItemId"
          @add="addSearchPlace"
          @select-schedule="selectScheduleItem"
        />
      </template>
    </main>
  </div>
</template>

<style scoped>
.editor-page { min-width: 1180px; min-height: 100vh; color: #172033; background: #eef2f7; }
.editor-skip-link {
  position: fixed;
  z-index: 1300;
  top: 10px;
  left: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--color-text);
  color: var(--color-brand-on);
  font-weight: 800;
  text-decoration: none;
  transform: translateY(-160%);
}
.editor-skip-link:focus { transform: translateY(0); }
.editor-main { display: grid; grid-template-columns: var(--schedule-panel-width, 430px) 8px minmax(0, 1fr); min-height: calc(100vh - 82px); }
.editor-main:focus { outline: none; }
.editor-resizer { position: relative; z-index: 8; background: #dce3ec; cursor: col-resize; outline: none; }
.editor-resizer::after { position: absolute; top: 50%; left: 2px; width: 4px; height: 42px; border-radius: 999px; background: #94a3b8; content: ''; transform: translateY(-50%); }
.editor-resizer:hover,.editor-resizer:focus-visible { background: var(--color-brand-border); }
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
.editor-state__spinner { width: 38px; height: 38px; margin-bottom: 16px; border: 4px solid #e2e8f0; border-top-color: var(--color-brand); border-radius: 50%; animation: spin .8s linear infinite; }
.editor-state__icon { display: grid; width: 44px; height: 44px; margin-bottom: 14px; place-items: center; color: #b91c1c; border-radius: 50%; background: #fee2e2; font-size: 20px; font-weight: 850; }
.editor-state strong { color: #334155; font-size: 17px; }
.editor-state p { margin: 8px 0 0; font-size: 12px; }
.editor-state button { min-height: 40px; margin-top: 18px; padding: 0 16px; color: var(--color-brand-on); border: 0; border-radius: 10px; background: var(--color-brand); font-weight: 800; cursor: pointer; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner { animation: none; }
}
</style>
