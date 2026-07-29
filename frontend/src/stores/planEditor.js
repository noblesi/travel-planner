import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getTravelPlanEditor, updateTravelPlanDates } from '@/api/plans'

function apiErrorMessage(error) {
  if (error?.response?.status === 401) {
    return '로그인 후 여행 계획을 편집할 수 있습니다.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '여행 계획을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export const usePlanEditorStore = defineStore('planEditor', () => {
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

  function resetEditor() {
    status.value = 'idle'
    errorMessage.value = ''
    plan.value = null
    days.value = []
    selectedDayId.value = null
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

  async function savePlanDates(payload) {
    const preferredDayId = selectedDayId.value
    const data = await updateTravelPlanDates(plan.value.planId, payload)
    applyEditorData(data, preferredDayId)
    return data
  }

  return {
    status,
    errorMessage,
    plan,
    days,
    selectedDayId,
    isLoading,
    isEmpty,
    hasError,
    isReady,
    selectedDay,
    scheduleItems,
    isSelectedDayEmpty,
    morningItems,
    afternoonItems,
    selectDay,
    resetEditor,
    loadPlanEditor,
    savePlanDates,
  }
})
