import http from './http'

export async function createTravelPlan(payload) {
  const response = await http.post('/plans', payload)
  return response.data.data
}
