import http from './http'

export async function createTravelPlan(payload) {
  const response = await http.post('/plans', payload)
  return response.data.data
}

export async function getTravelPlanEditor(planId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.get(`/plans/${encodedPlanId}/editor`)
  return response.data.data
}

export async function updateTravelPlanDates(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.patch(`/plans/${encodedPlanId}/dates`, payload)
  return response.data.data
}
