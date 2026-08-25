import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getTravelPlanEditor,
  updatePlanPublication,
  updateTravelPlanDates,
  updateTravelPlanMetadata,
} from '@/api/plans'
import { usePlanSearchStore } from '@/stores/planSearch'
import { createLocalScheduleError, planEditorErrorMessage } from '@/stores/planEditor/errors'
import { createPlanEditorSaveCoordinator } from '@/stores/planEditor/saveCoordinator'
import { createPlanEditorScheduleActions } from '@/stores/planEditor/scheduleActions'

export const usePlanEditorStore = defineStore('planEditor', () => {
  const planSearchStore = usePlanSearchStore()
  const status = ref('idle')
  const errorMessage = ref('')
  const plan = ref(null)
  const days = ref([])
  const selectedDayId = ref(null)

  const isLoading = computed(() => status.value === 'loading')
  const isEmpty = computed(() => status.value === 'empty')
  const hasError = computed(() => status.value === 'error')
  const isReady = computed(() => status.value === 'success' || status.value === 'empty')
  const selectedDay = computed(
    () => days.value.find((day) => day.planDayId === selectedDayId.value) ?? null,
  )
  const scheduleItems = computed(() =>
    Array.isArray(selectedDay.value?.items) ? selectedDay.value.items : [],
  )
  const isSelectedDayEmpty = computed(() => scheduleItems.value.length === 0)
  const morningItems = computed(() =>
    scheduleItems.value
      .filter((item) => item.timeSlot === 'MORNING')
      .sort((left, right) => left.positionNo - right.positionNo),
  )
  const afternoonItems = computed(() =>
    scheduleItems.value
      .filter((item) => item.timeSlot === 'AFTERNOON')
      .sort((left, right) => left.positionNo - right.positionNo),
  )

  function selectDay(planDayId) {
    if (!days.value.some((day) => day.planDayId === planDayId)) return false

    selectedDayId.value = planDayId
    return true
  }

  function applyEditorData(data, preferredDayId = null) {
    const loadedDays = Array.isArray(data.days) ? data.days : []

    plan.value = data.plan
    days.value = loadedDays
    selectedDayId.value = loadedDays.some((day) => day.planDayId === preferredDayId)
      ? preferredDayId
      : (loadedDays[0]?.planDayId ?? null)
    status.value = loadedDays.every((day) => !Array.isArray(day.items) || day.items.length === 0)
      ? 'empty'
      : 'success'
  }

  function invalidatePublicSearch(data, force = false) {
    if (force || data?.plan?.publishStatus === 'PUBLISHED') {
      planSearchStore.invalidateCache()
    }
  }

  async function refreshPlanEditor(
    preferredDayId = selectedDayId.value,
    shouldApply = () => true,
    targetPlanId = plan.value.planId,
  ) {
    const data = await getTravelPlanEditor(targetPlanId)
    if (shouldApply()) applyEditorData(data, preferredDayId)
    return data
  }

  const saveCoordinator = createPlanEditorSaveCoordinator({
    applyScheduleResult(editor, preferredDayId) {
      applyEditorData(editor, preferredDayId)
      invalidatePublicSearch(editor)
    },
    refreshEditor: refreshPlanEditor,
  })
  const {
    saveStatus,
    saveMessage,
    saveErrorMessage,
    pendingSaveCount,
    isSaving,
    hasSaveError,
    hasUnsavedChanges,
    canRetrySave,
    reset: resetSaveState,
    isCurrent: isCurrentSaveGeneration,
    trackDirectSave,
    enqueueScheduleOperation,
    retryLastSave,
    discardFailedSave,
    clearDirectSaveFailure,
    waitForPendingSaves,
  } = saveCoordinator

  const scheduleActions = createPlanEditorScheduleActions({
    plan,
    days,
    selectedDayId,
    enqueueOperation: enqueueScheduleOperation,
  })
  const {
    addPlaceToSchedule,
    moveScheduleItemTimeSlot,
    moveScheduleItemToEnd,
    moveScheduleItemBefore,
    removeScheduleItem,
    moveScheduleItemPosition,
  } = scheduleActions

  function resetEditor() {
    status.value = 'idle'
    errorMessage.value = ''
    plan.value = null
    days.value = []
    selectedDayId.value = null
    resetSaveState()
  }

  async function loadPlanEditor(planId) {
    const loadGeneration = resetSaveState()
    status.value = 'loading'
    errorMessage.value = ''
    plan.value = null
    days.value = []
    selectedDayId.value = null

    try {
      const data = await getTravelPlanEditor(planId)
      if (!isCurrentSaveGeneration(loadGeneration)) return null
      applyEditorData(data)
      return data
    } catch (error) {
      if (!isCurrentSaveGeneration(loadGeneration)) return null
      status.value = 'error'
      errorMessage.value = planEditorErrorMessage(error)
      return null
    }
  }

  function captureEditorContext() {
    return {
      planId: plan.value.planId,
      preferredDayId: selectedDayId.value,
    }
  }

  async function savePlanChange(request, payload, context, isCurrent) {
    if (!isCurrent()) return null

    try {
      const data = await request(context.planId, payload)
      if (!isCurrent()) return null

      applyEditorData(data, context.preferredDayId)
      invalidatePublicSearch(data)
      return data
    } catch (error) {
      if (!isCurrent()) return null
      if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
        try {
          await refreshPlanEditor(context.preferredDayId, isCurrent, context.planId)
        } catch {
          // 원래 충돌 응답을 유지해 호출자가 정확한 저장 실패 원인을 표시하게 합니다.
        }
      }
      throw error
    }
  }

  function savePlanDates(payload) {
    const context = captureEditorContext()
    return trackDirectSave('여행 날짜를 저장하고 있습니다.', (isCurrent) =>
      savePlanChange(updateTravelPlanDates, payload, context, isCurrent),
    )
  }

  function savePlanMetadata(payload) {
    const context = captureEditorContext()
    return trackDirectSave('플랜 정보를 저장하고 있습니다.', (isCurrent) =>
      savePlanChange(updateTravelPlanMetadata, payload, context, isCurrent),
    )
  }

  function savePlanPublication(publishStatus) {
    const context = captureEditorContext()
    return trackDirectSave(
      '플랜 공개 상태를 저장하고 있습니다.',
      async (isCurrent) => {
        if (!isCurrent()) return null
        if (hasUnsavedChanges.value) {
          throw createLocalScheduleError('저장되지 않은 변경사항을 해결한 후 다시 시도해 주세요.')
        }

        try {
          const data = await updatePlanPublication(context.planId, {
            publishStatus,
            versionNo: plan.value.versionNo,
          })
          if (!isCurrent()) return null

          applyEditorData(data, context.preferredDayId)
          invalidatePublicSearch(data, true)
          return data
        } catch (error) {
          if (!isCurrent()) return null
          if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
            try {
              await refreshPlanEditor(context.preferredDayId, isCurrent, context.planId)
            } catch {
              // 원래 충돌 응답을 유지합니다.
            }
          }
          throw error
        }
      },
      { updateSaveState: false },
    )
  }

  return {
    status,
    errorMessage,
    plan,
    days,
    selectedDayId,
    saveStatus,
    saveMessage,
    saveErrorMessage,
    pendingSaveCount,
    isLoading,
    isEmpty,
    hasError,
    isReady,
    isSaving,
    hasSaveError,
    hasUnsavedChanges,
    canRetrySave,
    selectedDay,
    scheduleItems,
    isSelectedDayEmpty,
    morningItems,
    afternoonItems,
    selectDay,
    resetEditor,
    loadPlanEditor,
    refreshPlanEditor,
    savePlanDates,
    savePlanMetadata,
    savePlanPublication,
    addPlaceToSchedule,
    moveScheduleItemTimeSlot,
    moveScheduleItemToEnd,
    moveScheduleItemBefore,
    removeScheduleItem,
    moveScheduleItemPosition,
    retryLastSave,
    discardFailedSave,
    clearDirectSaveFailure,
    waitForPendingSaves,
  }
})
