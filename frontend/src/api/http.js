import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 5000,
  withCredentials: true,
})

const csrfProtectedMethods = new Set(['post', 'put', 'patch', 'delete'])
let cachedCsrf = null
let pendingCsrfRequest = null
let csrfCacheGeneration = 0

export function clearCsrfTokenCache() {
  csrfCacheGeneration += 1
  cachedCsrf = null
  pendingCsrfRequest = null
}

async function getCachedCsrfToken() {
  if (cachedCsrf) return cachedCsrf
  if (pendingCsrfRequest) return pendingCsrfRequest

  // 같은 시점의 mutation들이 CSRF endpoint를 중복 호출하지 않도록 하나의 진행 중 요청을 공유한다.
  const requestGeneration = csrfCacheGeneration
  const request = http
    .get('/auth/csrf')
    .then((response) => {
      const csrf = response.data?.data
      if (!csrf?.headerName || !csrf?.token) {
        throw new Error('CSRF token response is invalid')
      }
      // 인증 직후 cache가 초기화됐다면 이전 session에서 시작된 늦은 응답을 다시 저장하지 않는다.
      if (requestGeneration === csrfCacheGeneration) cachedCsrf = csrf
      return csrf
    })
    .finally(() => {
      if (pendingCsrfRequest === request) pendingCsrfRequest = null
    })

  pendingCsrfRequest = request
  return request
}

export async function attachCsrfToken(config) {
  const method = config.method?.toLowerCase() || 'get'
  if (!csrfProtectedMethods.has(method)) return config

  const csrf = await getCachedCsrfToken()

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
