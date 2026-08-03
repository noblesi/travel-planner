import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 5000,
  withCredentials: true,
})

const csrfProtectedMethods = new Set(['post', 'put', 'patch', 'delete'])

export async function attachCsrfToken(config) {
  const method = config.method?.toLowerCase() || 'get'
  if (!csrfProtectedMethods.has(method)) return config

  const response = await http.get('/auth/csrf')
  const csrf = response.data?.data
  if (!csrf?.headerName || !csrf?.token) {
    throw new Error('CSRF token response is invalid')
  }

  if (typeof config.headers?.set === 'function') {
    config.headers.set(csrf.headerName, csrf.token)
  } else {
    config.headers = {
      ...config.headers,
      [csrf.headerName]: csrf.token,
    }
  }

  return config
}

http.interceptors.request.use(attachCsrfToken)

export default http
