import http from './http'

export async function createPlanInvitations(planId, payload) {
  const encodedPlanId = encodeURIComponent(String(planId))
  const response = await http.post(`/plans/${encodedPlanId}/invitations`, payload)
  return response.data.data
}

export async function getPlanInvitation(token) {
  const encodedToken = encodeURIComponent(String(token))
  const response = await http.get(`/plan-invitations/${encodedToken}`)
  return response.data.data
}

export async function acceptPlanInvitation(token) {
  const encodedToken = encodeURIComponent(String(token))
  const response = await http.post(`/plan-invitations/${encodedToken}/accept`)
  return response.data.data
}
