import { beforeEach, describe, expect, it, vi } from 'vitest'

const { authStore, joinStore, restoreSessionMock, clearRegistrationMock } = vi.hoisted(() => {
  const store = {
    initialized: false,
    isAuthenticated: false,
  }
  const draft = {
    hasCredentials: false,
  }
  return {
    authStore: store,
    joinStore: draft,
    clearRegistrationMock: vi.fn(() => {
      draft.hasCredentials = false
    }),
    restoreSessionMock: vi.fn(async () => {
      store.initialized = true
    }),
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ ...authStore, restoreSession: restoreSessionMock }),
}))

vi.mock('@/stores/joinDraft', () => ({
  useJoinDraftStore: () => ({ ...joinStore, clearRegistration: clearRegistrationMock }),
}))

import router from '@/router'

beforeEach(async () => {
  restoreSessionMock.mockClear()
  clearRegistrationMock.mockClear()
  authStore.initialized = true
  authStore.isAuthenticated = false
  joinStore.hasCredentials = false
  await router.replace('/')
})

describe('join profile route guard', () => {
  it('1단계 정보가 없으면 회원가입 첫 화면으로 돌려보낸다', async () => {
    await router.push('/joinProfileView')

    expect(router.currentRoute.value.name).toBe('join')
    expect(router.currentRoute.value.query.reset).toBe('true')
  })

  it('1단계 정보가 있으면 프로필 화면에 진입한다', async () => {
    joinStore.hasCredentials = true

    await router.push('/joinProfileView')

    expect(router.currentRoute.value.name).toBe('joinProfile')
  })

  it('회원가입 flow를 벗어나면 credential draft를 정리한다', async () => {
    joinStore.hasCredentials = true
    await router.push('/joinProfileView')

    await router.push('/')

    expect(clearRegistrationMock).toHaveBeenCalled()
  })
})

describe('my page route authentication guard', () => {
  it('비로그인 사용자를 로그인 화면으로 보낸다', async () => {
    await router.push('/myPage')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/myPage')
  })

  it('로그인 사용자는 마이페이지에 진입할 수 있다', async () => {
    authStore.isAuthenticated = true

    await router.push('/myPage')

    expect(router.currentRoute.value.name).toBe('myPage')
  })
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
