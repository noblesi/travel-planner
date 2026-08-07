import { beforeEach, describe, expect, it, vi } from 'vitest'

const { authStore, restoreSessionMock } = vi.hoisted(() => {
  const store = {
    initialized: false,
    isAuthenticated: false,
  }
  return {
    authStore: store,
    restoreSessionMock: vi.fn(async () => {
      store.initialized = true
    }),
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ ...authStore, restoreSession: restoreSessionMock }),
}))

import router from '@/router'

beforeEach(async () => {
  restoreSessionMock.mockClear()
  authStore.initialized = true
  authStore.isAuthenticated = false
  await router.replace('/')
})

describe('plan route authentication guard', () => {
  it('세션 복원 후 비로그인 사용자를 로그인 화면으로 보낸다', async () => {
    authStore.initialized = false

    await router.push('/plans/new')

    expect(restoreSessionMock).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/plans/new')
  })

  it('로그인 사용자는 플랜 제작 화면에 진입할 수 있다', async () => {
    authStore.isAuthenticated = true

    await router.push('/plans/101/edit')

    expect(router.currentRoute.value.name).toBe('plan-editor')
  })

  it('비로그인 사용자의 내 플랜 접근을 로그인 화면으로 보낸다', async () => {
    await router.push('/my-plans')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/my-plans')
  })
})
