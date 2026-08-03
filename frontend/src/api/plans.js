import http from './http'

export async function searchPublicPlans({ keyword = '', limit = 100 } = {}) {
  const response = await http.get('/plans', { params: { keyword, limit } })
  return response.data.data
}

export async function getPublicTravelPlan(planId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.get(`/plans/${encodedPlanId}`)
  return response.data.data
}

export async function createTravelPlan(payload) {
  const response = await http.post('/plans', payload)
  return response.data.data
}

export async function getTravelPlanEditor(planId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.get(`/plans/${encodedPlanId}/editor`)
  return response.data.data
}

export async function updateTravelPlanMetadata(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.patch(`/plans/${encodedPlanId}`, payload)
  return response.data.data
}

export async function updateTravelPlanDates(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.patch(`/plans/${encodedPlanId}/dates`, payload)
  return response.data.data
}

function scheduleItemsPath(planId, planDayId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const encodedPlanDayId = encodeURIComponent(String(planDayId))
  return `/plans/${encodedPlanId}/days/${encodedPlanDayId}/items`
}

export async function addScheduleItem(planId, planDayId, payload) {
  const response = await http.post(scheduleItemsPath(planId, planDayId), payload)
  return response.data.data
}

export async function updateScheduleItem(planId, planDayId, scheduleItemId, payload) {
  const encodedScheduleItemId = encodeURIComponent(String(scheduleItemId))
  const response = await http.patch(
    `${scheduleItemsPath(planId, planDayId)}/${encodedScheduleItemId}`,
    payload,
  )
  return response.data.data
}

export async function deleteScheduleItem(planId, planDayId, scheduleItemId, payload) {
  const encodedScheduleItemId = encodeURIComponent(String(scheduleItemId))
  const response = await http.delete(
    `${scheduleItemsPath(planId, planDayId)}/${encodedScheduleItemId}`,
    { data: payload },
  )
  return response.data.data
}

export async function reorderScheduleItems(planId, planDayId, payload) {
  const response = await http.put(`${scheduleItemsPath(planId, planDayId)}/order`, payload)
  return response.data.data
}
