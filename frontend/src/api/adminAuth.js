import http from './http'

export async function loginAdmin(payload) {
  const response = await http.post('/admin/auth/login', payload)
  return response.data
}
