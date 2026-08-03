import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getAuthenticationSession,
  getCsrfToken,
  loginWithLocalAccount,
  logoutAuthenticationSession,
} from '@/api/auth'
import http from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('authentication API', () => {
  it('현재 session을 조회한다', async () => {
    const session = { authenticated: false, member: null }
    http.get.mockResolvedValue({ data: { data: session } })

    await expect(getAuthenticationSession()).resolves.toEqual(session)
    expect(http.get).toHaveBeenCalledWith('/auth/session')
  })

  it('CSRF token을 조회한다', async () => {
    const csrf = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' }
    http.get.mockResolvedValue({ data: { data: csrf } })

    await expect(getCsrfToken()).resolves.toEqual(csrf)
    expect(http.get).toHaveBeenCalledWith('/auth/csrf')
  })

  it('로그인과 로그아웃 요청에 서버가 발급한 CSRF token을 포함한다', async () => {
    const csrf = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' }
    const session = { authenticated: true, member: { memberId: '7' } }
    http.get.mockResolvedValue({ data: { data: csrf } })
    http.post
      .mockResolvedValueOnce({ data: { data: session } })
      .mockResolvedValueOnce({ data: { data: null } })

    const credentials = { email: 'member@example.com', password: 'correct-password' }
    await expect(loginWithLocalAccount(credentials)).resolves.toEqual(session)
    await expect(logoutAuthenticationSession()).resolves.toBeNull()

    expect(http.post).toHaveBeenNthCalledWith(1, '/auth/login', credentials, {
      headers: { 'X-CSRF-TOKEN': 'csrf-token' },
    })
    expect(http.post).toHaveBeenNthCalledWith(2, '/auth/logout', undefined, {
      headers: { 'X-CSRF-TOKEN': 'csrf-token' },
    })
  })
})
