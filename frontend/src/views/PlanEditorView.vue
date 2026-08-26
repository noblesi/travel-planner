<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'

import PlanEditorMapWorkspace from '@/components/plan/PlanEditorMapWorkspace.vue'
import PlanEditorSchedulePanel from '@/components/plan/PlanEditorSchedulePanel.vue'
import PlanEditorToolbar from '@/components/plan/PlanEditorToolbar.vue'
import { usePlanEditorLeaveGuard } from '@/composables/usePlanEditorLeaveGuard'
import { usePlanEditorPanelResize } from '@/composables/usePlanEditorPanelResize'
import { usePlanEditorScheduleInteractions } from '@/composables/usePlanEditorScheduleInteractions'
import { usePlanEditorStore } from '@/stores/planEditor'
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

const {
  selectedScheduleItemId,
  addSearchPlace,
  moveScheduleItem,
  selectScheduleItem,
  startScheduleDrag,
  endScheduleDrag,
  dropSchedule,
  dropScheduleBefore,
  moveScheduleItemPosition,
  removeScheduleItem,
  retryScheduleSave,
} = usePlanEditorScheduleInteractions({
  editorStore,
  toastStore,
  selectedDay,
  selectedDayId,
})
const { schedulePanelWidth, startPanelResize, adjustPanelWidth } = usePlanEditorPanelResize()

usePlanEditorLeaveGuard({
  isSaving,
  hasUnsavedChanges,
  waitForPendingSaves: () => editorStore.waitForPendingSaves(),
})

function retryLoad() {
  return editorStore.loadPlanEditor(props.planId)
}

async function togglePublication() {
  if (!plan.value?.canManagePlan || publicationBusy.value) return
  publicationBusy.value = true
  const targetStatus = plan.value.publishStatus === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  try {
    const data = await editorStore.savePlanPublication(targetStatus)
    if (!data) return
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

watch(() => props.planId, retryLoad, { immediate: true })
onBeforeUnmount(() => {
  editorStore.resetEditor()
})
</script>

<template>
  <div class="editor-page">
    <a class="editor-skip-link" href="#plan-editor-main">본문 바로가기</a>
    <PlanEditorToolbar
      :plan="plan"
      :days-count="days.length"
      :is-ready="isReady"
      :is-saving="isSaving"
      :save-status="saveStatus"
      :save-message="saveMessage"
      :pending-save-count="pendingSaveCount"
      :publication-busy="publicationBusy"
      @busy-change="settingsBusy = $event"
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
          :settings-busy="settingsBusy"
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
.editor-page {
  min-width: 1180px;
  min-height: 100vh;
  color: #172033;
  background: #eef2f7;
}
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
.editor-skip-link:focus {
  transform: translateY(0);
}
.editor-main {
  display: grid;
  grid-template-columns: var(--schedule-panel-width, 430px) 8px minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}
.editor-main:focus {
  outline: none;
}
.editor-resizer {
  position: relative;
  z-index: 8;
  background: #dce3ec;
  cursor: col-resize;
  outline: none;
}
.editor-resizer::after {
  position: absolute;
  top: 50%;
  left: 2px;
  width: 4px;
  height: 42px;
  border-radius: 999px;
  background: #94a3b8;
  content: '';
  transform: translateY(-50%);
}
.editor-resizer:hover,
.editor-resizer:focus-visible {
  background: var(--color-brand-border);
}
.editor-state {
  display: grid;
  grid-column: 1 / -1;
  min-height: calc(100vh - 106px);
  align-content: center;
  justify-items: center;
  padding: 40px;
  color: #64748b;
  text-align: center;
}
.editor-state__spinner {
  width: 38px;
  height: 38px;
  margin-bottom: 16px;
  border: 4px solid #e2e8f0;
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.editor-state__icon {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 14px;
  place-items: center;
  color: #b91c1c;
  border-radius: 50%;
  background: #fee2e2;
  font-size: 20px;
  font-weight: 850;
}
.editor-state strong {
  color: #334155;
  font-size: 17px;
}
.editor-state p {
  margin: 8px 0 0;
  font-size: 12px;
}
.editor-state button {
  min-height: 40px;
  margin-top: 18px;
  padding: 0 16px;
  color: var(--color-brand-on);
  border: 0;
  border-radius: 10px;
  background: var(--color-brand);
  font-weight: 800;
  cursor: pointer;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .editor-state__spinner {
    animation: none;
  }
}
</style>
