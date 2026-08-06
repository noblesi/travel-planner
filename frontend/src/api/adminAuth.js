import http from './http'

export async function loginAdmin(payload) {
  const response = await http.post('/admin/auth/login', payload)
  return response.data
}

export async function getAdminSession() {
  const response = await http.get('/admin/auth/session')
  return response.data
}

export async function logoutAdmin() {
  await http.post('/admin/auth/logout')
}
