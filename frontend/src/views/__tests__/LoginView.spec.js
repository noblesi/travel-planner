import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/loginView/LoginView.vue'

const { loginWithLocalAccountMock, replaceMock, route } = vi.hoisted(() => ({
  loginWithLocalAccountMock: vi.fn(),
  replaceMock: vi.fn(),
  route: { query: { redirect: '/plans/new' } },
}))

vi.mock('@/api/auth', () => ({
  getAuthenticationSession: vi.fn(),
  loginWithLocalAccount: loginWithLocalAccountMock,
  logoutAuthenticationSession: vi.fn(),
}))

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  isNavigationFailure: (failure) => Boolean(failure),
  useRoute: () => route,
  useRouter: () => ({ replace: replaceMock }),
}))

beforeEach(() => {
  setActivePinia(createPinia())
  loginWithLocalAccountMock.mockReset().mockResolvedValue({
    authenticated: true,
    member: { memberId: '1', email: 'member@example.com', nickname: '여행자' },
  })
  replaceMock.mockReset().mockResolvedValue(undefined)
})

async function submitLogin(wrapper) {
  await wrapper.get('input[type="email"]').setValue('member@example.com')
  await wrapper.get('input[type="password"]').setValue('correct-password')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('LoginView', () => {
  it('로그인 성공 후 이동이 reject되어도 인증 상태를 유지하고 재로그인을 막는다', async () => {
    replaceMock.mockRejectedValueOnce(new Error('navigation failed'))
    const wrapper = mount(LoginView)
    const authStore = useAuthStore()

    await submitLogin(wrapper)

    expect(authStore.isAuthenticated).toBe(true)
    expect(wrapper.get('[role="alert"]').text()).toContain(
      '로그인은 완료되었지만 다음 화면으로 이동하지 못했습니다.',
    )
    expect(wrapper.get('input[type="password"]').element.value).toBe('')
    expect(wrapper.get('button[type="submit"]').text()).toBe('로그인 완료')
    expect(wrapper.get('button[type="submit"]').attributes()).toHaveProperty('disabled')

    await wrapper.get('form').trigger('submit')
    expect(loginWithLocalAccountMock).toHaveBeenCalledOnce()
  })

  it('router가 NavigationFailure를 resolve해도 로그인 완료 상태를 유지한다', async () => {
    replaceMock.mockResolvedValueOnce({ type: 'aborted' })
    const wrapper = mount(LoginView)

    await submitLogin(wrapper)

    expect(wrapper.get('[role="alert"]').text()).toContain('다음 화면으로 이동하지 못했습니다.')
    expect(wrapper.get('button[type="submit"]').text()).toBe('로그인 완료')
  })

  it('로그인 API 실패는 비밀번호를 지우고 다시 제출할 수 있게 한다', async () => {
    loginWithLocalAccountMock.mockRejectedValueOnce({
      response: { data: { message: '이메일 또는 비밀번호가 올바르지 않습니다.' } },
    })
    const wrapper = mount(LoginView)

    await submitLogin(wrapper)

    expect(wrapper.get('[role="alert"]').text()).toBe(
      '이메일 또는 비밀번호가 올바르지 않습니다.',
    )
    expect(wrapper.get('input[type="password"]').element.value).toBe('')
    expect(wrapper.get('button[type="submit"]').attributes()).not.toHaveProperty('disabled')
    expect(replaceMock).not.toHaveBeenCalled()
  })
})
