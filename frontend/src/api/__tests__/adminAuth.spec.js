import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAdminSession, loginAdmin, logoutAdmin } from '@/api/adminAuth'
import http from '@/api/http'

const { clearCsrfTokenCacheMock } = vi.hoisted(() => ({
  clearCsrfTokenCacheMock: vi.fn(),
}))

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  clearCsrfTokenCache: clearCsrfTokenCacheMock,
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('admin authentication API', () => {
  it('관리자 로그인과 세션 조회 응답의 data를 반환한다', async () => {
    const session = { authenticated: true, admin: { loginId: 'e2e_admin' } }
    http.post.mockResolvedValueOnce({ data: { data: session } })
    http.get.mockResolvedValueOnce({ data: { data: session } })

    const credentials = { loginId: 'e2e_admin', password: 'correct-password' }
    await expect(loginAdmin(credentials)).resolves.toEqual(session)
    await expect(getAdminSession()).resolves.toEqual(session)

    expect(http.post).toHaveBeenCalledWith('/admin/auth/login', credentials)
    expect(http.get).toHaveBeenCalledWith('/admin/auth/session')
    expect(clearCsrfTokenCacheMock).toHaveBeenCalledTimes(1)
  })

  it('관리자 로그아웃 요청을 전송한다', async () => {
    http.post.mockResolvedValue({ data: { data: null } })

    await expect(logoutAdmin()).resolves.toBeNull()
    expect(http.post).toHaveBeenCalledWith('/admin/auth/logout')
    expect(clearCsrfTokenCacheMock).toHaveBeenCalledTimes(1)
  })
})
