import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useJoinDraftStore } from '@/stores/joinDraft'
import JoinProfileView from '@/views/joinView/JoinProfileView.vue'

const { postMemberJoinMock, replaceMock } = vi.hoisted(() => ({
  postMemberJoinMock: vi.fn(),
  replaceMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: replaceMock }),
  isNavigationFailure: (failure) => Boolean(failure),
}))

vi.mock('@/api/users', () => ({
  postMemberJoin: postMemberJoinMock,
}))

beforeEach(() => {
  setActivePinia(createPinia())
  replaceMock.mockReset()
  replaceMock.mockResolvedValue(undefined)
  postMemberJoinMock.mockReset()
  postMemberJoinMock.mockResolvedValue(true)
})

function mountViewWithStep1Data() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useJoinDraftStore()
  store.beginRegistration({ email: 'member@example.com', password: 'password-value' })
  const wrapper = mount(JoinProfileView, { global: { plugins: [pinia] } })
  return { store, wrapper }
}

describe('JoinProfileView', () => {
  it('동의한 프로필 정보를 서버에 전송하고 완료 화면으로 이동한다', async () => {
    const { store, wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="radio"][value="F"]').setValue()
    await wrapper.get('input[type="checkbox"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(postMemberJoinMock).toHaveBeenCalledWith({
      email: 'member@example.com',
      password: 'password-value',
      birth: '20000101',
      privacy: 'Y',
      name: '홍길동',
      nickname: '홍길동',
      gender: 'F',
      phone: '010-1234-5678',
    })
    expect(store.hasCredentials).toBe(false)
    expect(replaceMock).toHaveBeenCalledWith({ name: 'complete' })

    wrapper.unmount()
  })

  it('8자리가 아닌 생년월일은 전송하지 않는다', async () => {
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('2000010')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toContain('생년월일을 8자리로 정확하게')
    expect(postMemberJoinMock).not.toHaveBeenCalled()
    expect(replaceMock).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('개인정보 저장에 동의하지 않으면 전송하지 않는다', async () => {
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toContain('개인정보 저장에 동의해 주세요.')
    expect(postMemberJoinMock).not.toHaveBeenCalled()
    expect(replaceMock).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('중복 제출을 막고 API 오류를 화면에 표시한다', async () => {
    let rejectRequest
    postMemberJoinMock.mockReturnValue(
      new Promise((resolve, reject) => {
        rejectRequest = reject
      }),
    )
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="checkbox"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(postMemberJoinMock).toHaveBeenCalledOnce()
    expect(wrapper.get('button[type="submit"]').attributes()).toHaveProperty('disabled')

    rejectRequest({ response: { data: { message: '이미 사용 중인 이메일입니다.' } } })
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('이미 사용 중인 이메일입니다.')
    wrapper.unmount()
  })

  it('가입 성공 후 화면 이동이 실패해도 가입 요청을 재전송하지 않는다', async () => {
    replaceMock.mockRejectedValueOnce(new Error('navigation failed'))
    const { store, wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')
    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="checkbox"]').setValue(true)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(store.hasCredentials).toBe(false)
    expect(wrapper.get('[role="alert"]').text()).toContain(
      '회원가입은 완료되었지만 완료 화면으로 이동하지 못했습니다.',
    )
    expect(wrapper.get('button[type="submit"]').text()).toBe('가입 완료')
    expect(wrapper.get('button[type="submit"]').attributes()).toHaveProperty('disabled')

    await wrapper.get('form').trigger('submit')
    expect(postMemberJoinMock).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it('가입 성공 후 router가 NavigationFailure를 resolve해도 완료 상태를 유지한다', async () => {
    replaceMock.mockResolvedValueOnce({ type: 'aborted' })
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')
    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="checkbox"]').setValue(true)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('회원가입은 완료되었지만')
    expect(wrapper.get('button[type="submit"]').text()).toBe('가입 완료')
    wrapper.unmount()
  })
})
