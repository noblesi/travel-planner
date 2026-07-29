import http from './http'

export async function getRegions() {
  const response = await http.get('/regions')
  return response.data.data.regions
}
