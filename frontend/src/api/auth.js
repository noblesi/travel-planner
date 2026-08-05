import http from './http'

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
  return response.data.data
}

export async function logoutAuthenticationSession() {
  const response = await http.post('/auth/logout')
  return response.data.data
}
