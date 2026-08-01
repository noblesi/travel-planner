import http from './http'

export async function getCsrfToken() {
  const response = await http.get('/auth/csrf')
  return response.data.data
}

async function postWithCsrf(path, payload) {
  const csrf = await getCsrfToken()
  const response = await http.post(path, payload, {
    headers: { [csrf.headerName]: csrf.token },
  })
  return response.data.data
}

export async function getAuthenticationSession() {
  const response = await http.get('/auth/session')
  return response.data.data
}

export function loginWithLocalAccount(payload) {
  return postWithCsrf('/auth/login', payload)
}

export function logoutAuthenticationSession() {
  return postWithCsrf('/auth/logout')
}
