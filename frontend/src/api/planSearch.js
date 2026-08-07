import http from './http'

export async function getPlanList({ keyword = '', page = 1, size = 24 } = {}) {
  const response = await http.get('/plan-search/plans', { params: { keyword, page, size } })
  const { content, pagination } = response.data.data
  return {
    plans: content,
    page: pagination.page,
    totalCount: pagination.totalCount,
    hasNext: pagination.page < pagination.totalPages,
  }
}

export async function getPlanDetail(planId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.get(`/plan-search/plans/${encodedPlanId}`)
  return response.data.data
}

export async function toggleLike(planId) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.post(`/plan-search/plans/${encodedPlanId}/like`)
  return response.data.data
}

export async function reportPlan(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.post(`/plan-search/plans/${encodedPlanId}/report`, payload)
  return response.data.data
}

export async function copyPlan(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.post(`/plan-search/plans/${encodedPlanId}/copy`, payload)
  return response.data.data
}
