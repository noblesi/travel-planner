import http, { clearCsrfTokenCache } from './http'

export async function getCsrfToken() {
  const response = await http.get('/auth/csrf')
  return response.data.data
}

export async function getAuthenticationSession() {
  const response = await http.get('/auth/session')
  return response.data.data
}

export async function loginWithLocalAccount(payload) {
  const response = await http.post('/auth/login', payload)
  // Spring Security가 로그인 성공 시 CSRF token을 회전하므로 다음 mutation은 새 token을 조회해야 한다.
  clearCsrfTokenCache()
  return response.data.data
}

export async function logoutAuthenticationSession() {
  const response = await http.post('/auth/logout')
  // 무효화된 session의 token을 재사용하지 않도록 logout 완료 후 cache를 비운다.
  clearCsrfTokenCache()
  return response.data.data
}
