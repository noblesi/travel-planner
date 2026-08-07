import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  addScheduleItem,
  deleteScheduleItem,
  getTravelPlanEditor,
  reorderScheduleItems,
  updateScheduleItem,
  updatePlanPublication,
  updateTravelPlanDates,
  updateTravelPlanMetadata,
} from '@/api/plans'
import { usePlanSearchStore } from '@/stores/planSearch'

const CONFLICT_CODES = new Set([
  'SCHEDULE_VERSION_CONFLICT',
  'ITEM_VERSION_CONFLICT',
  'DUPLICATE_OPERATION',
])

function apiErrorMessage(error) {
  if (error?.response?.status === 401) {
    return '로그인 후 여행 계획을 편집할 수 있습니다.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '여행 계획을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

function scheduleSaveErrorMessage(error, refreshed) {
  const code = error?.response?.data?.code
  if (code === 'SCHEDULE_VERSION_CONFLICT' || code === 'ITEM_VERSION_CONFLICT') {
    return refreshed
      ? '다른 변경이 먼저 저장되어 최신 일정을 다시 불러왔습니다. 작업을 다시 시도해 주세요.'
      : '다른 변경이 먼저 저장되었습니다. 최신 일정을 불러온 뒤 다시 시도해 주세요.'
  }
  if (code === 'DUPLICATE_OPERATION') {
    return '자동 저장 작업 식별자가 충돌했습니다. 최신 일정으로 복구했으니 다시 시도해 주세요.'
  }
  if (code === 'SCHEDULE_ITEM_ALREADY_EXISTS') {
    return '선택한 시간대에 같은 장소가 이미 있습니다.'
  }
  if (code === 'SCHEDULE_ITEM_LIMIT_EXCEEDED') {
    return '시간대별 일정은 최대 100개까지 추가할 수 있습니다.'
  }
  if (code === 'INVALID_SCHEDULE_ORDER') {
    return refreshed
      ? '일정 순서가 달라져 최신 일정을 다시 불러왔습니다.'
      : '일정 순서를 저장하지 못했습니다.'
  }

  const message = error?.response?.data?.message ?? error?.userMessage
  return typeof message === 'string' && message
    ? message
    : '일정을 자동 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

function createOperationId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (token) => {
    const random = Math.floor(Math.random() * 16)
    const value = token === 'x' ? random : (random & 0x3) | 0x8
    return value.toString(16)
  })
}

function localScheduleError(message) {
  const error = new Error(message)
  error.userMessage = message
  return error
}

export const usePlanEditorStore = defineStore('planEditor', () => {
  const planSearchStore = usePlanSearchStore()
  const status = ref('idle')
  const errorMessage = ref('')
  const plan = ref(null)
  const days = ref([])
  const selectedDayId = ref(null)

  const saveStatus = ref('idle')
  const saveMessage = ref('자동 저장 준비')
  const saveErrorMessage = ref('')
  const schedulePendingSaveCount = ref(0)
  const directPendingSaveCount = ref(0)
  const directSaveFailed = ref(false)

  let queueTail = Promise.resolve()
  const directSavePromises = new Set()
  const lastFailedOperation = ref(null)
  let saveGeneration = 0

  const isLoading = computed(() => status.value === 'loading')
  const isEmpty = computed(() => status.value === 'empty')
  const hasError = computed(() => status.value === 'error')
  const isReady = computed(() => status.value === 'success' || status.value === 'empty')
  const pendingSaveCount = computed(
    () => schedulePendingSaveCount.value + directPendingSaveCount.value,
  )
  const isSaving = computed(() => pendingSaveCount.value > 0)
  const hasSaveError = computed(
    () => saveStatus.value === 'error' || saveStatus.value === 'conflict',
  )
  const hasUnsavedChanges = computed(
    () => hasSaveError.value || lastFailedOperation.value != null || directSaveFailed.value,
  )
  const canRetrySave = computed(() => lastFailedOperation.value != null)

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

  function resetSaveState() {
    saveGeneration += 1
    queueTail = Promise.resolve()
    directSavePromises.clear()
    lastFailedOperation.value = null
    schedulePendingSaveCount.value = 0
    directPendingSaveCount.value = 0
    directSaveFailed.value = false
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
    saveErrorMessage.value = ''
  }

  function resetEditor() {
    status.value = 'idle'
    errorMessage.value = ''
    plan.value = null
    days.value = []
    selectedDayId.value = null
    resetSaveState()
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

  async function loadPlanEditor(planId) {
    resetSaveState()
    status.value = 'loading'
    errorMessage.value = ''
    plan.value = null
    days.value = []
    selectedDayId.value = null

    try {
      const data = await getTravelPlanEditor(planId)
      applyEditorData(data)
      return data
    } catch (error) {
      status.value = 'error'
      errorMessage.value = apiErrorMessage(error)
      return null
    }
  }

  async function refreshPlanEditor(preferredDayId = selectedDayId.value) {
    const data = await getTravelPlanEditor(plan.value.planId)
    applyEditorData(data, preferredDayId)
    return data
  }

  function trackDirectSave(label, operation) {
    const generation = saveGeneration
    const previousScheduleFailure = lastFailedOperation.value
      ? {
          status: saveStatus.value,
          message: saveMessage.value,
        }
      : null
    directPendingSaveCount.value += 1
    directSaveFailed.value = false
    saveStatus.value = 'saving'
    saveMessage.value = label

    const task = Promise.resolve().then(operation)
    directSavePromises.add(task)

    return task
      .then((result) => {
        if (generation === saveGeneration) {
          if (previousScheduleFailure && lastFailedOperation.value) {
            saveStatus.value = previousScheduleFailure.status
            saveMessage.value = previousScheduleFailure.message
          } else {
            saveStatus.value = 'saved'
            saveMessage.value = '모든 변경사항이 저장되었습니다.'
          }
        }
        return result
      })
      .catch((error) => {
        if (generation === saveGeneration) {
          directSaveFailed.value = true
          if (previousScheduleFailure && lastFailedOperation.value) {
            saveStatus.value = previousScheduleFailure.status
            saveMessage.value = previousScheduleFailure.message
          } else {
            saveStatus.value = 'error'
            saveMessage.value = '변경사항 저장 실패'
            saveErrorMessage.value = apiErrorMessage(error)
          }
        }
        throw error
      })
      .finally(() => {
        directSavePromises.delete(task)
        if (generation === saveGeneration) {
          directPendingSaveCount.value = Math.max(0, directPendingSaveCount.value - 1)
        }
      })
  }

  function savePlanDates(payload) {
    return trackDirectSave('여행 날짜를 저장하고 있습니다.', async () => {
      const preferredDayId = selectedDayId.value

      try {
        const data = await updateTravelPlanDates(plan.value.planId, payload)
        applyEditorData(data, preferredDayId)
        invalidatePublicSearch(data)
        return data
      } catch (error) {
        if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
          try {
            await refreshPlanEditor(preferredDayId)
          } catch {
            // 원래 충돌 응답을 유지해 사용자가 저장 실패 원인을 확인할 수 있게 합니다.
          }
        }
        throw error
      }
    })
  }

  function savePlanMetadata(payload) {
    return trackDirectSave('플랜 정보를 저장하고 있습니다.', async () => {
      const preferredDayId = selectedDayId.value

      try {
        const data = await updateTravelPlanMetadata(plan.value.planId, payload)
        applyEditorData(data, preferredDayId)
        invalidatePublicSearch(data)
        return data
      } catch (error) {
        if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
          try {
            await refreshPlanEditor(preferredDayId)
          } catch {
            // 원래 충돌 응답을 유지해 호출자가 정확한 원인을 표시하게 합니다.
          }
        }
        throw error
      }
    })
  }

  async function savePlanPublication(publishStatus) {
    const savesReady = await waitForPendingSaves()
    if (!savesReady) {
      throw localScheduleError('저장되지 않은 변경사항을 해결한 후 다시 시도해 주세요.')
    }

    const preferredDayId = selectedDayId.value
    try {
      const data = await updatePlanPublication(plan.value.planId, {
        publishStatus,
        versionNo: plan.value.versionNo,
      })
      applyEditorData(data, preferredDayId)
      invalidatePublicSearch(data, true)
      return data
    } catch (error) {
      if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
        try {
          await refreshPlanEditor(preferredDayId)
        } catch {
          // 원래 충돌 응답을 유지합니다.
        }
      }
      throw error
    }
  }

  function currentDay(planDayId) {
    const day = days.value.find((candidate) => candidate.planDayId === planDayId)
    if (!day) throw localScheduleError('선택한 여행 일차를 찾을 수 없습니다.')
    return day
  }

  function currentItem(day, scheduleItemId) {
    const item = day.items?.find((candidate) => candidate.scheduleItemId === scheduleItemId)
    if (!item) throw localScheduleError('일정 항목이 최신 목록에 없어 작업을 건너뛰었습니다.')
    return item
  }

  async function executeScheduleOperation(operation, generation) {
    if (generation !== saveGeneration) return null

    saveStatus.value = 'saving'
    saveMessage.value = `자동 저장 중 · ${operation.label}`
    if (!lastFailedOperation.value || lastFailedOperation.value === operation) {
      saveErrorMessage.value = ''
    }

    try {
      const result = await operation.run()
      if (generation !== saveGeneration) return result

      if (result?.editor) {
        applyEditorData(result.editor, operation.preferredDayId)
        invalidatePublicSearch(result.editor)
      }
      if (lastFailedOperation.value === operation) lastFailedOperation.value = null
      saveStatus.value = 'saved'
      saveMessage.value = '모든 변경사항이 자동 저장되었습니다.'
      return result
    } catch (error) {
      if (generation !== saveGeneration) throw error

      const code = error?.response?.data?.code
      let refreshed = false
      if (CONFLICT_CODES.has(code) || code === 'INVALID_SCHEDULE_ORDER') {
        try {
          await refreshPlanEditor(operation.preferredDayId)
          refreshed = true
        } catch {
          refreshed = false
        }
      }

      saveStatus.value = CONFLICT_CODES.has(code) ? 'conflict' : 'error'
      saveMessage.value = saveStatus.value === 'conflict' ? '충돌 복구 필요' : '자동 저장 실패'
      saveErrorMessage.value = scheduleSaveErrorMessage(error, refreshed)
      lastFailedOperation.value = code === 'DUPLICATE_OPERATION' ? null : operation
      throw error
    }
  }

  function enqueueScheduleOperation(operation) {
    const generation = saveGeneration
    schedulePendingSaveCount.value += 1
    saveStatus.value = 'saving'
    saveMessage.value = `자동 저장 대기 · ${schedulePendingSaveCount.value}건`

    const task = queueTail.then(() => executeScheduleOperation(operation, generation))
    queueTail = task.catch(() => undefined)

    return task.finally(() => {
      if (generation !== saveGeneration) return

      schedulePendingSaveCount.value = Math.max(0, schedulePendingSaveCount.value - 1)
      if (schedulePendingSaveCount.value > 0) {
        saveStatus.value = 'saving'
        saveMessage.value = `자동 저장 대기 · ${schedulePendingSaveCount.value}건`
      } else if (lastFailedOperation.value) {
        if (saveStatus.value !== 'conflict') {
          saveStatus.value = 'error'
          saveMessage.value = '자동 저장 실패'
        }
      }
    })
  }

  function addPlaceToSchedule(place, timeSlot, planDayId = selectedDayId.value) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: `${place.placeName} 추가`,
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        return addScheduleItem(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot,
          placeProvider: place.placeProvider,
          externalPlaceId: place.externalPlaceId,
          placeName: place.placeName,
          categoryName: place.categoryName ?? null,
          address: place.address ?? null,
          latitude: place.latitude ?? null,
          longitude: place.longitude ?? null,
          imageUrl: place.imageUrl ?? null,
          description: place.description ?? null,
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function moveScheduleItemTimeSlot(
    scheduleItemId,
    timeSlot,
    planDayId = selectedDayId.value,
    targetPlanDayId = planDayId,
  ) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: timeSlot === 'MORNING' ? '오전으로 이동' : '오후로 이동',
      preferredDayId: targetPlanDayId,
      async run() {
        const day = currentDay(planDayId)
        const targetDay = currentDay(targetPlanDayId)
        const item = currentItem(day, scheduleItemId)
        return updateScheduleItem(planId, planDayId, scheduleItemId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          itemVersion: item.itemVersion,
          timeSlot,
          targetPlanDayId: targetPlanDayId === planDayId ? null : String(targetPlanDayId),
          targetScheduleVersion:
            targetPlanDayId === planDayId ? null : targetDay.scheduleVersion,
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function moveScheduleItemToEnd(
    scheduleItemId,
    planDayId = selectedDayId.value,
  ) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: '일정 순서 변경',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const orderedItems = [...(day.items ?? [])]
          .filter((candidate) => candidate.timeSlot === item.timeSlot)
          .sort((left, right) => left.positionNo - right.positionNo)
        const currentIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        if (currentIndex < 0 || currentIndex === orderedItems.length - 1) return null

        const [movedItem] = orderedItems.splice(currentIndex, 1)
        orderedItems.push(movedItem)
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: orderedItems.map((candidate) => candidate.scheduleItemId),
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function moveScheduleItemBefore(
    scheduleItemId,
    targetScheduleItemId,
    planDayId = selectedDayId.value,
  ) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: '일정 순서 변경',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const targetItem = currentItem(day, targetScheduleItemId)
        if (item.timeSlot !== targetItem.timeSlot || scheduleItemId === targetScheduleItemId) {
          return null
        }
        const orderedItems = [...(day.items ?? [])]
          .filter((candidate) => candidate.timeSlot === item.timeSlot)
          .sort((left, right) => left.positionNo - right.positionNo)
        const sourceIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        if (sourceIndex < 0) return null
        const [movedItem] = orderedItems.splice(sourceIndex, 1)
        const targetIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === targetScheduleItemId,
        )
        if (targetIndex < 0) return null
        orderedItems.splice(targetIndex, 0, movedItem)
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: orderedItems.map((candidate) => candidate.scheduleItemId),
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function removeScheduleItem(scheduleItemId, planDayId = selectedDayId.value) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: '일정 삭제',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        return deleteScheduleItem(planId, planDayId, scheduleItemId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          itemVersion: item.itemVersion,
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function moveScheduleItemPosition(
    scheduleItemId,
    direction,
    planDayId = selectedDayId.value,
  ) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: direction < 0 ? '일정 순서 올리기' : '일정 순서 내리기',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const orderedItems = [...(day.items ?? [])]
          .filter((candidate) => candidate.timeSlot === item.timeSlot)
          .sort((left, right) => left.positionNo - right.positionNo)
        const currentIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        const targetIndex = currentIndex + direction
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= orderedItems.length) return null

        const reordered = [...orderedItems]
        ;[reordered[currentIndex], reordered[targetIndex]] = [
          reordered[targetIndex],
          reordered[currentIndex],
        ]
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: reordered.map((candidate) => candidate.scheduleItemId),
        })
      },
    }
    return enqueueScheduleOperation(operation)
  }

  function retryLastSave() {
    if (!lastFailedOperation.value) return Promise.resolve(null)

    const operation = lastFailedOperation.value
    lastFailedOperation.value = null
    return enqueueScheduleOperation(operation)
  }

  function discardFailedSave() {
    lastFailedOperation.value = null
    directSaveFailed.value = false
    saveErrorMessage.value = ''
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
  }

  function clearDirectSaveFailure() {
    if (!directSaveFailed.value) return

    directSaveFailed.value = false
    if (lastFailedOperation.value) return
    saveErrorMessage.value = ''
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
  }

  async function waitForPendingSaves() {
    while (schedulePendingSaveCount.value > 0 || directPendingSaveCount.value > 0) {
      await Promise.allSettled([queueTail, ...directSavePromises])
    }
    return !hasUnsavedChanges.value
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
