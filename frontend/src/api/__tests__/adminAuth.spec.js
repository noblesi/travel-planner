import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAdminSession, loginAdmin, logoutAdmin } from '@/api/adminAuth'
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

describe('admin authentication API', () => {
  it('관리자 로그인과 세션 조회 응답을 반환한다', async () => {
    const admin = { adminId: 1, loginId: 'e2e_admin' }
    http.post.mockResolvedValueOnce({ data: admin })
    http.get.mockResolvedValueOnce({ data: admin })

    const credentials = { loginId: 'e2e_admin', password: 'correct-password' }
    await expect(loginAdmin(credentials)).resolves.toEqual(admin)
    await expect(getAdminSession()).resolves.toEqual(admin)

    expect(http.post).toHaveBeenCalledWith('/admin/auth/login', credentials)
    expect(http.get).toHaveBeenCalledWith('/admin/auth/session')
  })

  it('관리자 로그아웃 요청을 전송한다', async () => {
    http.post.mockResolvedValue({ data: null })

    await expect(logoutAdmin()).resolves.toBeUndefined()
    expect(http.post).toHaveBeenCalledWith('/admin/auth/logout')
  })
})
