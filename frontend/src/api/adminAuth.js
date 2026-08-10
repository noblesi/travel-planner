import http, { clearCsrfTokenCache } from './http'

export async function loginAdmin(payload) {
  const response = await http.post('/admin/auth/login', payload)
  // 관리자 인증도 session fixation 방어 과정에서 CSRF token이 회전하므로 성공 직후 cache를 폐기한다.
  clearCsrfTokenCache()
  return response.data.data
}

export async function getAdminSession() {
  const response = await http.get('/admin/auth/session')
  return response.data.data
}

export async function logoutAdmin() {
  const response = await http.post('/admin/auth/logout')
  clearCsrfTokenCache()
  return response.data.data
}
