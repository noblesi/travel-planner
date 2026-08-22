import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useJoinDraftStore } from '@/stores/joinDraft'
import JoinView from '@/views/joinView/JoinView.vue'

const { getMemberEmailCheckMock, pushMock, backMock } = vi.hoisted(() => ({
  getMemberEmailCheckMock: vi.fn(),
  pushMock: vi.fn(),
  backMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, back: backMock }),
}))

vi.mock('@/api/users', () => ({
  getMemberEmailCheck: getMemberEmailCheckMock,
}))

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  pushMock.mockResolvedValue(undefined)
  getMemberEmailCheckMock.mockResolvedValue(false)
})

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return {
    store: useJoinDraftStore(),
    wrapper: mount(JoinView, { global: { plugins: [pinia] } }),
  }
}

async function fillCredentials(wrapper, email = '  MEMBER@Example.COM ') {
  await wrapper.get('#join-email').setValue(email)
  await wrapper.get('#join-password').setValue('password-value')
  await wrapper.get('#join-password-confirmation').setValue('password-value')
}

describe('JoinView', () => {
  it('정규화한 email을 확인하고 profile 단계로 이동한다', async () => {
    const { store, wrapper } = mountView()
    await fillCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(getMemberEmailCheckMock).toHaveBeenCalledWith('member@example.com')
    expect(store.hasCredentials).toBe(true)
    expect(pushMock).toHaveBeenCalledWith({ name: 'joinProfile' })
    wrapper.unmount()
  })

  it('backend 계약을 벗어난 비밀번호는 API를 호출하지 않는다', async () => {
    const { wrapper } = mountView()
    await wrapper.get('#join-email').setValue('member@example.com')
    await wrapper.get('#join-password').setValue('short')
    await wrapper.get('#join-password-confirmation').setValue('short')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toContain('10자 이상 72자 이하')
    expect(getMemberEmailCheckMock).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('email 확인 중 중복 제출을 막고 서버 오류를 표시한다', async () => {
    let rejectRequest
    getMemberEmailCheckMock.mockReturnValue(
      new Promise((resolve, reject) => {
        rejectRequest = reject
      }),
    )
    const { wrapper } = mountView()
    await fillCredentials(wrapper, 'member@example.com')

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(getMemberEmailCheckMock).toHaveBeenCalledOnce()
    expect(wrapper.get('button[type="submit"]').attributes()).toHaveProperty('disabled')

    rejectRequest({ response: { data: { message: '이메일 확인 서비스 오류' } } })
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('이메일 확인 서비스 오류')
    wrapper.unmount()
  })
})
