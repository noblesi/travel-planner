import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getAuthenticationSession,
  loginWithLocalAccount,
  logoutAuthenticationSession,
} from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({
  getAuthenticationSession: vi.fn(),
  loginWithLocalAccount: vi.fn(),
  logoutAuthenticationSession: vi.fn(),
}))

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

describe('auth store', () => {
  const member = {
    memberId: '7',
    email: 'member@example.com',
    displayName: '여행자',
  }

  it('서버 session에서 현재 사용자를 복원한다', async () => {
    getAuthenticationSession.mockResolvedValue({ authenticated: true, member })
    const store = useAuthStore()

    await store.restoreSession()

    expect(store.initialized).toBe(true)
    expect(store.isAuthenticated).toBe(true)
    expect(store.currentUser).toEqual(member)
  })

  it('로그인 성공 시 사용자 상태를 저장한다', async () => {
    loginWithLocalAccount.mockResolvedValue({ authenticated: true, member })
    const store = useAuthStore()

    await expect(
      store.login({ email: member.email, password: 'correct-password' }),
    ).resolves.toEqual(member)

    expect(store.currentUser).toEqual(member)
    expect(store.errorMessage).toBe('')
  })

  it('로그인 실패 메시지를 보존한다', async () => {
    loginWithLocalAccount.mockRejectedValue({
      response: { data: { message: '이메일 또는 비밀번호가 올바르지 않습니다.' } },
    })
    const store = useAuthStore()

    await expect(store.login({ email: member.email, password: 'wrong' })).rejects.toBeTruthy()

    expect(store.isAuthenticated).toBe(false)
    expect(store.errorMessage).toBe('이메일 또는 비밀번호가 올바르지 않습니다.')
  })

  it('서버 로그아웃 성공 후 사용자 상태를 제거한다', async () => {
    logoutAuthenticationSession.mockResolvedValue(null)
    const store = useAuthStore()
    store.setCurrentUser(member)

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
  })
})
