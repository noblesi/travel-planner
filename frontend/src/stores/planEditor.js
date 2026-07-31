import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  addScheduleItem,
  deleteScheduleItem,
  getTravelPlanEditor,
  reorderScheduleItems,
  updateScheduleItem,
  updateTravelPlanDates,
} from '@/api/plans'

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
  const status = ref('idle')
  const errorMessage = ref('')
  const plan = ref(null)
  const days = ref([])
  const selectedDayId = ref(null)

  const saveStatus = ref('idle')
  const saveMessage = ref('자동 저장 준비')
  const saveErrorMessage = ref('')
  const pendingSaveCount = ref(0)

  let queueTail = Promise.resolve()
  const lastFailedOperation = ref(null)
  let saveGeneration = 0

  const isLoading = computed(() => status.value === 'loading')
  const isEmpty = computed(() => status.value === 'empty')
  const hasError = computed(() => status.value === 'error')
  const isReady = computed(() => status.value === 'success' || status.value === 'empty')
  const isSaving = computed(() => saveStatus.value === 'saving')
  const hasSaveError = computed(
    () => saveStatus.value === 'error' || saveStatus.value === 'conflict',
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
    lastFailedOperation.value = null
    pendingSaveCount.value = 0
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

  async function savePlanDates(payload) {
    const preferredDayId = selectedDayId.value
    const data = await updateTravelPlanDates(plan.value.planId, payload)
    applyEditorData(data, preferredDayId)
    return data
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
    pendingSaveCount.value += 1
    saveStatus.value = 'saving'
    saveMessage.value = `자동 저장 대기 · ${pendingSaveCount.value}건`

    const task = queueTail.then(() => executeScheduleOperation(operation, generation))
    queueTail = task.catch(() => undefined)

    return task.finally(() => {
      if (generation !== saveGeneration) return

      pendingSaveCount.value = Math.max(0, pendingSaveCount.value - 1)
      if (pendingSaveCount.value > 0) {
        saveStatus.value = 'saving'
        saveMessage.value = `자동 저장 대기 · ${pendingSaveCount.value}건`
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
  ) {
    const operationId = createOperationId()
    const planId = plan.value.planId
    const operation = {
      label: timeSlot === 'MORNING' ? '오전으로 이동' : '오후로 이동',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        return updateScheduleItem(planId, planDayId, scheduleItemId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          itemVersion: item.itemVersion,
          timeSlot,
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
    saveErrorMessage.value = ''
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
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
    addPlaceToSchedule,
    moveScheduleItemTimeSlot,
    removeScheduleItem,
    moveScheduleItemPosition,
    retryLastSave,
    discardFailedSave,
  }
})
