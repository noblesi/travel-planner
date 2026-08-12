import { beforeEach, describe, expect, it, vi } from 'vitest'

const { authStore, joinStore, restoreSessionMock } = vi.hoisted(() => {
  const store = {
    initialized: false,
    isAuthenticated: false,
  }
  return {
    authStore: store,
    joinStore: {
      userInfo: {
        email: '',
        password: '',
      },
    },
    restoreSessionMock: vi.fn(async () => {
      store.initialized = true
    }),
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ ...authStore, restoreSession: restoreSessionMock }),
}))

vi.mock('@/stores/useUserStore', () => ({
  useUserStore: () => joinStore,
}))

import router from '@/router'

beforeEach(async () => {
  restoreSessionMock.mockClear()
  authStore.initialized = true
  authStore.isAuthenticated = false
  joinStore.userInfo.email = ''
  joinStore.userInfo.password = ''
  await router.replace('/')
})

describe('join profile route guard', () => {
  it('1단계 정보가 없으면 회원가입 첫 화면으로 돌려보낸다', async () => {
    const alertMock = vi.spyOn(window, 'alert').mockImplementation(() => {})

    await router.push('/joinProfileView')

    expect(router.currentRoute.value.name).toBe('join')
    expect(alertMock).toHaveBeenCalledWith('회원가입 정보를 먼저 입력해주세요.')
    alertMock.mockRestore()
  })

  it('1단계 정보가 있으면 프로필 화면에 진입한다', async () => {
    joinStore.userInfo.email = 'member@example.com'
    joinStore.userInfo.password = 'password-value'

    await router.push('/joinProfileView')

    expect(router.currentRoute.value.name).toBe('joinProfile')
  })
})

describe('unfinished feature routes', () => {
  it('마이페이지 프로토타입을 공개 라우트로 노출하지 않는다', async () => {
    await router.push('/myPage')

    expect(router.currentRoute.value.name).toBe('not-found')
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
